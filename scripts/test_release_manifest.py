"""release-manifest.py: parser, provenance, build and verify regressions on synthetic APKs."""
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import struct
import tempfile
import unittest
from unittest import mock
import zipfile

spec = importlib.util.spec_from_file_location("release_manifest", Path(__file__).with_name("release-manifest.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

AAR_SHA = "26cf81315af5d9f06ee0c6a12d15578570f1a218e3191aa8406b9cd83d4fc3af"
NEUTRINO_REV = "2d85348ee5a0086c3f30725a31b68439f4fe89b4"
IROH_REV = "15117e9816b259ac156acfb1a45caebacafce530"
VERSION = "0.8.2-e2ee.2d85348-ble.15117e9"
COMMIT = "9677296eaf92666929635110bbdef2ab36d72166"
CERT = b"\x30\x06\x02\x01\x01\x02\x01\x02"  # any DER-looking bytes; only its digest matters
CERT_SHA = hashlib.sha256(CERT).hexdigest()


def provenance():
    return {
        "bindings": {"version": VERSION, "artifact": f"neutrino-bindings-{VERSION}.aar", "sha256": AAR_SHA,
                     "source": "https://example.invalid/release"},
        "neutrino": {"repository": "hanthor/neutrino", "branch": "e2ee-key-transport", "rev": NEUTRINO_REV},
        "neutrinoIroh": {"repository": "hanthor/neutrino-iroh", "ref": "neutrino-kit-15117e9", "rev": IROH_REV},
    }


# --- synthetic binary AndroidManifest.xml ------------------------------------------------------------

def _chunk(chunk_type, header_rest, body):
    header = struct.pack("<HHI", chunk_type, 8 + len(header_rest), 8 + len(header_rest) + len(body)) + header_rest
    return header + body


def _string_pool(strings, utf8):
    encoded, offsets, position = [], [], 0
    for text in strings:
        if utf8:
            raw = text.encode("utf-8")
            item = bytes([len(text), len(raw)]) + raw + b"\x00"
        else:
            raw = text.encode("utf-16-le")
            item = struct.pack("<H", len(text)) + raw + b"\x00\x00"
        offsets.append(position)
        encoded.append(item)
        position += len(item)
    body = struct.pack(f"<{len(strings)}I", *offsets) + b"".join(encoded)
    strings_start = 28 + 4 * len(strings)
    header_rest = struct.pack("<IIIII", len(strings), 0, (1 << 8) if utf8 else 0, strings_start, 0)
    return _chunk(module.RES_STRING_POOL_TYPE, header_rest, body)


def _start_element(strings, name, attributes):
    body = struct.pack("<IIHHHHHH", 0xFFFFFFFF, strings.index(name), 20, 20, len(attributes), 0, 0, 0)
    for attr_name, value in attributes:
        if isinstance(value, int):
            body += struct.pack("<IIIHBBI", 0, strings.index(attr_name), 0xFFFFFFFF, 8, 0, module.TYPE_INT_DEC, value)
        else:
            body += struct.pack("<IIIHBBI", 0, strings.index(attr_name), strings.index(value), 8, 0,
                                module.TYPE_STRING, strings.index(value))
    return _chunk(module.RES_XML_START_ELEMENT_TYPE, struct.pack("<II", 1, 0xFFFFFFFF), body)


def make_axml(package="org.indiafoss.chat", version_code=202700010, version_name="2026.1-abcdef0", utf8=True):
    strings = ["manifest", "package", "versionCode", "versionName", "uses-sdk", "minSdkVersion", package, version_name]
    elements = _start_element(strings, "manifest", [("versionCode", version_code), ("versionName", version_name),
                                                     ("package", package)])
    elements += _start_element(strings, "uses-sdk", [("minSdkVersion", 24)])
    body = _string_pool(strings, utf8) + elements
    return struct.pack("<HHI", module.RES_XML_TYPE, 8, 8 + len(body)) + body


# --- synthetic APK Signing Block ---------------------------------------------------------------------

def _prefixed(payload):
    return struct.pack("<I", len(payload)) + payload


def _signer(certificate, v3):
    signed_data = _prefixed(b"") + _prefixed(_prefixed(certificate))
    if v3:
        signed_data += struct.pack("<II", 24, 0x7FFFFFFF)
    signed_data += _prefixed(b"")
    signer = _prefixed(signed_data)
    if v3:
        signer += struct.pack("<II", 24, 0x7FFFFFFF)
    return _prefixed(signer + _prefixed(b"") + _prefixed(b"pubkey"))


def make_signing_block(signers_by_scheme):
    pairs = b""
    for scheme_id, certificates in signers_by_scheme.items():
        v3 = scheme_id != 0x7109871A
        value = _prefixed(b"".join(_signer(certificate, v3) for certificate in certificates))
        pairs += struct.pack("<QI", 4 + len(value), scheme_id) + value
    size = len(pairs) + 24
    return struct.pack("<Q", size) + pairs + struct.pack("<Q", size) + module.APK_SIG_BLOCK_MAGIC


def make_apk(path, axml=None, signers=None, extra=b""):
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        archive.writestr("AndroidManifest.xml", axml if axml is not None else make_axml())
        archive.writestr("classes.dex", b"dex\n" + extra)
    data = buffer.getvalue()
    if signers is None:
        signers = {0x7109871A: [CERT], 0xF05368C0: [CERT]}
    if signers:
        cd_offset, eocd = module.find_central_directory(data)
        block = make_signing_block(signers)
        data = data[:cd_offset] + block + data[cd_offset:]
        eocd += len(block)
        data = data[:eocd + 16] + struct.pack("<I", cd_offset + len(block)) + data[eocd + 20:]
    Path(path).write_bytes(data)
    return path


def make_repo(root, record=None, catalog_version=VERSION, script_sha=AAR_SHA, aar=True):
    root = Path(root)
    (root / module.PROVENANCE_PATH).parent.mkdir(parents=True, exist_ok=True)
    (root / module.CATALOG_PATH).parent.mkdir(parents=True, exist_ok=True)
    (root / module.PROVENANCE_PATH).write_text(json.dumps(record or provenance()))
    (root / module.CATALOG_PATH).write_text(f'[versions]\nneutrino = "{catalog_version}"\n')
    (root / module.BUILD_SCRIPT_PATH).write_text(f'val neutrinoSha256 = "{script_sha}"\n')
    if aar:
        aar_path = root / "libs" / f"neutrino-bindings-{VERSION}.aar"
        aar_path.parent.mkdir(parents=True)
        aar_path.write_bytes(b"aar")
        return root, aar_path
    return root, None


def runs_json(path, commit=COMMIT, conclusion="success"):
    runs = [{"id": index, "name": name, "head_sha": commit, "head_branch": "main", "event": "push",
             "conclusion": conclusion, "html_url": f"https://example.invalid/runs/{index}"}
            for index, name in enumerate(module.EVIDENCE_WORKFLOWS, start=1)]
    Path(path).write_text(json.dumps({"workflow_runs": runs}))
    return path


class ParserTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)

    def tearDown(self):
        self.directory.cleanup()

    def test_reads_identity_from_binary_manifest(self):
        for utf8 in (True, False):
            with self.subTest(utf8=utf8):
                apk = make_apk(self.root / f"{utf8}.apk", axml=make_axml(utf8=utf8))
                self.assertEqual(module.apk_identity(apk), {
                    "packageName": "org.indiafoss.chat", "versionCode": 202700010,
                    "versionName": "2026.1-abcdef0", "minSdkVersion": 24})

    def test_reads_certificate_from_every_scheme(self):
        apk = make_apk(self.root / "a.apk", signers={0x7109871A: [CERT], 0xF05368C0: [CERT], 0x1B93AD61: [CERT]})
        self.assertEqual(module.signing_block_certificates(apk), {"v2": [CERT_SHA], "v3": [CERT_SHA], "v3.1": [CERT_SHA]})
        self.assertEqual(module.declared_certificate(apk), (CERT_SHA, ["v2", "v3", "v3.1"]))

    def test_rejects_unsigned_multiple_and_disagreeing_signers(self):
        for label, signers in (
            ("unsigned", {}),
            ("two signers", {0x7109871A: [CERT, b"other"]}),
            ("disagreeing schemes", {0x7109871A: [CERT], 0xF05368C0: [b"other"]}),
        ):
            with self.subTest(label=label), self.assertRaises(module.ManifestError):
                module.declared_certificate(make_apk(self.root / "b.apk", signers=signers))


class ProvenanceTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)

    def tearDown(self):
        self.directory.cleanup()

    def test_consistent_record_passes_with_and_without_aar(self):
        aar_sha = hashlib.sha256(b"aar").hexdigest()
        record = provenance()
        record["bindings"]["sha256"] = aar_sha
        root, aar = make_repo(self.root, record=record, script_sha=aar_sha)
        self.assertEqual(module.check_provenance(root)["bindings"]["version"], VERSION)
        self.assertEqual(module.check_provenance(root, aar)["bindings"]["sha256"], aar_sha)

    def test_every_disagreement_is_refused(self):
        wrong_rev = dict(provenance(), neutrino={"repository": "hanthor/neutrino", "rev": "f" * 40})
        short_rev = dict(provenance(), neutrinoIroh={"repository": "hanthor/neutrino-iroh", "rev": "15117e9"})
        for label, kwargs in (
            ("catalog version", {"catalog_version": "0.8.2-e2ee.0000000-ble.15117e9"}),
            ("build script checksum", {"script_sha": "0" * 64}),
            ("rev not encoded in version", {"record": wrong_rev}),
            ("short rev", {"record": short_rev}),
            ("downloaded aar hash", {}),
        ):
            with tempfile.TemporaryDirectory() as directory, self.subTest(label=label):
                root, aar = make_repo(directory, **kwargs)
                with self.assertRaises(module.ManifestError):
                    module.check_provenance(root, aar)


class BuildAndVerifyTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)
        aar_sha = hashlib.sha256(b"aar").hexdigest()
        record = provenance()
        record["bindings"]["sha256"] = aar_sha
        self.repo, self.aar = make_repo(self.root / "repo", record=record, script_sha=aar_sha)
        self.apk = make_apk(self.root / "indiafoss-chat-android.apk")
        self.runs = runs_json(self.root / "runs.json")
        self.output = self.root / "release-manifest.json"
        self.env = {"GITHUB_SHA": COMMIT, "GITHUB_REPOSITORY": "hanthor/indiafoss-chat-android", "GITHUB_RUN_ID": "77",
                    "GITHUB_RUN_NUMBER": "1", "GITHUB_RUN_ATTEMPT": "1", "GITHUB_WORKFLOW": "Publish Android preview",
                    "NIGHTLY_CODE": "202700010", "NIGHTLY_VERSION": "2026.1-abcdef0", "NIGHTLY_CERT_SHA256": CERT_SHA}

    def tearDown(self):
        self.directory.cleanup()

    def build(self, *extra, env=None, apksigner=CERT_SHA):
        argv = ["build", "--apk", str(self.apk), "--output", str(self.output), "--repo-root", str(self.repo), *extra]
        with mock.patch.dict(os.environ, env if env is not None else self.env, clear=True), \
                mock.patch.object(module, "find_apksigner", return_value="/fake/apksigner" if apksigner else None), \
                mock.patch.object(module, "apksigner_certificate", return_value=apksigner):
            module.main(argv)
        return json.loads(self.output.read_text())

    def verify(self, *extra, apksigner=CERT_SHA):
        argv = ["verify", "--apk", str(self.apk), "--manifest", str(self.output), *extra]
        with mock.patch.dict(os.environ, {}, clear=True), \
                mock.patch.object(module, "find_apksigner", return_value="/fake/apksigner" if apksigner else None), \
                mock.patch.object(module, "apksigner_certificate", return_value=apksigner):
            return module.main(argv)

    def test_publication_manifest_round_trips(self):
        manifest = self.build("--aar", str(self.aar), "--runs", str(self.runs))
        self.assertFalse(manifest["dryRun"])
        self.assertEqual(manifest["chat"]["commit"], COMMIT)
        self.assertEqual(manifest["build"]["url"], "https://github.com/hanthor/indiafoss-chat-android/actions/runs/77")
        self.assertEqual(manifest["neutrino"]["rev"], NEUTRINO_REV)
        self.assertEqual(manifest["neutrinoIroh"]["rev"], IROH_REV)
        self.assertEqual(manifest["bindings"]["version"], VERSION)
        self.assertTrue(manifest["bindings"]["verifiedAgainstDownload"])
        self.assertEqual(manifest["apk"]["sha256"], module.sha256_file(self.apk))
        self.assertEqual(manifest["apk"]["versionCode"], 202700010)
        self.assertEqual(manifest["apk"]["flavour"], "fdroid")
        self.assertEqual(manifest["signing"], {"certificateSha256": CERT_SHA, "schemes": ["v2", "v3"], "verifiedBy": "apksigner"})
        self.assertEqual([entry["workflow"] for entry in manifest["evidence"]["ci"]], list(module.EVIDENCE_WORKFLOWS))
        self.assertEqual(manifest["evidence"]["device"]["status"], "not-claimed")
        self.verify("--expect-signer", CERT_SHA)

    def test_publication_build_fails_closed(self):
        failing_runs = runs_json(self.root / "failing.json", conclusion="failure")
        other_commit_runs = runs_json(self.root / "other.json", commit="0" * 40)
        for label, extra, env, apksigner in (
            ("no aar", ["--runs", str(self.runs)], self.env, CERT_SHA),
            ("no runs", ["--aar", str(self.aar)], self.env, CERT_SHA),
            ("no apksigner", ["--aar", str(self.aar), "--runs", str(self.runs)], self.env, None),
            ("apksigner disagrees", ["--aar", str(self.aar), "--runs", str(self.runs)], self.env, "b" * 64),
            ("wrong expected signer", ["--aar", str(self.aar), "--runs", str(self.runs)],
             dict(self.env, NIGHTLY_CERT_SHA256="c" * 64), CERT_SHA),
            ("no expected signer", ["--aar", str(self.aar), "--runs", str(self.runs)],
             {key: value for key, value in self.env.items() if key != "NIGHTLY_CERT_SHA256"}, CERT_SHA),
            ("identity mismatch", ["--aar", str(self.aar), "--runs", str(self.runs)], dict(self.env, NIGHTLY_CODE="1"), CERT_SHA),
            ("failing evidence", ["--aar", str(self.aar), "--runs", str(failing_runs)], self.env, CERT_SHA),
            ("evidence for another commit", ["--aar", str(self.aar), "--runs", str(other_commit_runs)], self.env, CERT_SHA),
            ("no github identity", ["--aar", str(self.aar), "--runs", str(self.runs)],
             {key: value for key, value in self.env.items() if not key.startswith("GITHUB_")}, CERT_SHA),
        ):
            with self.subTest(label=label), self.assertRaises(module.ManifestError):
                self.build(*extra, env=env, apksigner=apksigner)
            self.assertFalse(self.output.exists(), label)

    def test_dry_run_needs_nothing_but_the_apk_and_is_not_a_publication(self):
        manifest = self.build("--dry-run", env={}, apksigner=None)
        self.assertTrue(manifest["dryRun"])
        self.assertIsNone(manifest["chat"]["commit"])
        self.assertEqual(manifest["signing"]["verifiedBy"], "signing-block-only")
        self.assertEqual(manifest["evidence"]["ci"], [])
        with self.assertRaises(module.ManifestError):
            self.verify()
        self.verify("--allow-dry-run")
        self.verify("--allow-dry-run", "--no-apksigner", apksigner=None)

    def test_verify_refuses_tampering_and_unverifiable_signatures(self):
        self.build("--aar", str(self.aar), "--runs", str(self.runs))
        original = self.apk.read_bytes()
        cases = {
            "content changed": lambda: make_apk(self.apk, extra=b"x"),
            "identity changed": lambda: make_apk(self.apk, axml=make_axml(version_code=1)),
            "signer changed": lambda: make_apk(self.apk, signers={0x7109871A: [b"other"]}),
        }
        for label, tamper in cases.items():
            with self.subTest(label=label):
                tamper()
                with self.assertRaises(module.ManifestError):
                    self.verify()
                self.apk.write_bytes(original)
        with self.assertRaises(module.ManifestError):
            self.verify("--expect-signer", "d" * 64)
        with self.assertRaises(module.ManifestError):
            self.verify(apksigner=None)  # apksigner missing and no explicit opt-out
        with self.assertRaises(module.ManifestError):
            self.verify(apksigner="e" * 64)  # apksigner disagrees with the signing block
        self.verify("--no-apksigner", apksigner=None)

    def test_verify_rejects_a_manifest_whose_evidence_was_edited(self):
        manifest = self.build("--aar", str(self.aar), "--runs", str(self.runs))
        for label, mutate in (
            ("evidence removed", lambda m: m["evidence"].update(ci=[])),
            ("evidence for another commit", lambda m: m["evidence"]["ci"][0].update(headSha="0" * 40)),
            ("signature unverified at build", lambda m: m["signing"].update(verifiedBy="signing-block-only")),
            ("aar unverified at build", lambda m: m["bindings"].update(verifiedAgainstDownload=False)),
            ("rev shortened", lambda m: m["neutrino"].update(rev="2d85348")),
            ("schema bumped", lambda m: m.update(schemaVersion=2)),
        ):
            with self.subTest(label=label):
                edited = json.loads(json.dumps(manifest))
                mutate(edited)
                self.output.write_text(json.dumps(edited))
                with self.assertRaises(module.ManifestError):
                    self.verify()

    def test_previous_manifest_enforces_monotonic_code_and_stable_signer(self):
        self.build("--aar", str(self.aar), "--runs", str(self.runs))
        current = json.loads(self.output.read_text())
        previous_path = self.root / "previous.json"

        def previous(**apk_or_signing):
            record = json.loads(json.dumps(current))
            record["apk"].update({"versionCode": 202700000, "versionName": "2026.0-0000000", "sha256": "0" * 64})
            for section, values in apk_or_signing.items():
                record[section].update(values)
            previous_path.write_text(json.dumps(record))
            return previous_path

        self.verify("--previous", str(previous()))
        for label, kwargs in (
            ("same code", {"apk": {"versionCode": 202700010}}),
            ("higher code", {"apk": {"versionCode": 202700020}}),
            ("same name", {"apk": {"versionName": "2026.1-abcdef0"}}),
            ("other package", {"apk": {"packageName": "org.indiafoss.chat.debug"}}),
            ("other signer", {"signing": {"certificateSha256": "f" * 64}}),
        ):
            with self.subTest(label=label), self.assertRaises(module.ManifestError):
                self.verify("--previous", str(previous(**kwargs)))


if __name__ == "__main__":
    unittest.main()
