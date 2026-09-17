#!/usr/bin/env bash

# Copyright (c) 2025 Element Creations Ltd.
# Copyright 2023-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
# Please see LICENSE files in the repository root for full details.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"

echo "Zipping the contents of the 'files' directory..."

# Ensure tmp folder exists
mkdir -p "${REPO_ROOT}/tmp"

rm -f "${REPO_ROOT}/tmp/file_templates.zip"
pushd "${SCRIPT_DIR}/files" > /dev/null
zip -r "${REPO_ROOT}/tmp/file_templates.zip" .
popd > /dev/null
