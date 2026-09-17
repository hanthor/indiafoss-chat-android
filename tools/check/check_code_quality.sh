#!/usr/bin/env bash

# Copyright (c) 2025 Element Creations Ltd.
# Copyright 2023-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
# Please see LICENSE files in the repository root for full details.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"

TMP_DIR="${REPO_ROOT}/tmp"
mkdir -p "${TMP_DIR}"

searchForbiddenStringsScript="${TMP_DIR}/search_forbidden_strings.pl"
EXPECTED_SHA256="1dc0e0e4c954c92d549ca0f4864591f848b58357c032fe157e5cd56c89d79c7c"

verify_checksum() {
  local file="$1"
  local actual_sha256
  actual_sha256="$(sha256sum "$file" 2>/dev/null | awk '{print $1}')"
  [[ "$actual_sha256" == "$EXPECTED_SHA256" ]]
}

if [[ -f ${searchForbiddenStringsScript} ]] && verify_checksum "${searchForbiddenStringsScript}"; then
  echo "${searchForbiddenStringsScript} already there and verified"
else
  echo "Downloading search_forbidden_strings.pl securely..."
  tmp_download="${searchForbiddenStringsScript}.tmp.$$"
  download_url="https://raw.githubusercontent.com/matrix-org/matrix-dev-tools/develop/bin/search_forbidden_strings.pl"
  
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$download_url" -o "$tmp_download"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$download_url" -O "$tmp_download"
  else
    echo "ERROR: Neither curl nor wget is available to download quality check dependencies." >&2
    exit 1
  fi

  if verify_checksum "$tmp_download"; then
    mv "$tmp_download" "${searchForbiddenStringsScript}"
    chmod u+x "${searchForbiddenStringsScript}"
    echo "Successfully verified search_forbidden_strings.pl checksum."
  else
    echo "ERROR: SHA256 checksum verification failed for search_forbidden_strings.pl" >&2
    rm -f "$tmp_download"
    exit 1
  fi
fi

echo
echo "Search for forbidden patterns in Kotlin source files..."

# list all Kotlin folders of the project.
allKotlinDirs=$(find "${REPO_ROOT}" -type d | grep -v build | grep -v '\.git' | grep -v '\.gradle' | grep 'kotlin$' || true)

if [[ -n "${allKotlinDirs}" ]]; then
  "${searchForbiddenStringsScript}" "${REPO_ROOT}/tools/check/forbidden_strings_in_code.txt" ${allKotlinDirs}
fi

echo
echo "Search for forbidden patterns in XML resource files..."

# list all res folders of the project.
allResDirs=$(find "${REPO_ROOT}" -type d | grep -v build | grep -v '\.git' | grep -v '\.gradle' | grep '/res$' || true)

if [[ -n "${allResDirs}" ]]; then
  "${searchForbiddenStringsScript}" "${REPO_ROOT}/tools/check/forbidden_strings_in_xml.txt" ${allResDirs}
fi

echo "OK"
