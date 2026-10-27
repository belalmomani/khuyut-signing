# خيوط العرب — البناء والتوقيع

معرّف التطبيق (`applicationId`): `com.khuyutalarab.game` — ثابت ولا يتغير.

## أول مرة

المستودع لا يحتوي `gradlew` بعد. من جهازك (مع Android Studio أو Gradle 8.9+):

```bash
gradle wrapper --gradle-version 8.9
```

ثم أضف الملفات الناتجة (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) إلى المستودع. بدونها يعمل سير GitHub Actions عبر Gradle 8.9 مباشرة.

## البصمات المعتمدة

| الخوارزمية | القيمة |
|---|---|
| SHA-1 | `BD:18:EE:26:0B:CB:C2:AA:7D:02:C6:BE:9A:26:AF:C3:E5:E5:6B:5D` |
| SHA-256 | `A6:E6:B7:DA:63:D0:CB:3C:92:A6:1A:5B:36:E1:0A:A1:29:BB:06:55:9E:86:A0:AB:CA:2C:58:64:AA:55:CC:89` |

البصمات للتحقق فقط ولا تغني عن ملف المفتاح الأصلي.

## قواعد ثابتة

- لا يُنشأ مفتاح جديد ولا يُستبدل المفتاح الأصلي. فقدانه يعني استحالة تحديث التطبيق على المستخدمين الحاليين.
- ملف الـ Keystore وكلمات مروره خارج المستودع دائمًا.
- `applicationId` ثابت، و`versionCode` يزداد في كل إصدار.

## أين أجد ملف المفتاح؟

- ابحث على جهازك عن `*.jks` أو `*.keystore` (Windows: بحث الملفات؛ macOS/Linux: `find ~ -name "*.jks" -o -name "*.keystore"`).
- راجع مجلدات Android Studio القديمة، والنسخ الاحتياطية، والبريد الذي أرسلت إليه المفتاح، والتخزين السحابي.
- إن كنت قد فعّلت Play App Signing فاطلب من Play Console شهادة **Upload key** أو راجع صفحة App signing.
- بعد العثور عليه، تحقق منه:

```bash
KEYSTORE_PASSWORD='***' KEY_ALIAS='alias' ./scripts/verify-signing.sh keystore /path/to/release.jks
```

(اكتب الأمر من جهازك أنت، ولا ترسل كلمة المرور في أي محادثة.)

## النسخ الاحتياطي الآمن

1. انسخ الملف إلى ما لا يقل عن مكانين مستقلين (قرص خارجي مشفّر + مدير كلمات مرور أو خزنة مشفّرة).
2. احفظ كلمات المرور والـ alias في مدير كلمات مرور، منفصلة عن الملف.
3. لا ترفعه إلى GitHub (حتى لو كان المستودع خاصًا).
4. اختبر استعادة النسخة دوريًا بتشغيل `verify-signing.sh keystore`.

## البناء المحلي

أنشئ ملف `keystore.properties` في جذر المشروع (مستثنى في `.gitignore`):

```
storeFile=/absolute/path/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

ثم:

```bash
./gradlew assembleRelease bundleRelease
./scripts/verify-signing.sh apk app/build/outputs/apk/release/app-release.apk
./scripts/verify-signing.sh aab app/build/outputs/bundle/release/app-release.aab
```

## البناء الآلي (GitHub Actions)

أضف في Settings › Secrets and variables › Actions:

- `KEYSTORE_BASE64` — ناتج `base64 -w0 release.jks`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

يعمل سير العمل عند دفع وسم `v*`: يتحقق من المفتاح، يبني، يتحقق من APK وAAB، ثم ينشر Release. أي فشل يوقف النشر. الأسرار لا تُطبع في السجلات.

## Firebase

- `package_name` في `google-services.json` يجب أن يطابق `applicationId`.
- يجب أن تكون SHA-1 وSHA-256 أعلاه مسجلة في إعدادات التطبيق في Firebase (وبصمة Play App Signing إن استُخدمت).
- لا تُبدَّل بيانات مشروع Firebase الحالي.
- للفحص: `python3 scripts/check-firebase.py` (يفشل إن لم تُسجَّل SHA-1 المعتمدة؛ ويعمل أيضًا ضمن GitHub Actions).

## التحديث فوق إصدار سابق

1. نفس `applicationId`.
2. نفس شهادة التوقيع.
3. `versionCode` أكبر من السابق.
4. لا تغيّر أسماء قواعد البيانات أو مفاتيح التخزين دون ترحيل (migration).
5. جرّب: ثبّت الإصدار السابق → ثبّت الجديد فوقه بدون حذف → تأكد من بقاء البيانات.
