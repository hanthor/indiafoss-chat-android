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

if [[ -f ${searchForbiddenStringsScript} ]]; then
  echo "${searchForbiddenStringsScript} already there"
else
  echo "Get the script"
  wget https://raw.githubusercontent.com/matrix-org/matrix-dev-tools/develop/bin/search_forbidden_strings.pl -O "${searchForbiddenStringsScript}"
fi

if [[ -x ${searchForbiddenStringsScript} ]]; then
  echo "${searchForbiddenStringsScript} is already executable"
else
  echo "Make the script executable"
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
