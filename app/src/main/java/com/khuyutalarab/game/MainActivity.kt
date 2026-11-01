package com.khuyutalarab.game

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.content.res.Configuration
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val maxMistakes = 4
    private val levelColors = intArrayOf(
        Color.parseColor("#F2C14E"), // أصفر
        Color.parseColor("#7FB77E"), // أخضر
        Color.parseColor("#6FA8DC"), // أزرق
        Color.parseColor("#9B7FC4"), // بنفسجي
    )

    // ألوان الواجهة تتبع وضع النظام (فاتح/داكن)
    private val night: Boolean by lazy {
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
    private val bgColor get() = Color.parseColor(if (night) "#14213A" else "#F5F1E8")
    private val headingColor get() = Color.parseColor(if (night) "#F2C14E" else "#1B3A5C")
    private val subColor get() = Color.parseColor(if (night) "#C9D1E0" else "#444444")
    private val tileColor get() = Color.parseColor(if (night) "#2B3A5A" else "#E3DCCB")
    private val tileTextColor get() = Color.parseColor(if (night) "#F3F4F8" else "#1A1A1A")
    private val tileSelColor get() = Color.parseColor(if (night) "#F2C14E" else "#1B3A5C")
    private val tileSelTextColor get() = Color.parseColor(if (night) "#14213A" else "#FFFFFF")

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
    private lateinit var shareBtn: Button
    private lateinit var hintBtn: Button
    private lateinit var ads: RewardedHint
    private val maxHints = 2
    private var hintsUsed = 0
    private var practice = false // لغز أُعيد: للتدريب ولا يُحتسب في الإحصاءات
    private lateinit var restartBtn: Button
    private lateinit var giveUpBtn: Button
    private lateinit var helpBtn: Button
    private lateinit var statsBtn: Button
    private lateinit var soundBtn: Button
    private val sfx = SoundFx()

    override fun onDestroy() {
        sfx.release()
        super.onDestroy()
    }

    private fun soundLabel() = if (stats.soundOn) "🔊 الصوت" else "🔇 الصوت"

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stats = Stats(this)
        sfx.enabled = stats.soundOn
        buildUi()
        ads = RewardedHint(this)
        ads.init()

        // لعبة اليوم: تتغير مرة كل يوم، وتبقى نفسها عند إعادة فتح التطبيق في اليوم ذاته
        val today = System.currentTimeMillis() / 86_400_000L
        if (stats.lastDay != today) {
            stats.lastDay = today
            stats.puzzleIndex = (today % Puzzles.all.size).toInt()
            stats.saved = null // يوم جديد: لا نكمل لغز الأمس
        }
        // أكمل من حيث توقفت إن وُجد تقدم محفوظ سليم، وإلا ابدأ اللغز من جديد
        if (!restoreProgress()) startPuzzle(stats.puzzleIndex)

        // أول تشغيل: اعرض شرح اللعبة مرة واحدة
        if (!stats.seenHelp) {
            stats.seenHelp = true
            showHelp()
        }
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
            practice = stats.practice
            won = state == 1
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
            setBackgroundColor(bgColor)
        }

        root.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 26f
            setTextColor(headingColor)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "اجمع الكلمات الستة عشر في أربع مجموعات، في كل مجموعة أربع كلمات يجمعها رابط."
            textSize = 14f
            setTextColor(subColor)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(12))
        })

        solvedBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        boardBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(solvedBox)
        root.addView(boardBox)

        messageText = TextView(this).apply {
            textSize = 16f
            setTextColor(headingColor)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(4))
        }
        infoText = TextView(this).apply {
            textSize = 15f
            setTextColor(subColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(messageText)
        root.addView(infoText)

        hintBtn = Button(this).apply {
            text = "تلميح (شاهد إعلانًا قصيرًا)"
            setOnClickListener { requestHint() }
        }
        root.addView(hintBtn)

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

        // صف أدوات اللغز: إعادة / لغز آخر
        val tools1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        restartBtn = controlButton("إعادة اللغز") {
            if (hasProgress()) confirm("إعادة اللغز من البداية؟ (الإعادة للتدريب ولا تُحتسب في إحصاءاتك)") { startPuzzle(puzzleIndex, true) }
            else startPuzzle(puzzleIndex, true)
        }
        nextBtn = controlButton("لغز آخر") {
            val goNext = {
                stats.puzzleIndex = (stats.puzzleIndex + 1) % Puzzles.all.size
                startPuzzle(stats.puzzleIndex)
            }
            if (hasProgress()) confirm("ترك هذا اللغز والانتقال إلى لغز آخر؟", goNext) else goNext()
        }
        tools1.addView(restartBtn)
        tools1.addView(nextBtn)
        root.addView(tools1)

        // صف ثانٍ: استسلام / كيف تلعب
        val tools2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        giveUpBtn = controlButton("استسلام") {
            if (!finished) confirm("الاستسلام وكشف الحل؟ سيُحتسب هذا اللغز خسارة.") {
                finish(false)
                render()
            }
        }
        helpBtn = controlButton("كيف تلعب؟") { showHelp() }
        statsBtn = controlButton("إحصاءات") { showStats() }
        tools2.addView(giveUpBtn)
        tools2.addView(helpBtn)
        tools2.addView(statsBtn)
        soundBtn = controlButton(soundLabel()) {
            stats.soundOn = !stats.soundOn
            sfx.enabled = stats.soundOn
            soundBtn.text = soundLabel()
            sfx.correct()
        }
        tools2.addView(soundBtn)
        root.addView(tools2)

        shareBtn = Button(this).apply {
            text = "مشاركة النتيجة"
            visibility = View.GONE
            setOnClickListener { shareResult() }
        }
        root.addView(shareBtn)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(bgColor)
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

    private fun startPuzzle(index: Int, isPractice: Boolean = false) {
        hintsUsed = 0
        practice = isPractice
        stats.practice = isPractice
        puzzleIndex = index
        puzzle = Puzzles.all[index]
        solved.clear()
        selected.clear()
        mistakes = 0
        finished = false
        won = false
        remaining = puzzle.allWords.shuffled().toMutableList()
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
            window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            sfx.correct()
            if (solved.size == puzzle.groups.size) finish(true)
        } else {
            mistakes++
            val oneAway = puzzle.groups.any { g ->
                g !in solved && g.words.count { it in chosen } == 3
            }
            messageText.text = if (oneAway) "قريب! كلمة واحدة فقط خارج المجموعة" else "ليست مجموعة صحيحة"
            selected.clear()
            window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            sfx.wrong()
            if (mistakes >= maxMistakes) finish(false)
        }
        render()
    }

    private fun finish(win: Boolean) {
        finished = true
        won = win
        if (!practice) stats.record(win)
        if (win) sfx.win() else sfx.lose()
        if (!win) {
            // اكشف المجموعات المتبقية
            puzzle.groups.filter { it !in solved }.forEach { solved.add(it) }
            remaining.clear()
            messageText.text = "انتهت المحاولات. هذه هي المجموعات الصحيحة."
        } else {
            messageText.text = "مبروك! أكملت اللعبة 🎉"
        }
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
                    setTextColor(if (isSel) tileSelTextColor else tileTextColor)
                    background = rounded(if (isSel) tileSelColor else tileColor)
                    isEnabled = !finished
                    layoutParams = LinearLayout.LayoutParams(0, dp(64), 1f)
                        .apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
                    setOnClickListener { toggle(word) }
                })
            }
            boardBox.addView(row)
        }

        val left = (maxMistakes - mistakes).coerceAtLeast(0)
        infoText.text = "اللغز ${puzzleIndex + 1} من ${Puzzles.all.size}   |   المحاولات المتبقية: $left" + if (practice) "   |   وضع التدريب" else ""

        submitBtn.isEnabled = !finished && selected.size == 4
        shuffleBtn.isEnabled = !finished
        clearBtn.isEnabled = !finished && selected.isNotEmpty()

        shareBtn.visibility = if (finished) View.VISIBLE else View.GONE
        hintBtn.isEnabled = !finished
        giveUpBtn.isEnabled = !finished

        saveProgress()
    }

    /** تلميح مقابل إعلان: يكشف اسم أسهل مجموعة لم تُحل بعد. حد أقصى لكل لغز. */
    private fun requestHint() {
        if (finished) return
        if (hintsUsed >= maxHints) {
            messageText.text = "استخدمت كل التلميحات المتاحة لهذا اللغز"
            return
        }
        ads.show(
            onReward = {
                val target = puzzle.groups.filter { it !in solved }.minByOrNull { it.level }
                if (target != null && !finished) {
                    hintsUsed++
                    messageText.text = "تلميح: إحدى المجموعات هي «${target.title}»"
                    render()
                }
            },
            onUnavailable = {
                messageText.text = "الإعلان غير متاح الآن، حاول بعد قليل"
            }
        )
    }

    private fun showStats() {
        val played = stats.played
        val rate = if (played == 0) 0 else stats.won * 100 / played
        AlertDialog.Builder(this)
            .setTitle("إحصاءاتك")
            .setMessage(
                "ألعاب مكتملة: $played\n" +
                "الفوز: ${stats.won} ($rate%)\n" +
                "التتابع الحالي: ${stats.streak}\n" +
                "أفضل تتابع: ${stats.best}\n\n" +
                "(إعادة اللغز للتدريب لا تُحتسب هنا)"
            )
            .setPositiveButton("حسنًا", null)
            .show()
    }

    private fun hasProgress() = !finished && (solved.isNotEmpty() || mistakes > 0)

    private fun confirm(message: String, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("نعم") { _, _ -> onYes() }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showHelp() {
        AlertDialog.Builder(this)
            .setTitle("كيف تلعب؟")
            .setMessage(
                "• أمامك 16 كلمة، وهي أربع مجموعات في كل مجموعة أربع كلمات يجمعها رابط.\n" +
                "• اضغط أربع كلمات تظن أنها مجموعة واحدة ثم اضغط «تأكيد».\n" +
                "• إن صدقت تظهر المجموعة باسم رابطها ولونها: الأصفر الأسهل والبنفسجي الأصعب.\n" +
                "• إن أخطأت تخسر محاولة من أربع. وتظهر «قريب» إن كانت ثلاث كلمات صحيحة.\n" +
                "• «تلميح» يكشف اسم إحدى المجموعات مقابل مشاهدة إعلان قصير.\n" +
                "• «إعادة اللغز» للتدريب ولا تُحتسب في الإحصاءات، و«لغز آخر» ينقلك إلى لغز جديد."
            )
            .setPositiveButton("فهمت", null)
            .show()
    }

    /** يشارك النتيجة كنص: مربعات ملونة بترتيب حل المجموعات، دون كشف الكلمات. */
    private fun shareResult() {
        val squares = arrayOf("🟨", "🟩", "🟦", "🟪")
        val text = if (won) {
            val grid = solved.joinToString("\n") { g ->
                squares[g.level.coerceIn(0, 3)].repeat(4)
            }
            "${getString(R.string.app_name)} 🧩\nحللتُ اللغز بـ $mistakes من $maxMistakes أخطاء\n$grid"
        } else {
            "${getString(R.string.app_name)} 🧩\nلم أنجح في لغز اليوم هذه المرة"
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, "مشاركة النتيجة"))
    }

    private fun toggle(word: String) {
        if (finished) return
        if (word in selected) selected.remove(word)
        else if (selected.size < 4) selected.add(word)
        sfx.tick()
        render()
    }

    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(10).toFloat()
    }
}
