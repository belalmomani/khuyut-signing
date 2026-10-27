#!/usr/bin/env python3
"""يفحص أن google-services.json يطابق applicationId وأن بصمة SHA-1 المعتمدة مسجّلة في Firebase.

الاستخدام: python3 scripts/check-firebase.py
لا يعدّل أي ملف ولا يطبع مفاتيح API.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXPECTED_SHA1 = "BD:18:EE:26:0B:CB:C2:AA:7D:02:C6:BE:9A:26:AF:C3:E5:E5:6B:5D"
expected_hash = EXPECTED_SHA1.replace(":", "").lower()  # الصيغة المستخدمة في google-services.json

gradle = (ROOT / "app" / "build.gradle.kts").read_text(encoding="utf-8")
m = re.search(r'applicationId\s*=\s*"([^"]+)"', gradle)
if not m:
    sys.exit("❌ تعذّر إيجاد applicationId في app/build.gradle.kts")
app_id = m.group(1)

gs_path = ROOT / "app" / "google-services.json"
if not gs_path.exists():
    sys.exit("❌ app/google-services.json غير موجود")
data = json.loads(gs_path.read_text(encoding="utf-8"))

client = next(
    (c for c in data.get("client", [])
     if c["client_info"]["android_client_info"]["package_name"] == app_id),
    None,
)
if client is None:
    pkgs = [c["client_info"]["android_client_info"]["package_name"] for c in data.get("client", [])]
    sys.exit(f"❌ لا يوجد عميل بالحزمة {app_id} في google-services.json. الموجود: {pkgs}")
print(f"✅ package_name يطابق applicationId: {app_id}")

hashes = {
    o.get("android_info", {}).get("certificate_hash", "").lower()
    for o in client.get("oauth_client", [])
}
hashes.discard("")
if expected_hash in hashes:
    print("✅ بصمة SHA-1 المعتمدة مسجّلة في Firebase")
else:
    print("❌ بصمة SHA-1 المعتمدة غير مسجّلة في google-services.json")
    print(f"   المسجّل: {sorted(hashes) if hashes else 'لا شيء'}")
    print("   سجّل SHA-1 وSHA-256 في Firebase ثم نزّل الملف من جديد.")
    sys.exit(1)
