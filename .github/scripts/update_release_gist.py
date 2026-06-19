#!/usr/bin/env python3
import json
import os
import sys
import urllib.error
import urllib.request

gist_id = os.environ["UPDATE_GIST_ID"]
filename = os.environ.get("UPDATE_GIST_FILENAME", "gistfile2.txt")
token = os.environ["GIST_TOKEN"]
version_code = int(os.environ["VERSION_CODE"])
version_name = os.environ["VERSION_NAME"]
apk_url = os.environ["APK_URL"]
update_title = os.environ.get("UPDATE_TITLE", "").strip()
update_body = os.environ.get("UPDATE_BODY", "").strip()

api_url = f"https://api.github.com/gists/{gist_id}"
headers = {
    "Accept": "application/vnd.github+json",
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json",
    "X-GitHub-Api-Version": "2022-11-28",
}


def request(method, url, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        sys.stderr.write(exc.read().decode("utf-8", errors="replace"))
        raise


gist = request("GET", api_url)
current_content = gist.get("files", {}).get(filename, {}).get("content", "{}")
try:
    payload = json.loads(current_content)
except json.JSONDecodeError:
    payload = {}

payload["versionCode"] = version_code
payload["versionName"] = version_name
payload["apkUrl"] = apk_url

updates = payload.get("updates")
if not isinstance(updates, list):
    updates = []
if update_title or update_body:
    updates.insert(
        0,
        {
            "title": update_title or f"Version {version_name}",
            "body": update_body or f"Version {version_name} is ready to download.",
        },
    )

deduped_updates = []
seen_updates = set()
for update in updates:
    if not isinstance(update, dict):
        continue
    title = str(update.get("title", "")).strip()
    body = str(update.get("body", "")).strip()
    if not title and not body:
        continue
    key = (title, body)
    if key in seen_updates:
        continue
    seen_updates.add(key)
    deduped_updates.append({"title": title, "body": body})

payload["updates"] = deduped_updates

request(
    "PATCH",
    api_url,
    {
        "files": {
            filename: {
                "content": json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
            }
        }
    },
)
print(f"Updated gist {gist_id}/{filename} to {version_name} ({version_code})")
