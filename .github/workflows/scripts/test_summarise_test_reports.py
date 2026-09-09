from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from summarise_test_reports import summary


class ReportSummaryTest(unittest.TestCase):
    def test_root_merged_and_module_reports_are_counted(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            for name in (
                "build/reports/kover/reportMerged.xml",
                "build/reports/kover/htmlMerged/index.html",
                "app/build/reports/kover/report.xml",
                "libraries/androidutils/build/test-results/testDebugUnitTest/TEST-DiffCacheTest.xml",
                "libraries/androidutils/build/reports/tests/testDebugUnitTest/index.html",
            ):
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("report")
            result = summary(root, "success")
            self.assertIn("step: passed", result)
            self.assertIn("Kover XML reports: 2", result)
            self.assertIn("Kover HTML entry points: 1", result)
            self.assertIn("JUnit XML results: 1", result)
            self.assertIn("Unit-test HTML entry points: 1", result)

    def test_missing_reports_do_not_imply_success(self):
        with TemporaryDirectory() as directory:
            result = summary(Path(directory), "skipped")
            self.assertIn("step: not run", result)
            self.assertEqual(result.count("not generated; this is not a passing result"), 4)

    def test_partial_results_do_not_hide_gradle_failure(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "app/build/test-results/testDebugUnitTest/TEST-Example.xml"
            path.parent.mkdir(parents=True)
            path.write_text("report")
            result = summary(root, "failure")
            self.assertIn("step: failed (reports may be partial)", result)
            self.assertIn("JUnit XML results: 1", result)
            self.assertIn("Kover XML reports: not generated", result)


if __name__ == "__main__":
    unittest.main()
