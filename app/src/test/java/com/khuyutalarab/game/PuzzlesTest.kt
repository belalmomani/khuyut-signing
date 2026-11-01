package com.khuyutalarab.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** يتحقق من سلامة بيانات الألغاز عند كل بناء، فلا يصل لغز معطوب إلى اللاعب. */
class PuzzlesTest {

    @Test
    fun everyPuzzleHasFourGroupsOfFourUniqueWords() {
        Puzzles.all.forEachIndexed { i, p ->
            val n = i + 1
            assertEquals("اللغز $n: عدد المجموعات", 4, p.groups.size)
            p.groups.forEach { g ->
                assertEquals("اللغز $n، «${g.title}»: عدد الكلمات", 4, g.words.size)
            }
            assertEquals("اللغز $n: الكلمات الـ16 يجب ألا تتكرر", 16, p.allWords.toSet().size)
        }
    }

    @Test
    fun everyPuzzleUsesAllFourDifficultyLevelsOnce() {
        Puzzles.all.forEachIndexed { i, p ->
            assertEquals("اللغز ${i + 1}: مستويات الصعوبة", listOf(0, 1, 2, 3), p.groups.map { it.level }.sorted())
        }
    }

    @Test
    fun groupTitlesAreUniqueWithinPuzzle() {
        Puzzles.all.forEachIndexed { i, p ->
            assertEquals("اللغز ${i + 1}: أسماء المجموعات", 4, p.groups.map { it.title }.toSet().size)
        }
    }

    // الحفظ يفصل الكلمات بـ , و | فلا يجوز أن تحتويهما كلمة
    @Test
    fun wordsDoNotContainSaveSeparators() {
        Puzzles.all.forEach { p ->
            p.allWords.forEach { w ->
                assertFalse("الكلمة «$w» تحتوي فاصل حفظ", w.contains(",") || w.contains("|"))
                assertTrue("كلمة فارغة", w.isNotBlank())
            }
        }
    }
}
