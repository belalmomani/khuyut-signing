#!/usr/bin/env bash
# setup-secrets.sh — يشغَّل على جهازك أنت (وليس في أي محادثة).
# يتحقق من أن المفتاح هو الأصلي (مطابقة البصمات)، ثم يرفع الأسرار الأربعة إلى GitHub.
#
# المتطلبات: keytool (JDK)، gh (GitHub CLI) مسجَّل الدخول: gh auth login
# الاستخدام:  ./scripts/setup-secrets.sh /path/to/release.jks owner/repo
#
# كلمات المرور تُدخل مخفية، ولا تُطبع ولا تُحفظ في ملف.

set -euo pipefail

[ $# -eq 2 ] || { echo "الاستخدام: $0 <keystore> <owner/repo>" >&2; exit 1; }
KS="$1"; REPO="$2"
[ -f "$KS" ] || { echo "❌ الملف غير موجود: $KS" >&2; exit 1; }
command -v gh >/dev/null || { echo "❌ ثبّت GitHub CLI (gh) وسجّل الدخول: gh auth login" >&2; exit 1; }
command -v keytool >/dev/null || { echo "❌ keytool غير موجود (ثبّت JDK)" >&2; exit 1; }

HERE="$(cd "$(dirname "$0")" && pwd)"

read -rsp "كلمة مرور ملف المفتاح (KEYSTORE_PASSWORD): " KEYSTORE_PASSWORD; echo
export KEYSTORE_PASSWORD

echo "الأسماء (alias) الموجودة في الملف:"
keytool -list -keystore "$KS" -storepass:env KEYSTORE_PASSWORD 2>/dev/null \
  | grep -E 'PrivateKeyEntry|trustedCertEntry' | sed 's/,.*//' || {
    echo "❌ تعذّرت القراءة: كلمة المرور خاطئة أو الملف تالف." >&2; exit 1; }

read -rp "اكتب الـ alias المطلوب: " KEY_ALIAS
export KEY_ALIAS

read -rsp "كلمة مرور المفتاح (KEY_PASSWORD) — اتركها فارغة إن كانت نفس كلمة الملف: " KEY_PASSWORD; echo
[ -n "$KEY_PASSWORD" ] || KEY_PASSWORD="$KEYSTORE_PASSWORD"

echo "جارٍ التحقق من مطابقة البصمات..."
bash "$HERE/verify-signing.sh" keystore "$KS" \
  || { echo "⛔ لن تُرفع الأسرار: هذا ليس المفتاح الأصلي." >&2; exit 1; }

echo "رفع الأسرار إلى $REPO ..."
base64 < "$KS" | tr -d '\n' | gh secret set KEYSTORE_BASE64 --repo "$REPO"
printf '%s' "$KEYSTORE_PASSWORD" | gh secret set KEYSTORE_PASSWORD --repo "$REPO"
printf '%s' "$KEY_ALIAS"         | gh secret set KEY_ALIAS --repo "$REPO"
printf '%s' "$KEY_PASSWORD"      | gh secret set KEY_PASSWORD --repo "$REPO"

unset KEYSTORE_PASSWORD KEY_PASSWORD
echo "✅ أُضيفت الأسرار الأربعة. شغّل workflow النشر من تبويب Actions."
