package com.khuyutalarab.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BankTest {
    @Test
    fun everyCategoryHasAtLeastFourUniqueWords() {
        Bank.categories.forEach {
            assertTrue(it.title, it.words.size >= 4)
            assertEquals(it.title, it.words.size, it.words.toSet().size)
            assertTrue(it.title, it.level in 0..3)
        }
    }

    @Test
    fun noWordBelongsToTwoCategories() {
        val all = Bank.categories.flatMap { c -> c.words.map { it to c.title } }
        val dup = all.groupBy({ it.first }, { it.second }).filter { it.value.size > 1 }
        assertTrue("كلمات مكررة: $dup", dup.isEmpty())
    }

    @Test
    fun everyLevelHasEnoughFamilies() {
        for (lv in 0..3) {
            assertTrue("المستوى $lv", Bank.categories.filter { it.level == lv }.map { it.family }.toSet().size >= 4)
        }
    }

    @Test
    fun generatorIsDeterministicAndWellFormed() {
        val a = Generator.generate(Puzzles.GENERATED_COUNT)
        val b = Generator.generate(Puzzles.GENERATED_COUNT)
        assertEquals(Puzzles.GENERATED_COUNT, a.size)
        assertEquals(a.map { it.allWords }, b.map { it.allWords })
        a.forEach { p ->
            assertEquals(4, p.groups.size)
            assertEquals(16, p.allWords.toSet().size)
            assertEquals(setOf(0, 1, 2, 3), p.groups.map { it.level }.toSet())
        }
    }
}
