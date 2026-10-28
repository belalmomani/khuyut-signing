package com.khuyutalarab.game

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val maxMistakes = 4
    private val levelColors = intArrayOf(
        Color.parseColor("#F2C14E"), // أصفر
        Color.parseColor("#7FB77E"), // أخضر
        Color.parseColor("#6FA8DC"), // أزرق
        Color.parseColor("#9B7FC4"), // بنفسجي
    )

    private lateinit var stats: Stats
    private lateinit var puzzle: Puzzle
    private var puzzleIndex = 0

    private val solved = mutableListOf<Group>()
    private var remaining = mutableListOf<String>()
    private val selected = linkedSetOf<String>()
    private var mistakes = 0
    private var finished = false
    private var won = false

    private lateinit var infoText: TextView
    private lateinit var messageText: TextView
    private lateinit var solvedBox: LinearLayout
    private lateinit var boardBox: LinearLayout
    private lateinit var submitBtn: Button
    private lateinit var shuffleBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var nextBtn: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stats = Stats(this)
        buildUi()

        // لعبة اليوم: تتغير مرة كل يوم، وتبقى نفسها عند إعادة فتح التطبيق في اليوم ذاته
        val today = System.currentTimeMillis() / 86_400_000L
        if (stats.lastDay != today) {
            stats.lastDay = today
            stats.puzzleIndex = (today % Puzzles.all.size).toInt()
            stats.saved = null // يوم جديد: لا نكمل لغز الأمس
        }
        // أكمل من حيث توقفت إن وُجد تقدم محفوظ سليم، وإلا ابدأ اللغز من جديد
        if (!restoreProgress()) startPuzzle(stats.puzzleIndex)
    }

    /** يحفظ حالة اللغز الحالي: الفهرس|الأخطاء|الحالة|المجموعات المحلولة|الكلمات المتبقية */
    private fun saveProgress() {
        val solvedIdx = solved.map { puzzle.groups.indexOf(it) }.joinToString(",")
        val state = if (!finished) 0 else if (won) 1 else 2
        stats.saved = listOf(
            puzzleIndex.toString(), mistakes.toString(), state.toString(),
            solvedIdx, remaining.joinToString(",")
        ).joinToString("|")
    }

    /** يستعيد التقدم المحفوظ إن كان سليمًا ويخص لغز اليوم الحالي، وإلا يعيد false. */
    private fun restoreProgress(): Boolean {
        val raw = stats.saved ?: return false
        return try {
            val parts = raw.split("|", limit = 5)
            if (parts.size < 5) return false
            val idx = parts[0].toInt()
            if (idx != stats.puzzleIndex || idx !in Puzzles.all.indices) return false
            val p = Puzzles.all[idx]
            val mist = parts[1].toInt()
            val state = parts[2].toInt()
            val solvedIdx = if (parts[3].isEmpty()) emptyList() else parts[3].split(",").map { it.toInt() }
            val rem = if (parts[4].isEmpty()) emptyList() else parts[4].split(",")

            if (mist !in 0..maxMistakes || state !in 0..2) return false
            if (solvedIdx.any { it !in p.groups.indices } || solvedIdx.toSet().size != solvedIdx.size) return false

            val solvedGroups = solvedIdx.map { p.groups[it] }
            val expected = p.allWords - solvedGroups.flatMap { it.words }.toSet()
            if (state == 0) {
                if (rem.size != expected.size || rem.toSet() != expected.toSet()) return false
            } else {
                if (solvedGroups.size != p.groups.size || rem.isNotEmpty()) return false
            }

            puzzleIndex = idx
            puzzle = p
            solved.clear()
            solved.addAll(solvedGroups)
            remaining = rem.toMutableList()
            selected.clear()
            mistakes = mist
            finished = state != 0
            won = state == 1
            nextBtn.visibility = if (finished) View.VISIBLE else View.GONE
            messageText.text = when (state) {
                1 -> "مبروك! أكملت اللعبة 🎉"
                2 -> "انتهت المحاولات. هذه هي المجموعات الصحيحة."
                else -> "تابع من حيث توقفت"
            }
            render()
            true
        } catch (e: Exception) {
            false // أي بيانات تالفة: نبدأ لغزًا جديدًا بدل أن ينهار التطبيق
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.parseColor("#F5F1E8"))
        }

        root.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 26f
            setTextColor(Color.parseColor("#1B3A5C"))
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "اجمع الكلمات الستة عشر في أربع مجموعات، في كل مجموعة أربع كلمات يجمعها رابط."
            textSize = 14f
            setTextColor(Color.parseColor("#444444"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(12))
        })

        solvedBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        boardBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(solvedBox)
        root.addView(boardBox)

        messageText = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#1B3A5C"))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(4))
        }
        infoText = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#444444"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(messageText)
        root.addView(infoText)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        shuffleBtn = controlButton("خلط") { remaining.shuffle(); render() }
        clearBtn = controlButton("إلغاء التحديد") { selected.clear(); render() }
        submitBtn = controlButton("تأكيد") { submit() }
        controls.addView(shuffleBtn)
        controls.addView(clearBtn)
        controls.addView(submitBtn)
        root.addView(controls)

        nextBtn = Button(this).apply {
            text = "لعبة جديدة"
            visibility = View.GONE
            setOnClickListener {
                stats.puzzleIndex = (stats.puzzleIndex + 1) % Puzzles.all.size
                startPuzzle(stats.puzzleIndex)
            }
        }
        root.addView(nextBtn)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F5F1E8"))
            addView(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
    }

    private fun controlButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(dp(3), 0, dp(3), 0) }
            setOnClickListener { action() }
        }

    private fun startPuzzle(index: Int) {
        puzzleIndex = index
        puzzle = Puzzles.all[index]
        solved.clear()
        selected.clear()
        mistakes = 0
        finished = false
        won = false
        remaining = puzzle.allWords.shuffled().toMutableList()
        nextBtn.visibility = View.GONE
        messageText.text = ""
        render()
    }

    private fun submit() {
        if (finished || selected.size != 4) return
        val chosen = selected.toSet()
        val exact = puzzle.groups.firstOrNull { g -> g !in solved && g.words.toSet() == chosen }

        if (exact != null) {
            solved.add(exact)
            remaining.removeAll(chosen)
            selected.clear()
            messageText.text = "أحسنت! ${exact.title}"
            if (solved.size == puzzle.groups.size) finish(true)
        } else {
            mistakes++
            val oneAway = puzzle.groups.any { g ->
                g !in solved && g.words.count { it in chosen } == 3
            }
            messageText.text = if (oneAway) "قريب! كلمة واحدة فقط خارج المجموعة" else "ليست مجموعة صحيحة"
            selected.clear()
            if (mistakes >= maxMistakes) finish(false)
        }
        render()
    }

    private fun finish(win: Boolean) {
        finished = true
        won = win
        stats.record(win)
        if (!win) {
            // اكشف المجموعات المتبقية
            puzzle.groups.filter { it !in solved }.forEach { solved.add(it) }
            remaining.clear()
            messageText.text = "انتهت المحاولات. هذه هي المجموعات الصحيحة."
        } else {
            messageText.text = "مبروك! أكملت اللعبة 🎉"
        }
        nextBtn.visibility = View.VISIBLE
    }

    private fun render() {
        // المجموعات المحلولة
        solvedBox.removeAllViews()
        solved.forEach { g ->
            solvedBox.addView(TextView(this).apply {
                text = g.title + "\n" + g.words.joinToString("، ")
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#1A1A1A"))
                background = rounded(levelColors[g.level.coerceIn(0, 3)])
                setPadding(dp(8), dp(10), dp(8), dp(10))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(6)) }
            })
        }

        // لوحة الكلمات: صفوف من 4
        boardBox.removeAllViews()
        remaining.chunked(4).forEach { rowWords ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }
            rowWords.forEach { word ->
                val isSel = word in selected
                row.addView(Button(this).apply {
                    text = word
                    textSize = 15f
                    isAllCaps = false
                    setTextColor(if (isSel) Color.WHITE else Color.parseColor("#1A1A1A"))
                    background = rounded(if (isSel) Color.parseColor("#1B3A5C") else Color.parseColor("#E3DCCB"))
                    isEnabled = !finished
                    layoutParams = LinearLayout.LayoutParams(0, dp(64), 1f)
                        .apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
                    setOnClickListener { toggle(word) }
                })
            }
            boardBox.addView(row)
        }

        val left = (maxMistakes - mistakes).coerceAtLeast(0)
        infoText.text = "المحاولات المتبقية: $left   |   " +
            "فزت ${stats.won} من ${stats.played}   |   التتابع ${stats.streak} (الأفضل ${stats.best})"

        submitBtn.isEnabled = !finished && selected.size == 4
        shuffleBtn.isEnabled = !finished
        clearBtn.isEnabled = !finished && selected.isNotEmpty()

        saveProgress()
    }

    private fun toggle(word: String) {
        if (finished) return
        if (word in selected) selected.remove(word)
        else if (selected.size < 4) selected.add(word)
        render()
    }

    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(10).toFloat()
    }
}
