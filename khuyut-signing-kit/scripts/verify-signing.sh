#!/usr/bin/env bash
# verify-signing.sh — التحقق من بصمات شهادة التوقيع لـ «خيوط العرب»
#
# الاستخدام:
#   KEYSTORE_PASSWORD=... KEY_ALIAS=... ./scripts/verify-signing.sh keystore path/to/release.jks
#   ./scripts/verify-signing.sh apk path/to/app-release.apk
#   ./scripts/verify-signing.sh aab path/to/app-release.aab
#
# لا يطبع كلمات المرور أبدًا، ولا ينشئ أي مفتاح. عند أي اختلاف يخرج برمز خطأ.

set -euo pipefail

EXPECTED_SHA1="BD:18:EE:26:0B:CB:C2:AA:7D:02:C6:BE:9A:26:AF:C3:E5:E5:6B:5D"
EXPECTED_SHA256="A6:E6:B7:DA:63:D0:CB:3C:92:A6:1A:5B:36:E1:0A:A1:29:BB:06:55:9E:86:A0:AB:CA:2C:58:64:AA:55:CC:89"

norm() { tr -d ': \r\n' | tr 'a-f' 'A-F'; }

die() { echo "❌ $*" >&2; exit 1; }

[ $# -eq 2 ] || die "الاستخدام: $0 {keystore|apk|aab} <path>"
mode="$1"; target="$2"
[ -f "$target" ] || die "الملف غير موجود: $target"

find_apksigner() {
  if command -v apksigner >/dev/null 2>&1; then command -v apksigner; return; fi
  local root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  [ -n "$root" ] || return 1
  ls -1 "$root"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -n1
}

case "$mode" in
  keystore)
    [ -n "${KEYSTORE_PASSWORD:-}" ] || die "عيّن KEYSTORE_PASSWORD كمتغير بيئة (لا تمرره كوسيط)."
    args=(-list -v -keystore "$target" -storepass:env KEYSTORE_PASSWORD)
    [ -n "${KEY_ALIAS:-}" ] && args+=(-alias "$KEY_ALIAS")
    out="$(keytool "${args[@]}" 2>&1)" || die "فشل keytool (كلمة مرور خاطئة أو alias غير صحيح)."
    actual_sha1="$(printf '%s\n' "$out" | grep -m1 -E '^\s*SHA1:' | sed 's/.*SHA1://' | norm)"
    actual_sha256="$(printf '%s\n' "$out" | grep -m1 -E '^\s*SHA256:' | sed 's/.*SHA256://' | norm)"
    ;;
  apk)
    tool="$(find_apksigner)" || die "apksigner غير موجود. ثبّت Android build-tools أو عيّن ANDROID_HOME."
    out="$("$tool" verify --verbose --print-certs "$target" 2>&1)" || die "توقيع APK غير صالح."
    printf '%s\n' "$out" | grep -q '^Verifies' || die "apksigner لم يؤكد صحة التوقيع."
    actual_sha1="$(printf '%s\n' "$out" | grep -m1 'certificate SHA-1 digest' | sed 's/.*: //' | norm)"
    actual_sha256="$(printf '%s\n' "$out" | grep -m1 'certificate SHA-256 digest' | sed 's/.*: //' | norm)"
    ;;
  aab)
    out="$(keytool -printcert -jarfile "$target" 2>&1)" || die "تعذّر قراءة شهادة AAB (قد يكون غير موقّع)."
    actual_sha1="$(printf '%s\n' "$out" | grep -m1 -E '^\s*SHA1:' | sed 's/.*SHA1://' | norm)"
    actual_sha256="$(printf '%s\n' "$out" | grep -m1 -E '^\s*SHA256:' | sed 's/.*SHA256://' | norm)"
    ;;
  *) die "وضع غير معروف: $mode" ;;
esac

[ -n "${actual_sha1:-}" ] && [ -n "${actual_sha256:-}" ] || die "تعذّر استخراج البصمات."

exp1="$(printf '%s' "$EXPECTED_SHA1" | norm)"
exp256="$(printf '%s' "$EXPECTED_SHA256" | norm)"

fail=0
if [ "$actual_sha1" = "$exp1" ]; then echo "✅ SHA-1   مطابقة"; else echo "❌ SHA-1   غير مطابقة"; fail=1; fi
if [ "$actual_sha256" = "$exp256" ]; then echo "✅ SHA-256 مطابقة"; else echo "❌ SHA-256 غير مطابقة"; fail=1; fi

if [ "$fail" -ne 0 ]; then
  echo "البصمات الفعلية (علنية وغير سرية):"
  echo "  SHA-1:   $actual_sha1"
  echo "  SHA-256: $actual_sha256"
  die "الشهادة لا تطابق المعتمدة. هذا البناء غير صالح للنشر. لا تنشئ مفتاحًا بديلًا."
fi
echo "✅ الشهادة الفعلية تطابق البصمات المعتمدة."
