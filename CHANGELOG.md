# سجل التغييرات

يتبع الإصدار صيغة `MAJOR.MINOR.PATCH`، ويزداد `versionCode` مع كل إصدار منشور.

## [غير منشور]

### أُضيف
- workflow للبناء التجريبي (debug) بدون أسرار، للتطوير فقط.
- لعبة «ربط الكلمات»: 16 كلمة في 4 مجموعات، 4 أخطاء مسموحة، تلميح «قريب»، 17 لغزًا، لعبة اليوم، وإحصاءات محلية على الجهاز.
- هيكل مشروع Android جديد: `com.khuyutalarab.game` (versionCode 1، versionName 0.1.0، minSdk 26).
- سكربت `scripts/verify-signing.sh` للتحقق من بصمات Keystore وAPK وAAB.
- سير عمل GitHub Actions للبناء الموقّع والتحقق قبل النشر.
- إعدادات توقيع Gradle تعتمد على متغيرات البيئة.
- `.gitignore` يمنع رفع المفاتيح والأسرار.
- README وCHANGELOG.
