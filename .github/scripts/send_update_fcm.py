#!/usr/bin/env python3
import json
import os
import sys

import google.auth.transport.requests
import requests
from google.oauth2 import service_account

project_id = os.environ.get("FCM_PROJECT_ID")
service_account_json = os.environ.get("FCM_SERVICE_ACCOUNT_JSON")
if not project_id or not service_account_json:
    print("FCM secrets not configured; skipping push notification.")
    sys.exit(0)

try:
    service_account_info = json.loads(service_account_json)
except json.JSONDecodeError as exc:
    print(f"FCM_SERVICE_ACCOUNT_JSON is not valid JSON; skipping push notification. {exc}")
    sys.exit(0)

private_key = str(service_account_info.get("private_key", ""))
if "\\n" in private_key:
    service_account_info["private_key"] = private_key.replace("\\n", "\n")

if "-----BEGIN PRIVATE KEY-----" not in str(service_account_info.get("private_key", "")):
    print(
        "FCM_SERVICE_ACCOUNT_JSON does not look like a Firebase service-account key; "
        "skipping push notification."
    )
    sys.exit(0)

try:
    credentials = service_account.Credentials.from_service_account_info(
        service_account_info,
        scopes=["https://www.googleapis.com/auth/firebase.messaging"],
    )
    credentials.refresh(google.auth.transport.requests.Request())
except Exception as exc:
    print(
        "Could not load FCM service-account credentials; skipping push notification. "
        "Replace the FCM_SERVICE_ACCOUNT_JSON secret with the full Firebase service "
        f"account JSON. Cause: {type(exc).__name__}: {exc}"
    )
    sys.exit(0)

message = {
    "message": {
        "topic": os.environ.get("FCM_TOPIC", "app_updates"),
        "android": {"priority": "HIGH"},
        "data": {
            "type": "app_update",
            "versionCode": os.environ["VERSION_CODE"],
            "versionName": os.environ["VERSION_NAME"],
            "apkUrl": os.environ["APK_URL"],
        },
    }
}

response = requests.post(
    f"https://fcm.googleapis.com/v1/projects/{project_id}/messages:send",
    headers={
        "Authorization": f"Bearer {credentials.token}",
        "Content-Type": "application/json; UTF-8",
    },
    json=message,
    timeout=30,
)
if response.status_code >= 300:
    print(f"FCM push request failed; skipping push notification. {response.text}")
    sys.exit(0)

print("Sent app update push notification to topic.")
