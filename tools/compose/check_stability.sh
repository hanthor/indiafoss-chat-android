#!/usr/bin/env bash

# Copyright (c) 2025 Element Creations Ltd.
# Copyright 2025 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
# Please see LICENSE files in the repository root for full details.

set -e

# Build the project with compose report
echo "Building the project with compose report..."
./gradlew assembleGplayDebug -PcomposeCompilerReports=true -PcomposeCompilerMetrics=true --stacktrace

echo "Checking stability of State classes..."
found_unstable=0
# List all the files ending with -classes.txt
while read -r file; do
    if grep -E 'unstable class .*State \{' "$file"; then
        echo "❌ ERROR: Found unstable State class in $file"
        found_unstable=1
    fi
done < <(find . -type f -name "*-classes.txt")

if [[ "$found_unstable" -eq 1 ]]; then
    exit 1
fi
