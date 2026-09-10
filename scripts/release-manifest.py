#!/usr/bin/env python3
"""Produce and verify the preview release manifest (Neutrino SHA -> iroh SHA -> AAR -> Chat SHA -> APK).

Three commands, all fail-closed (a check that cannot be made is a failure, not a warning):

  check-provenance  Cross-check services/neutrino/impl/neutrino-provenance.json against the version
                    catalog, the checksum in build.gradle.kts and (optionally) the downloaded .aar.
  build             Write release-manifest.json for a signed APK from the provenance record, the APK's
                    own manifest and signing block, apksigner, and the CI runs of the source commit.
  verify            Validate a downloaded APK + manifest pair offline: content hash, package identity,
                    signing certificate, provenance shape, evidence, and monotonic versionCode against a
                    previous manifest. This is what the F-Droid repository and reviewers run.

Only the standard library is used. The APK parsing here reads the binary AndroidManifest.xml and the
APK Signing Block (v2/v3/v3.1) to identify the signing certificate; it does not verify signatures
cryptographically. apksigner does that, and is required unless the caller explicitly opts out.
"""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import sys
import zipfile

SCHEMA_VERSION = 1
PROVENANCE_PATH = Path("services/neutrino/impl/neutrino-provenance.json")
CATALOG_PATH = Path("gradle/libs.versions.toml")
BUILD_SCRIPT_PATH = Path("services/neutrino/impl/build.gradle.kts")
EVIDENCE_WORKFLOWS = ("Test", "Code Quality Checks", "APK Build")
PREVIEW_PACKAGE = "org.indiafoss.chat"
SHA256_RE = re.compile(r"[0-9a-f]{64}")
COMMIT_RE = re.compile(r"[0-9a-f]{40}")
CERT_LINE_RE = re.compile(
    r"^(?:Signer (?:#\d+|\(minSdkVersion=[^\n]+\))|V[234]\.[0-9]+ Signer:) certificate SHA-256 digest: ([0-9a-fA-F]{64})$",
    re.MULTILINE,
)


class ManifestError(ValueError):
    """A check failed; the caller must not publish or accept the artifact."""


