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

credentials = service_account.Credentials.from_service_account_info(
    json.loads(service_account_json),
    scopes=["https://www.googleapis.com/auth/firebase.messaging"],
)
credentials.refresh(google.auth.transport.requests.Request())

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
    sys.stderr.write(response.text)
    response.raise_for_status()

print("Sent app update push notification to topic.")
