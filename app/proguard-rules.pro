# قواعد R8 للمشروع. أضف هنا ما يلزم لاحقًا (Firebase وغيره).

# Start.io: تمنع R8 من حذف/تغيير أسماء كلاسات المكتبة (تتحمل أحيانًا بالانعكاس).
# مكتوبة من الذاكرة؛ إن أصدرت Start.io قواعد رسمية أحدث فاستبدل بها.
-keep class com.startapp.** { *; }
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod
-dontwarn com.startapp.**
-dontwarn org.jetbrains.annotations.**
