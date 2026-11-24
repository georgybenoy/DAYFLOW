import os
import sys
import json
import urllib.request
import urllib.parse
import subprocess

def get_github_token():
    # Query git credential helper
    proc = subprocess.Popen(['git', 'credential', 'fill'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
    out, _ = proc.communicate('protocol=https\nhost=github.com\n')
    creds = dict(line.split('=', 1) for line in out.strip().splitlines() if '=' in line)
    return creds.get('password')

def main():
    token = get_github_token()
    if not token:
        print("Error: Could not retrieve GitHub token from git credentials.")
        sys.exit(1)

    owner = "georgybenoy"
    repo = "DAYFLOW"
    tag_name = "v1.0.0"

    release_body = """# ⚡ DAYFLOW v1.0.0 — Offline-First Daily Productivity & Execution

DAYFLOW is a modern, web-first and offline-first personal productivity application packaged for Android via Capacitor. Designed for deep execution, habit building, structured reminders, and deterministic daily planning.

---

### 📦 Download & Installation

| Asset | Platform | Architecture | File Size | SHA-256 Checksum |
| :--- | :--- | :--- | :--- | :--- |
| **[`DayFlow.apk`](https://github.com/georgybenoy/DAYFLOW/releases/download/v1.0.0/DayFlow.apk)** | Android 7.0+ (API 24+) | Universal (`arm64-v8a`, `armeabi-v7a`, `x86_64`) | **4.41 MB** | `F8369CF44231F261D1FDBD8BEC8E23B2D9CE8EADBC2DB7D387F8EBFD1061E58D` |

#### How to Install on Android
1. Download **`DayFlow.apk`** from the Assets section below directly on your phone.
2. Tap the downloaded file to install (enable *"Install from Unknown Sources / Allow from this source"* if prompted).
3. Launch **DAYFLOW** — completely offline, private, and zero account required.

---

### ✨ Highlights & Features

- **📱 100% Offline-First Architecture:** Zero backend, zero cloud tracking, zero AI dependencies. All user data is persisted locally.
- **🎨 Custom "D Check" Branding:** Vibrant cyan-to-mint-to-gold gradient emblem with clean negative-space checkmark cutout.
- **🛡️ Android Notch & Safe Area Insets:** Fully optimized header and navigation spacing via `@capacitor/status-bar` and CSS environment insets.
- **⏰ Smart Reminder & Recurrence Engine:** One-time, daily, weekly, custom weekday, and interval reminders with snooze, skip, and pause.
- **🎯 Deterministic Daily Planner:** Algorithmic schedule generator factoring in deadlines, priority levels, duration, and availability.
- **📊 Real Data Habit Tracker:** Binary, quantity, and duration tracking with streak calculation and execution scoring.
- **💾 Dual-Engine Local Persistence:** High-performance IndexedDB for browser preview and SQLite for Android native storage.
- **⚡ Web & Mobile Parity:** Full desktop browser experience at `http://localhost:5173` without requiring an Android emulator.

---

### 📋 Release Metadata
- **Tag:** `v1.0.0`
- **Application ID:** `com.dayflow.app`
- **Min SDK:** Android 7.0 (API 24)
- **Target SDK:** Android 14 (API 34)
- **SHA-256:** `F8369CF44231F261D1FDBD8BEC8E23B2D9CE8EADBC2DB7D387F8EBFD1061E58D`
"""

    headers = {
        'Authorization': f'token {token}',
        'User-Agent': 'DayFlow-Agent',
        'Accept': 'application/vnd.github.v3+json'
    }

    # 1. Check if release already exists for this tag
    check_url = f"https://api.github.com/repos/{owner}/{repo}/releases/tags/{tag_name}"
    req = urllib.request.Request(check_url, headers=headers)
    release = None
    try:
        with urllib.request.urlopen(req) as resp:
            release = json.loads(resp.read())
            print(f"Found existing release for {tag_name} (ID: {release['id']})")
    except urllib.error.HTTPError as e:
        if e.code != 404:
            raise

    # 2. Create release if not exists, or update it
    if release is None:
        create_url = f"https://api.github.com/repos/{owner}/{repo}/releases"
        payload = {
            "tag_name": tag_name,
            "target_commitish": "main",
            "name": "DAYFLOW v1.0.0 — Offline-First Daily Productivity & Execution",
            "body": release_body,
            "draft": False,
            "prerelease": False
        }
        req = urllib.request.Request(
            create_url,
            data=json.dumps(payload).encode('utf-8'),
            headers={**headers, 'Content-Type': 'application/json'}
        )
        with urllib.request.urlopen(req) as resp:
            release = json.loads(resp.read())
            print(f"Created new release '{release['name']}' (ID: {release['id']})")
    else:
        # Update existing release
        update_url = f"https://api.github.com/repos/{owner}/{repo}/releases/{release['id']}"
        payload = {
            "name": "DAYFLOW v1.0.0 — Offline-First Daily Productivity & Execution",
            "body": release_body,
            "draft": False,
            "prerelease": False
        }
        req = urllib.request.Request(
            update_url,
            data=json.dumps(payload).encode('utf-8'),
            headers={**headers, 'Content-Type': 'application/json'}
        )
        with urllib.request.urlopen(req) as resp:
            release = json.loads(resp.read())
            print(f"Updated release '{release['name']}'")

    release_id = release['id']
    upload_url_template = release['upload_url'] # e.g. https://uploads.github.com/.../assets{?name,label}
    base_upload_url = upload_url_template.split('{')[0]

    # 3. Check existing assets and delete old DayFlow.apk if present
    assets_url = f"https://api.github.com/repos/{owner}/{repo}/releases/{release_id}/assets"
    req = urllib.request.Request(assets_url, headers=headers)
    with urllib.request.urlopen(req) as resp:
        assets = json.loads(resp.read())
        for asset in assets:
            if asset['name'] == 'DayFlow.apk':
                print(f"Deleting previous DayFlow.apk asset (ID: {asset['id']})...")
                del_req = urllib.request.Request(
                    f"https://api.github.com/repos/{owner}/{repo}/releases/assets/{asset['id']}",
                    headers=headers,
                    method='DELETE'
                )
                urllib.request.urlopen(del_req)

    # 4. Upload DayFlow.apk
    apk_path = "DayFlow.apk"
    if not os.path.exists(apk_path):
        print(f"Error: {apk_path} not found.")
        sys.exit(1)

    apk_size = os.path.getsize(apk_path)
    print(f"Uploading {apk_path} ({apk_size} bytes / {apk_size / (1024*1024):.2f} MB)...")

    with open(apk_path, 'rb') as f:
        apk_data = f.read()

    upload_url = f"{base_upload_url}?name=DayFlow.apk"
    upload_req = urllib.request.Request(
        upload_url,
        data=apk_data,
        headers={
            **headers,
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Length': str(len(apk_data))
        }
    )

    with urllib.request.urlopen(upload_req) as resp:
        asset_info = json.loads(resp.read())
        print(f"Successfully uploaded DayFlow.apk!")
        print(f"Download URL: {asset_info['browser_download_url']}")

    print("\n--- Release Published Successfully! ---")
    print(f"View Release at: {release['html_url']}")

if __name__ == '__main__':
    main()
