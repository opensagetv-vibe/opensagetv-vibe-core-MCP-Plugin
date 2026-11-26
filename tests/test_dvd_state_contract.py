"""Only the documented stock read APIs may supply DVD closure evidence."""
from pathlib import Path
import unittest


class DvdStateContractTests(unittest.TestCase):
    def test_stock_dvd_read_api_names(self):
        root = Path(__file__).resolve().parents[1]
        source = (root / "src/main/java/org/opensagetv/vibe/coremcp/ControlService.java").read_text()
        for field, api in (("dvdTitle", "GetDVDCurrentTitle"),
                           ("dvdChapter", "GetDVDCurrentChapter"),
                           ("dvdAngle", "GetDVDCurrentAngle"),
                           ("dvdAngleCount", "GetDVDNumberOfAngles")):
            self.assertIn(f'out.put("{field}", safeNumber(context, "{api}"))', source)
        self.assertNotIn('"GetDVDTitleNumber"', source)
        self.assertNotIn('"GetDVDChapterNumber"', source)


if __name__ == "__main__":
    unittest.main()