def sha256_file(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalise_fingerprint(value, what):
    fingerprint = (value or "").replace(":", "").lower()
    if not SHA256_RE.fullmatch(fingerprint):
        raise ManifestError(f"{what} must be a SHA-256 fingerprint")
    return fingerprint


# --- APK Signing Block -------------------------------------------------------------------------------

APK_SIG_BLOCK_MAGIC = b"APK Sig Block 42"
EOCD_MAGIC = b"PK\x05\x06"
SIGNATURE_SCHEME_IDS = {0x7109871A: "v2", 0xF05368C0: "v3", 0x1B93AD61: "v3.1"}


def _u32(data, offset):
    return struct.unpack_from("<I", data, offset)[0]


def _prefixed(data, offset):
    """Return (payload, next_offset) for a uint32 length-prefixed field."""
    length = _u32(data, offset)
    start = offset + 4
    end = start + length
    if end > len(data):
        raise ManifestError("APK signing block is truncated")
    return data[start:end], end


def _sequence(data):
    """Split a sequence of length-prefixed elements."""
    offset, items = 0, []
    while offset < len(data):
        item, offset = _prefixed(data, offset)
        items.append(item)
    return items


def find_central_directory(data):
    """Return the central directory offset from the end-of-central-directory record."""
    index = data.rfind(EOCD_MAGIC, max(0, len(data) - 65557))
    if index < 0:
        raise ManifestError("not a zip file (no end of central directory)")
    return _u32(data, index + 16), index


def signing_block_certificates(apk_path):
    """Return {scheme: [certificate SHA-256 per signer]} declared by the APK Signing Block."""
    data = Path(apk_path).read_bytes()
    cd_offset, _ = find_central_directory(data)
    if cd_offset < 32 or data[cd_offset - 16:cd_offset] != APK_SIG_BLOCK_MAGIC:
        raise ManifestError("APK has no APK Signing Block (unsigned, or v1-only)")
    block_size = struct.unpack_from("<Q", data, cd_offset - 24)[0]
    start = cd_offset - block_size - 8
    if start < 0 or struct.unpack_from("<Q", data, start)[0] != block_size:
        raise ManifestError("APK Signing Block size fields disagree")
    offset, end = start + 8, cd_offset - 24
    schemes = {}
    while offset < end:
        pair_length = struct.unpack_from("<Q", data, offset)[0]
        pair_id = _u32(data, offset + 8)
        value = data[offset + 12:offset + 8 + pair_length]
        offset += 8 + pair_length
        scheme = SIGNATURE_SCHEME_IDS.get(pair_id)
        if scheme is None:
            continue
        signers, _ = _prefixed(value, 0)
        digests = []
        for signer in _sequence(signers):
            signed_data, _ = _prefixed(signer, 0)
            _, after_digests = _prefixed(signed_data, 0)
            certificates, _ = _prefixed(signed_data, after_digests)
            first = _sequence(certificates)
            if not first:
                raise ManifestError(f"{scheme} signer carries no certificate")
            digests.append(hashlib.sha256(first[0]).hexdigest())
        schemes[scheme] = digests
    if not schemes:
        raise ManifestError("APK Signing Block carries no v2/v3 signature")
    return schemes


def declared_certificate(apk_path):
    """The single certificate every scheme in the signing block agrees on."""
    schemes = signing_block_certificates(apk_path)
    identities = {tuple(digests) for digests in schemes.values()}
    if len(identities) != 1 or len(next(iter(identities))) != 1:
        raise ManifestError("APK signing block does not declare exactly one signer across all schemes")
    return next(iter(identities))[0], sorted(schemes)


# --- Binary AndroidManifest.xml ----------------------------------------------------------------------

RES_STRING_POOL_TYPE = 0x0001
RES_XML_TYPE = 0x0003
RES_XML_START_ELEMENT_TYPE = 0x0102
TYPE_STRING = 0x03
TYPE_INT_DEC = 0x10


def _string_pool(data, start):
    header_size = struct.unpack_from("<H", data, start + 2)[0]
    count, _, flags, strings_start, _ = struct.unpack_from("<IIIII", data, start + 8)
    utf8 = bool(flags & (1 << 8))
    offsets = struct.unpack_from(f"<{count}I", data, start + header_size)
    strings = []
    for offset in offsets:
        position = start + strings_start + offset
        if utf8:
            char_length = data[position]
            position += 2 if char_length & 0x80 else 1
            byte_length = data[position]
            position += 1
            if byte_length & 0x80:
                byte_length = ((byte_length & 0x7F) << 8) | data[position]
                position += 1
            strings.append(data[position:position + byte_length].decode("utf-8", "replace"))
        else:
            length = struct.unpack_from("<H", data, position)[0]
            position += 2
            if length & 0x8000:
                length = ((length & 0x7FFF) << 16) | struct.unpack_from("<H", data, position)[0]
                position += 2
            strings.append(data[position:position + length * 2].decode("utf-16-le", "replace"))
    return strings


def parse_manifest_attributes(axml):
    """Return {element name: {attribute name: value}} for the first occurrence of each element."""
    if struct.unpack_from("<H", axml, 0)[0] != RES_XML_TYPE:
        raise ManifestError("AndroidManifest.xml is not binary XML")
    strings, elements = [], {}
    offset = struct.unpack_from("<H", axml, 2)[0]
    while offset + 8 <= len(axml):
        chunk_type, header_size, chunk_size = struct.unpack_from("<HHI", axml, offset)
        if chunk_size < 8:
            raise ManifestError("AndroidManifest.xml chunk is malformed")
        if chunk_type == RES_STRING_POOL_TYPE:
            strings = _string_pool(axml, offset)
        elif chunk_type == RES_XML_START_ELEMENT_TYPE:
            body = offset + header_size
            _, name_index, attribute_start, attribute_size, attribute_count = struct.unpack_from("<IIHHH", axml, body)
            element = strings[name_index]
            attributes = {}
            position = body + attribute_start
            for _ in range(attribute_count):
                _, attr_name, raw_value, _, _, data_type, value = struct.unpack_from("<IIIHBBI", axml, position)
                position += attribute_size
                if data_type == TYPE_STRING:
                    attributes[strings[attr_name]] = strings[raw_value if raw_value != 0xFFFFFFFF else value]
                elif data_type == TYPE_INT_DEC:
                    attributes[strings[attr_name]] = value
            elements.setdefault(element, attributes)
        offset += chunk_size
    return elements


def apk_identity(apk_path):
    with zipfile.ZipFile(apk_path) as archive:
        elements = parse_manifest_attributes(archive.read("AndroidManifest.xml"))
    manifest = elements.get("manifest", {})
    identity = {
        "packageName": manifest.get("package"),
        "versionCode": manifest.get("versionCode"),
        "versionName": manifest.get("versionName"),
        "minSdkVersion": elements.get("uses-sdk", {}).get("minSdkVersion"),
    }
    if not all(identity[key] is not None for key in ("packageName", "versionCode", "versionName")):
        raise ManifestError("could not read package, versionCode and versionName from AndroidManifest.xml")
    return identity


# --- apksigner ---------------------------------------------------------------------------------------

def find_apksigner(explicit=None):
    if explicit:
        return explicit
    if os.environ.get("APKSIGNER"):
        return os.environ["APKSIGNER"]
    for variable in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        root = os.environ.get(variable)
        if not root:
            continue
        candidates = sorted(Path(root, "build-tools").glob("*/apksigner"), key=lambda path: path.parent.name)
        if candidates:
            return str(candidates[-1])
    return None


def apksigner_certificate(apksigner, apk_path):
    """Cryptographically verify the APK with apksigner and return its single signer certificate."""
    # Never echo tool output: sign-nightly.py keeps the same rule for diagnostics.
    result = subprocess.run([apksigner, "verify", "--verbose", "--print-certs", str(apk_path)],
                            capture_output=True, text=True)
    if result.returncode:
        raise ManifestError("apksigner rejected the APK signature")
    report = result.stdout
    digests = {match.lower() for match in CERT_LINE_RE.findall(report)}
    if re.search(r"^Number of signers: 1$", report, re.MULTILINE) is None or len(digests) != 1:
        # The verification report describes public certificate material only, so it is safe to show.
        raise ManifestError("apksigner did not report exactly one signing certificate:\n" + report + result.stderr)
    return next(iter(digests))


# --- Provenance --------------------------------------------------------------------------------------

def load_provenance(repo_root):
    path = Path(repo_root) / PROVENANCE_PATH
    try:
        record = json.loads(path.read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise ManifestError(f"cannot read {PROVENANCE_PATH}: {error}") from error
    validate_provenance_shape(record)
    return record


def validate_provenance_shape(record):
    bindings = record.get("bindings", {})
    neutrino = record.get("neutrino", {})
    iroh = record.get("neutrinoIroh", {})
    for section, keys in (("bindings", ("version", "artifact", "sha256", "source")),
                          ("neutrino", ("repository", "rev")),
                          ("neutrinoIroh", ("repository", "rev"))):
        missing = [key for key in keys if not record.get(section, {}).get(key)]
        if missing:
            raise ManifestError(f"provenance {section} is missing {', '.join(missing)}")
    if not SHA256_RE.fullmatch(bindings["sha256"]):
        raise ManifestError("provenance bindings sha256 must be 64 lowercase hex characters")
    for name, rev in (("neutrino", neutrino["rev"]), ("neutrinoIroh", iroh["rev"])):
        if not COMMIT_RE.fullmatch(rev):
            raise ManifestError(f"provenance {name} rev must be a full 40-character commit SHA")
    version = bindings["version"]
    match = re.fullmatch(r"(\d+\.\d+\.\d+)-e2ee\.([0-9a-f]{7,})-ble\.([0-9a-f]{7,})", version)
    if not match:
        raise ManifestError(f"bindings version {version!r} is not <base>-e2ee.<neutrino rev>-ble.<neutrino-iroh rev>")
    if not neutrino["rev"].startswith(match.group(2)):
        raise ManifestError("bindings version does not encode the recorded neutrino rev")
    if not iroh["rev"].startswith(match.group(3)):
        raise ManifestError("bindings version does not encode the recorded neutrino-iroh rev")
    if bindings["artifact"] != f"neutrino-bindings-{version}.aar":
        raise ManifestError("bindings artifact name does not match the bindings version")


def check_provenance(repo_root, aar_path=None):
    """Return the provenance record after confirming every pinned value agrees."""
    root = Path(repo_root)
    record = load_provenance(root)
    catalog = (root / CATALOG_PATH).read_text()
    match = re.search(r'^neutrino\s*=\s*"([^"]+)"', catalog, re.MULTILINE)
    if not match:
        raise ManifestError(f"no neutrino version in {CATALOG_PATH}")
    if match.group(1) != record["bindings"]["version"]:
        raise ManifestError(f"{CATALOG_PATH} pins {match.group(1)} but provenance records {record['bindings']['version']}")
    build_script = (root / BUILD_SCRIPT_PATH).read_text()
    match = re.search(r'^val neutrinoSha256\s*=\s*"([0-9a-f]{64})"', build_script, re.MULTILINE)
    if not match:
        raise ManifestError(f"no neutrinoSha256 in {BUILD_SCRIPT_PATH}")
    if match.group(1) != record["bindings"]["sha256"]:
        raise ManifestError(f"{BUILD_SCRIPT_PATH} expects a different .aar checksum than the provenance record")
    if aar_path is not None:
        aar = Path(aar_path)
        if aar.name != record["bindings"]["artifact"]:
            raise ManifestError(f"{aar.name} is not the recorded artifact {record['bindings']['artifact']}")
        actual = sha256_file(aar)
        if actual != record["bindings"]["sha256"]:
            raise ManifestError(f"downloaded .aar SHA-256 {actual} does not match the provenance record")
    return record


# --- Evidence ----------------------------------------------------------------------------------------

def select_evidence(runs, commit):
    """Latest push run on main per required workflow for this commit; all must have succeeded."""
    evidence = []
    for name in EVIDENCE_WORKFLOWS:
        matching = [run for run in runs if run.get("name") == name and run.get("head_sha") == commit
                    and run.get("head_branch") == "main" and run.get("event") == "push"]
        if not matching:
            raise ManifestError(f"no main push run of {name!r} for {commit}")
        latest = max(matching, key=lambda run: run["id"])
        if latest.get("conclusion") != "success":
            raise ManifestError(f"{name!r} did not succeed on {commit}")
        evidence.append({"workflow": name, "runId": latest["id"], "url": latest.get("html_url"),
                         "headSha": latest["head_sha"], "conclusion": latest["conclusion"]})
    return evidence


DEVICE_EVIDENCE = {
    "status": "not-claimed",
    "note": "CI verified the source, checksums and signature only. Two-phone text/photo/voice over Wi-Fi and BLE, "
            "fresh-install/upgrade retention and recovery remain tracked on the issues below and are not "
            "established by this manifest.",
    "tracking": [
        "https://github.com/hanthor/indiafoss-chat-android/issues/45",
        "https://github.com/hanthor/indiafoss-chat-android/issues/49",
    ],
}


# --- build -------------------------------------------------------------------------------------------

def build_manifest(args):
    dry_run = args.dry_run
    env = os.environ
    repo_root = Path(args.repo_root)
    record = check_provenance(repo_root, args.aar)
    if args.aar is None and not dry_run:
        raise ManifestError("--aar is required so the .aar checksum is verified against the downloaded file")
    apk = Path(args.apk)
    identity = apk_identity(apk)
    declared, schemes = declared_certificate(apk)
    apksigner = find_apksigner(args.apksigner)
    if apksigner:
        verified = apksigner_certificate(apksigner, apk)
        if verified != declared:
            raise ManifestError("signing block certificate disagrees with apksigner")
        verified_by = "apksigner"
    elif dry_run:
        verified_by = "signing-block-only"
    else:
        raise ManifestError("apksigner is required to verify the APK signature before publication")
    if args.expected_signer or env.get("NIGHTLY_CERT_SHA256"):
        expected = normalise_fingerprint(args.expected_signer or env["NIGHTLY_CERT_SHA256"], "expected signer")
        if declared != expected:
            raise ManifestError("APK signer does not match the configured preview certificate")
    elif not dry_run:
        raise ManifestError("no expected signing certificate configured (NIGHTLY_CERT_SHA256)")
    if not dry_run:
        if identity["packageName"] != PREVIEW_PACKAGE:
            raise ManifestError(f"APK package is {identity['packageName']}, expected {PREVIEW_PACKAGE}")
        if str(identity["versionCode"]) != env.get("NIGHTLY_CODE") or identity["versionName"] != env.get("NIGHTLY_VERSION"):
            raise ManifestError("APK versionCode/versionName do not match the preview identity")
    commit = env.get("GITHUB_SHA")
    repository = env.get("GITHUB_REPOSITORY")
    server = env.get("GITHUB_SERVER_URL", "https://github.com")
    run_id = env.get("GITHUB_RUN_ID")
    if not dry_run and not (commit and COMMIT_RE.fullmatch(commit) and repository and run_id):
        raise ManifestError("GITHUB_SHA, GITHUB_REPOSITORY and GITHUB_RUN_ID are required outside --dry-run")
    if args.runs:
        runs = json.loads(Path(args.runs).read_text()).get("workflow_runs", [])
        ci_evidence = select_evidence(runs, commit)
    elif dry_run:
        ci_evidence = []
    else:
        raise ManifestError("--runs is required so the manifest links the CI evidence for this commit")
    manifest = {
        "schemaVersion": SCHEMA_VERSION,
        "dryRun": dry_run,
        "generatedAt": datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0).isoformat(),
        "generator": "scripts/release-manifest.py",
        "chat": {
            "repository": repository,
            "commit": commit,
            "url": f"{server}/{repository}/commit/{commit}" if repository and commit else None,
        },
        "build": {
            "workflow": env.get("GITHUB_WORKFLOW"),
            "runId": int(run_id) if run_id else None,
            "runNumber": int(env["GITHUB_RUN_NUMBER"]) if env.get("GITHUB_RUN_NUMBER") else None,
            "runAttempt": int(env["GITHUB_RUN_ATTEMPT"]) if env.get("GITHUB_RUN_ATTEMPT") else None,
            "url": f"{server}/{repository}/actions/runs/{run_id}" if repository and run_id else None,
        },
        "neutrino": {**record["neutrino"], "url": f"https://github.com/{record['neutrino']['repository']}/commit/{record['neutrino']['rev']}"},
        "neutrinoIroh": {**record["neutrinoIroh"], "url": f"https://github.com/{record['neutrinoIroh']['repository']}/commit/{record['neutrinoIroh']['rev']}"},
        "bindings": {**record["bindings"], "verifiedAgainstDownload": args.aar is not None},
        "apk": {
            "filename": apk.name,
            "sha256": sha256_file(apk),
            "sizeBytes": apk.stat().st_size,
            **identity,
            "flavour": args.flavour,
            "buildType": args.build_type,
            "abi": args.abi,
        },
        "signing": {
            "certificateSha256": declared,
            "schemes": schemes,
            "verifiedBy": verified_by,
        },
        "evidence": {"ci": ci_evidence, "device": DEVICE_EVIDENCE},
    }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"Wrote {output}: {identity['packageName']} {identity['versionName']} ({identity['versionCode']}) "
          f"signed by {declared} ({verified_by}), bindings {record['bindings']['version']}")
    return manifest


