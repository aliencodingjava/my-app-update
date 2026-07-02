#!/usr/bin/env python3
import json
import os
import textwrap
import sys
import base64
import binascii

import google.auth.transport.requests
import requests
from google.oauth2 import service_account

def normalize_private_key(raw_key: str) -> str:
    private_key = raw_key.strip().strip('"')
    private_key = private_key.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\r\n", "\n")

    begin = "-----BEGIN PRIVATE KEY-----"
    end = "-----END PRIVATE KEY-----"
    if begin not in private_key or end not in private_key:
        return private_key

    before_end = private_key.split(end, 1)[0]
    body = before_end.split(begin, 1)[1]
    body = "".join(body.split())
    if not body:
        return private_key

    return f"{begin}\n{textwrap.fill(body, 64)}\n{end}\n"


def load_service_account_info(raw_json: str) -> dict:
    try:
        loaded = json.loads(raw_json)
    except json.JSONDecodeError:
        try:
            decoded = base64.b64decode(raw_json, validate=True).decode("utf-8")
        except (binascii.Error, UnicodeDecodeError):
            raise
        loaded = json.loads(decoded)

    if not isinstance(loaded, dict):
        raise ValueError("service account secret must be a JSON object")

    return loaded


def describe_private_key_shape(private_key: str) -> str:
    begin = "-----BEGIN PRIVATE KEY-----"
    end = "-----END PRIVATE KEY-----"
    has_begin = begin in private_key
    has_end = end in private_key
    body_chars = 0
    base64_like = False
    if has_begin and has_end:
        body = private_key.split(begin, 1)[1].split(end, 1)[0]
        body = "".join(body.split())
        body_chars = len(body)
        base64_like = bool(body) and all(
            char.isalnum() or char in "+/=" for char in body
        )

    return (
        f"private_key_chars={len(private_key)}, "
        f"private_key_newlines={private_key.count(chr(10))}, "
        f"has_begin={has_begin}, has_end={has_end}, "
        f"body_chars={body_chars}, base64_like={base64_like}"
    )


project_id = os.environ.get("FCM_PROJECT_ID")
service_account_json = os.environ.get("FCM_SERVICE_ACCOUNT_JSON")
if not project_id or not service_account_json:
    print("FCM secrets not configured; skipping push notification.")
    sys.exit(0)

try:
    service_account_info = load_service_account_info(service_account_json)
except Exception as exc:
    print(
        "FCM_SERVICE_ACCOUNT_JSON is not valid raw JSON or base64 JSON; "
        f"skipping push notification. {type(exc).__name__}: {exc}"
    )
    sys.exit(0)

private_key = normalize_private_key(str(service_account_info.get("private_key", "")))
service_account_info["private_key"] = private_key

if "-----BEGIN PRIVATE KEY-----" not in str(service_account_info.get("private_key", "")):
    print(
        "FCM_SERVICE_ACCOUNT_JSON does not look like a Firebase service-account key; "
        "skipping push notification."
    )
    sys.exit(0)

print(
    "Loaded FCM service-account metadata: "
    f"project_id={service_account_info.get('project_id', '<missing>')}, "
    f"client_email={service_account_info.get('client_email', '<missing>')}, "
    f"private_key_id={'present' if service_account_info.get('private_key_id') else 'missing'}"
)

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
        "account JSON. "
        f"Safe private-key shape: {describe_private_key_shape(private_key)}. "
        f"Cause: {type(exc).__name__}: {exc}"
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
