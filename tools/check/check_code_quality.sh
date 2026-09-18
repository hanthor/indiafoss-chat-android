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
scriptUrl="https://raw.githubusercontent.com/matrix-org/matrix-dev-tools/develop/bin/search_forbidden_strings.pl"
# The script runs with our source tree as input, so it is pinned by digest:
# a changed upstream fails here instead of running unreviewed code.
scriptSha256="1dc0e0e4c954c92d549ca0f4864591f848b58357c032fe157e5cd56c89d79c7c"

verifyScript() {
  local actual
  actual="$(sha256sum "$1" | awk '{print $1}')"
  [[ "${actual}" == "${scriptSha256}" ]]
}

if [[ -f ${searchForbiddenStringsScript} ]] && verifyScript "${searchForbiddenStringsScript}"; then
  echo "${searchForbiddenStringsScript} already there and verified"
else
  echo "Get the script"
  download="${searchForbiddenStringsScript}.download"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "${scriptUrl}" -o "${download}"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "${scriptUrl}" -O "${download}"
  else
    echo "❌ ERROR: neither curl nor wget is available to fetch ${scriptUrl}" >&2
    exit 1
  fi
  if ! verifyScript "${download}"; then
    echo "❌ ERROR: ${scriptUrl} does not match the pinned SHA-256 ${scriptSha256}; review it before updating the pin." >&2
    rm -f "${download}"
    exit 1
  fi
  mv "${download}" "${searchForbiddenStringsScript}"
  chmod u+x "${searchForbiddenStringsScript}"
fi

echo
echo "Search for forbidden patterns in Kotlin source files..."

# list all Kotlin folders of the project.
allKotlinDirs=$(find "${REPO_ROOT}" -type d | grep -v build | grep -v '\.git' | grep -v '\.gradle' | grep 'kotlin$' || true)

if [[ -n "${allKotlinDirs}" ]]; then
  "${searchForbiddenStringsScript}" "${REPO_ROOT}/tools/check/forbidden_strings_in_code.txt" "$allKotlinDirs"
fi

echo
echo "Search for forbidden patterns in XML resource files..."

# list all res folders of the project.
allResDirs=$(find "${REPO_ROOT}" -type d | grep -v build | grep -v '\.git' | grep -v '\.gradle' | grep '/res$' || true)

if [[ -n "${allResDirs}" ]]; then
  "${searchForbiddenStringsScript}" "${REPO_ROOT}/tools/check/forbidden_strings_in_xml.txt" "$allResDirs"
fi

echo "OK"
