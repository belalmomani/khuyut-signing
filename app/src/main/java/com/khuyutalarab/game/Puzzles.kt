package com.khuyutalarab.game

/** مجموعة من 4 كلمات يجمعها رابط. level: 0 سهل (أصفر) … 3 صعب (بنفسجي). */
data class Group(val title: String, val words: List<String>, val level: Int)

data class Puzzle(val groups: List<Group>) {
    val allWords: List<String> get() = groups.flatMap { it.words }
}

object Puzzles {
    val all: List<Puzzle> = listOf(
        Puzzle(listOf(
            Group("عواصم عربية", listOf("القاهرة", "الرياض", "عمان", "بغداد"), 0),
            Group("فواكه", listOf("تفاح", "موز", "عنب", "رمان"), 1),
            Group("ألوان", listOf("أحمر", "أزرق", "أخضر", "أصفر"), 2),
            Group("حيوانات مفترسة", listOf("أسد", "نمر", "ذئب", "فهد"), 3),
        )),
        Puzzle(listOf(
            Group("فصول السنة", listOf("صيف", "شتاء", "ربيع", "خريف"), 0),
            Group("وسائل نقل", listOf("سيارة", "قطار", "طائرة", "سفينة"), 1),
            Group("أدوات مدرسية", listOf("قلم", "ممحاة", "مسطرة", "دفتر"), 2),
            Group("أجزاء الجسم", listOf("يد", "رجل", "عين", "أنف"), 3),
        )),
        Puzzle(listOf(
            Group("مشروبات", listOf("شاي", "قهوة", "عصير", "حليب"), 0),
            Group("أثاث", listOf("كرسي", "طاولة", "سرير", "خزانة"), 1),
            Group("أنهار", listOf("النيل", "دجلة", "الفرات", "بردى"), 2),
            Group("مدن سعودية", listOf("جدة", "مكة", "الدمام", "أبها"), 3),
        )),
        Puzzle(listOf(
            Group("مهن", listOf("طبيب", "معلم", "مهندس", "نجار"), 0),
            Group("أدوات مطبخ", listOf("ملعقة", "سكين", "صحن", "قدر"), 1),
            Group("كواكب", listOf("عطارد", "زحل", "المريخ", "الزهرة"), 2),
            Group("أسماء أولاد", listOf("خالد", "سامي", "يوسف", "عمر"), 3),
        )),
        Puzzle(listOf(
            Group("أطباق عربية", listOf("كبسة", "مندي", "منسف", "مقلوبة"), 0),
            Group("حلويات", listOf("كنافة", "بقلاوة", "قطايف", "بسبوسة"), 1),
            Group("خضروات", listOf("خيار", "جزر", "بطاطس", "باذنجان"), 2),
            Group("توابل", listOf("كمون", "قرفة", "هيل", "زعفران"), 3),
        )),
    )
}
