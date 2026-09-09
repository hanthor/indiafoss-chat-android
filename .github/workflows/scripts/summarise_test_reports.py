#!/usr/bin/env python3
"""Describe report availability separately from the Gradle step outcome."""
from pathlib import Path
import sys


def summary(root: Path, outcome: str) -> str:
    outcomes = {
        "success": "passed",
        "failure": "failed (reports may be partial)",
        "cancelled": "cancelled (reports may be partial)",
        "skipped": "not run",
        "": "not run",
    }
    lines = ["## Test execution", "", f"Gradle test/coverage step: {outcomes.get(outcome, 'unknown')}.", ""]
    groups = {
        "Kover XML reports": "**/build/reports/kover/**/*.xml",
        "Kover HTML entry points": "**/build/reports/kover/**/index.html",
        "JUnit XML results": "**/build/test-results/*UnitTest/*.xml",
        "Unit-test HTML entry points": "**/build/reports/tests/*UnitTest/index.html",
    }
    for label, pattern in groups.items():
        count = sum(1 for path in root.glob(pattern) if path.is_file())
        availability = str(count) if count else "not generated; this is not a passing result"
        lines.append(f"- {label}: {availability}")
    lines.extend(["", "Available reports are uploaded with 14-day retention. Report presence does not imply the full suite passed."])
    return "\n".join(lines)


if __name__ == "__main__":
    print(summary(Path(sys.argv[1]), sys.argv[2]))