# --- verify ------------------------------------------------------------------------------------------

def load_manifest(path):
    try:
        manifest = json.loads(Path(path).read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise ManifestError(f"cannot read manifest {path}: {error}") from error
    if manifest.get("schemaVersion") != SCHEMA_VERSION:
        raise ManifestError(f"unsupported manifest schemaVersion {manifest.get('schemaVersion')!r}")
    for section in ("chat", "build", "neutrino", "neutrinoIroh", "bindings", "apk", "signing", "evidence"):
        if not isinstance(manifest.get(section), dict):
            raise ManifestError(f"manifest is missing the {section} section")
    validate_provenance_shape({"bindings": manifest["bindings"], "neutrino": manifest["neutrino"],
                               "neutrinoIroh": manifest["neutrinoIroh"]})
    normalise_fingerprint(manifest["signing"].get("certificateSha256"), "manifest signing certificate")
    if not SHA256_RE.fullmatch(manifest["apk"].get("sha256") or ""):
        raise ManifestError("manifest apk sha256 must be 64 lowercase hex characters")
    return manifest


def verify_pair(args):
    manifest = load_manifest(args.manifest)
    report = []
    if manifest.get("dryRun") and not args.allow_dry_run:
        raise ManifestError("manifest is a dry run, not a publication (pass --allow-dry-run to inspect it)")
    apk = Path(args.apk)
    actual_sha = sha256_file(apk)
    if actual_sha != manifest["apk"]["sha256"]:
        raise ManifestError(f"APK SHA-256 {actual_sha} does not match the manifest")
    report.append(f"sha256 {actual_sha}")
    if manifest["apk"].get("sizeBytes") not in (None, apk.stat().st_size):
        raise ManifestError("APK size does not match the manifest")
    identity = apk_identity(apk)
    for key in ("packageName", "versionCode", "versionName"):
        if identity[key] != manifest["apk"].get(key):
            raise ManifestError(f"APK {key} {identity[key]!r} does not match the manifest {manifest['apk'].get(key)!r}")
    report.append(f"{identity['packageName']} {identity['versionName']} versionCode {identity['versionCode']}")
    declared, _ = declared_certificate(apk)
    expected = manifest["signing"]["certificateSha256"]
    if declared != expected:
        raise ManifestError("APK signing certificate does not match the manifest")
    if args.expect_signer and declared != normalise_fingerprint(args.expect_signer, "--expect-signer"):
        raise ManifestError("APK signing certificate does not match --expect-signer")
    apksigner = None if args.no_apksigner else find_apksigner(args.apksigner)
    if apksigner:
        if apksigner_certificate(apksigner, apk) != declared:
            raise ManifestError("apksigner reports a different certificate than the signing block")
        report.append(f"signer {declared} (signature verified by apksigner)")
    elif args.no_apksigner:
        report.append(f"signer {declared} (WARNING: certificate read from the signing block; signature not verified)")
    else:
        raise ManifestError("apksigner not found; pass --apksigner PATH, set ANDROID_HOME, or opt out with --no-apksigner")
    if not manifest.get("dryRun"):
        commit = manifest["chat"].get("commit") or ""
        if not COMMIT_RE.fullmatch(commit):
            raise ManifestError("manifest chat commit is not a full commit SHA")
        ci = manifest["evidence"].get("ci") or []
        present = {entry.get("workflow") for entry in ci}
        missing = [name for name in EVIDENCE_WORKFLOWS if name not in present]
        if missing:
            raise ManifestError(f"manifest lacks CI evidence for {', '.join(missing)}")
        for entry in ci:
            if entry.get("conclusion") != "success" or entry.get("headSha") != commit:
                raise ManifestError(f"CI evidence for {entry.get('workflow')!r} is not a successful run of {commit}")
        if manifest["signing"].get("verifiedBy") != "apksigner":
            raise ManifestError("manifest signature was not verified by apksigner at build time")
        if manifest["bindings"].get("verifiedAgainstDownload") is not True:
            raise ManifestError("manifest .aar checksum was not verified against the downloaded file at build time")
        report.append(f"evidence: {len(ci)} CI runs of {commit[:7]}")
    if args.previous:
        previous = load_manifest(args.previous)
        if previous["apk"].get("packageName") != identity["packageName"]:
            raise ManifestError("previous manifest is for a different package")
        if previous["signing"]["certificateSha256"] != declared:
            raise ManifestError("signing certificate changed since the previous manifest; an in-place update is impossible")
        if not (isinstance(previous["apk"].get("versionCode"), int) and identity["versionCode"] > previous["apk"]["versionCode"]):
            raise ManifestError(f"versionCode {identity['versionCode']} is not greater than the previous {previous['apk'].get('versionCode')}")
        if previous["apk"].get("versionName") == identity["versionName"]:
            raise ManifestError("versionName is unchanged since the previous manifest")
        report.append(f"versionCode increased from {previous['apk']['versionCode']}")
    report.append(f"bindings {manifest['bindings']['version']} .aar {manifest['bindings']['sha256'][:12]}… "
                  f"neutrino {manifest['neutrino']['rev'][:7]} neutrino-iroh {manifest['neutrinoIroh']['rev'][:7]}")
    for line in report:
        print(f"ok: {line}")
    return manifest


# --- CLI ---------------------------------------------------------------------------------------------

def parse_args(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)

    provenance = commands.add_parser("check-provenance", help="cross-check the pinned Neutrino bindings")
    provenance.add_argument("--repo-root", default=".")
    provenance.add_argument("--aar", help="downloaded .aar to hash against the record")

    build = commands.add_parser("build", help="write release-manifest.json for a signed APK")
    build.add_argument("--apk", required=True)
    build.add_argument("--output", required=True)
    build.add_argument("--repo-root", default=".")
    build.add_argument("--aar", help="downloaded .aar; required unless --dry-run")
    build.add_argument("--apksigner", help="path to apksigner (default: $APKSIGNER or newest build-tools)")
    build.add_argument("--runs", help="JSON from GET /repos/{repo}/actions/runs?head_sha=…; required unless --dry-run")
    build.add_argument("--expected-signer", help="certificate SHA-256 the APK must be signed with (default: $NIGHTLY_CERT_SHA256)")
    build.add_argument("--flavour", default="fdroid")
    build.add_argument("--build-type", default="release")
    build.add_argument("--abi", default="universal")
    build.add_argument("--dry-run", action="store_true",
                       help="PR CI mode: no publication identity, evidence or apksigner required; the manifest says dryRun: true")

    verify = commands.add_parser("verify", help="validate a downloaded APK + manifest pair offline")
    verify.add_argument("--apk", required=True)
    verify.add_argument("--manifest", required=True)
    verify.add_argument("--previous", help="the previously accepted manifest; versionCode must increase and the signer must not change")
    verify.add_argument("--expect-signer", help="certificate SHA-256 the APK must be signed with")
    verify.add_argument("--apksigner", help="path to apksigner (default: $APKSIGNER or newest build-tools)")
    verify.add_argument("--no-apksigner", action="store_true", help="skip cryptographic signature verification (prints a warning)")
    verify.add_argument("--allow-dry-run", action="store_true", help="accept a manifest with dryRun: true")
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    if args.command == "check-provenance":
        record = check_provenance(args.repo_root, args.aar)
        print(f"ok: bindings {record['bindings']['version']} sha256 {record['bindings']['sha256']}"
              f"{' verified against ' + args.aar if args.aar else ''}")
    elif args.command == "build":
        build_manifest(args)
    else:
        verify_pair(args)


if __name__ == "__main__":
    try:
        main()
    except (ManifestError, OSError, zipfile.BadZipFile, struct.error, KeyError, IndexError) as error:
        print(f"release-manifest: refused: {error}", file=sys.stderr)
        sys.exit(1)
