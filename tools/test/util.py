#!/usr/bin/env python3

# Copyright (c) 2025 Element Creations Ltd.
# Copyright 2024, 2025 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
# Please see LICENSE files in the repository root for full details.

import filecmp
from pathlib import Path


def compare(file1: str | Path, file2: str | Path) -> bool:
    """Compare two files, return True if different, False if identical."""
    p1 = Path(file1)
    p2 = Path(file2)

    if not p1.exists() or not p2.exists():
        return True

    if p1.stat().st_size != p2.stat().st_size:
        return True

    return not filecmp.cmp(p1, p2, shallow=False)

