#!/usr/bin/env python3
import os
import re
from datetime import datetime, timezone
from pathlib import Path

version_code = os.environ["VERSION_CODE"]
version_name = os.environ["VERSION_NAME"]
release_date = os.environ.get("RELEASE_DATE") or datetime.now(timezone.utc).strftime("%b-%d-%Y")

gradle_file = Path("app/build.gradle.kts")
text = gradle_file.read_text(encoding="utf-8")
text = re.sub(r"versionCode\s*=\s*\d+", f"versionCode = {version_code}", text, count=1)
text = re.sub(r'versionName\s*=\s*"[^"]+"', f'versionName = "{version_name}"', text, count=1)
text = re.sub(
    r'buildConfigField\("String",\s*"RELEASE_DATE",\s*"\\"[^"]+\\""\)',
    f'buildConfigField("String", "RELEASE_DATE", "\\"{release_date}\\"")',
    text,
    count=1,
)
gradle_file.write_text(text, encoding="utf-8")
