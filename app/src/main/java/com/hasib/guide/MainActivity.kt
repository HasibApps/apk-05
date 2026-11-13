package com.hasib.guide

import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.RelativeSizeSpan
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val ACC = 0xFF00E0A4.toInt(); private val CARD = 0xFF141C2F.toInt(); private val LINE = 0xFF24304A.toInt()
    private val colors = listOf(0xFFFF6B6B, 0xFF0ABDE3, 0xFF10AC84, 0xFFF368E0, 0xFF5F27CD, 0xFF01A3A4, 0xFF2E86DE).map { it.toInt() }
    private val icons = listOf("অ", "En", "💻", "⚛", "🧪", "🧬", "∑")
    private lateinit var content: LinearLayout
    private lateinit var title: TextView
    private var subject: String? = null
    private var paper = "first"
    private var admin = false
    private var q = ""
    private val expanded = HashSet<String>()
    private val downloading = HashSet<String>()
    private val actViews = HashMap<String, TextView>()

    private val exporter = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        u?.let { contentResolver.openOutputStream(it)?.use { o -> o.write(Store.root.toString(2).toByteArray()) }; toast("JSON ডাউনলোড হয়েছে") } }
    private val importer = registerForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        u?.let { try { Store.import(contentResolver.openInputStream(it)!!.bufferedReader().readText()); render(true); toast("ইমপোর্ট হয়েছে") } catch (e: Exception) { toast("ভুল JSON ফাইল") } } }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); setContentView(R.layout.activity_main)
        Store.init(filesDir)
        content = findViewById(R.id.content); title = findViewById(R.id.title)
        findViewById<View>(R.id.btnSettings).setOnClickListener { settings(it) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (subject != null) { subject = null; q = ""; render(true) } else finish() } })
        render(true)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun shape(c: Int, r: Int = 16, stroke: Int = 0) = GradientDrawable().apply {
        setColor(c); cornerRadius = dp(r).toFloat(); if (stroke != 0) setStroke(dp(1), stroke) }
    private fun grad(c: Int, o: GradientDrawable.Orientation = GradientDrawable.Orientation.TL_BR) =
        GradientDrawable(o, intArrayOf(c, 0xFF0B1220.toInt())).apply { cornerRadius = dp(20).toFloat() }
    private fun View.press() {
        setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(.96f).scaleY(.96f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }; false }
    }

    private fun render(a: Boolean = false) {
        content.removeAllViews(); actViews.clear()
        val s = subject
        title.text = (s ?: "HSC গাইড ২০২৭") + if (admin) " • ADMIN" else ""
        if (s == null) home() else subjectView(s)
        if (a) animateIn()
    }

    private fun animateIn() {
        val vs = ArrayList<View>()
        for (i in 0 until content.childCount) { val c = content.getChildAt(i)
            if (c is GridLayout) for (j in 0 until c.childCount) vs.add(c.getChildAt(j)) else vs.add(c) }
        vs.forEachIndexed { i, v -> v.alpha = 0f; v.translationY = dp(24).toFloat()
            v.animate().alpha(1f).translationY(0f).setStartDelay(i * 45L).setDuration(350).setInterpolator(DecelerateInterpolator()).start() }
    }

    // pdf সংখ্যা (onlyLocal = true হলে শুধু ডাউনলোড করা)
    private fun count(arr: JSONArray, onlyLocal: Boolean): Int {
        var n = 0
        for (i in 0 until arr.length()) { val o = arr.getJSONObject(i)
            n += if (o.optBoolean("f")) count(o.optJSONArray("c") ?: JSONArray(), onlyLocal)
            else if (!onlyLocal || (o.optString("l").isNotBlank() && local(o.getString("l")).exists())) 1 else 0 }
        return n
    }
    private fun subjCount(s: String): Int { var n = 0
        for (p in listOf("first", "second")) for (c in listOf("main", "test")) n += count(Store.list(s, p, c), true); return n }

    private fun home() {
        content.addView(TextView(this).apply {
            text = "স্বাগতম 👋\nঅফলাইনে সংরক্ষিত: ${Store.subjects.sumOf { subjCount(it) }} টি PDF"
            setTextColor(Color.WHITE); textSize = 16f; setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFF00B894.toInt(), 0xFF0A6C8F.toInt())).apply { cornerRadius = dp(20).toFloat() }
        }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(6), dp(4), dp(6), dp(8)) })
        val g = GridLayout(this).apply { columnCount = 2 }
        Store.subjects.forEachIndexed { i, s ->
            val line = "${subjCount(s)} ফাইল অফলাইন"
            val sp = SpannableString("${icons[i]}\n$s\n$line"); sp.setSpan(RelativeSizeSpan(0.7f), sp.length - line.length, sp.length, 0)
            val t = TextView(this).apply {
                text = sp; gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 17f; setTypeface(typeface, Typeface.BOLD)
                background = grad(colors[i]); press(); setOnClickListener { subject = s; q = ""; render(true) } }
            val lp = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f))
            lp.width = 0; lp.height = dp(130); lp.setMargins(dp(6), dp(6), dp(6), dp(6)); g.addView(t, lp)
        }
        content.addView(g)
    }

    private fun subjectView(s: String) {
        val idx = Store.subjects.indexOf(s)
        content.addView(TextView(this).apply {
            text = "${icons[idx]}  $s\nঅফলাইনে ${subjCount(s)} টি PDF"; setTextColor(Color.WHITE); textSize = 18f
            setTypeface(typeface, Typeface.BOLD); setPadding(dp(20), dp(18), dp(20), dp(18)); background = grad(colors[idx], GradientDrawable.Orientation.LEFT_RIGHT)
        }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(4), 0, dp(4), dp(10)) })
        val tabs = LinearLayout(this)
        listOf("first" to "1st Paper", "second" to "2nd Paper").forEach { (k, n) ->
            tabs.addView(TextView(this).apply {
                text = n; gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(12)); textSize = 15f
                setTextColor(if (paper == k) 0xFF06121F.toInt() else 0xFFAAB6CC.toInt())
                background = shape(if (paper == k) ACC else CARD, 14); press(); setOnClickListener { paper = k; render(true) }
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        }
        content.addView(tabs)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(EditText(this).apply {
            hint = "🔍 PDF খুঁজুন…"; setText(q); setTextColor(Color.WHITE); setHintTextColor(0xFF6B7A93.toInt()); textSize = 14f; isSingleLine = true
            setPadding(dp(16), dp(12), dp(16), dp(12)); background = shape(CARD, 14, LINE)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(e: Editable?) { q = e.toString(); list.removeAllViews(); fill(list, s) }
                override fun beforeTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
                override fun onTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {} })
        }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(4), dp(12), dp(4), 0) })
        content.addView(list); fill(list, s)
    }

    private fun collect(arr: JSONArray, out: ArrayList<Pair<JSONArray, Int>>) {
        for (i in 0 until arr.length()) { val o = arr.getJSONObject(i)
            if (o.optBoolean("f")) collect(o.optJSONArray("c") ?: JSONArray(), out)
            else if (o.getString("t").contains(q, true)) out.add(arr to i) }
    }

    private fun fill(list: LinearLayout, s: String) {
        if (q.isNotBlank()) {
            val out = ArrayList<Pair<JSONArray, Int>>()
            listOf("main", "test").forEach { collect(Store.list(s, paper, it), out) }
            if (out.isEmpty()) list.addView(TextView(this).apply { text = "কিছু পাওয়া যায়নি"; setTextColor(0xFF6B7A93.toInt()); gravity = Gravity.CENTER; setPadding(0, dp(30), 0, 0) })
            out.forEach { addItem(list, it.first, it.second, 0, s) }
            return
        }
        listOf("main" to "📗 মূল বই", "test" to "📋 Test Paper").forEach { (sec, h) ->
            list.addView(TextView(this).apply { text = h; setTextColor(ACC); textSize = 16f; setTypeface(typeface, Typeface.BOLD); setPadding(dp(4), dp(18), 0, dp(4)) })
            val arr = Store.list(s, paper, sec)
            for (i in 0 until arr.length()) addItem(list, arr, i, 0, s)
            if (admin) list.addView(TextView(this).apply {
                text = "＋ নতুন যোগ করুন"; setTextColor(ACC); setPadding(dp(8), dp(10), 0, dp(10)); setOnClickListener { addDialog(arr) } })
        }
    }

    private fun addItem(parent: LinearLayout, arr: JSONArray, i: Int, indent: Int, s: String) {
        val o = arr.getJSONObject(i); val folder = o.optBoolean("f"); val t = o.getString("t"); val link = o.optString("l")
        val v = layoutInflater.inflate(R.layout.item_row, parent, false)
        v.background = shape(CARD, 16, LINE); v.press()
        (v.layoutParams as LinearLayout.LayoutParams).setMargins(dp(indent * 16), dp(5), 0, dp(5))
        val name = v.findViewById<TextView>(R.id.name); val sub = v.findViewById<TextView>(R.id.sub)
        val act = v.findViewById<TextView>(R.id.act); val adm = v.findViewById<LinearLayout>(R.id.adm); val thumb = v.findViewById<ImageView>(R.id.thumb)
        val key = "$s$paper$t$indent"
        if (folder) {
            val c = o.optJSONArray("c") ?: JSONArray()
            name.text = "📂 $t"; sub.text = "${count(c, true)}/${count(c, false)} অফলাইন"; act.text = if (key in expanded) "⌃" else "⌄"
            v.setOnClickListener { if (!expanded.remove(key)) expanded.add(key); render() }
        } else {
            name.text = t; val f = local(link)
            if (f.exists()) {
                act.text = "✓"; sub.text = "📥 অফলাইন • ${"%.1f".format(f.length() / 1048576f)} MB"; thumb(f, thumb)
                v.setOnLongClickListener {
                    AlertDialog.Builder(this).setMessage("অফলাইন কপি মুছবেন?").setPositiveButton("হ্যাঁ") { _, _ ->
                        f.delete(); File(File(filesDir, "thumbs"), f.name + ".png").delete(); render() }.setNegativeButton("না", null).show(); true }
            } else {
                act.text = if (link in downloading) "…" else "⬇"; sub.text = "☁ অনলাইন • ডাউনলোড করুন"
                act.setOnClickListener { download(t, link) }; actViews[link] = act
            }
            v.setOnClickListener { openPdf(t, link) }
        }
        if (admin) {
            fun btn(x: String, c: () -> Unit) = adm.addView(TextView(this).apply { text = x; textSize = 18f; setTextColor(Color.WHITE); setPadding(dp(8), 0, dp(8), 0); setOnClickListener { c() } })
            btn("✎") { form("এডিট", t, if (folder) null else link) { n, l -> o.put("t", n); if (!folder) o.put("l", l); Store.save(); render() } }
            btn("✕") { AlertDialog.Builder(this).setMessage("মুছে ফেলবেন?").setPositiveButton("হ্যাঁ") { _, _ -> arr.remove(i); Store.save(); render() }.setNegativeButton("না", null).show() }
            if (folder) btn("＋") { addDialog(o.optJSONArray("c") ?: JSONArray().also { o.put("c", it) }) }
        }
        parent.addView(v)
        if (folder && key in expanded) { val c = o.optJSONArray("c") ?: JSONArray(); for (j in 0 until c.length()) addItem(parent, c, j, indent + 1, s) }
    }

    private fun form(ttl: String, name: String, link: String?, ok: (String, String) -> Unit) {
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val n = EditText(this).apply { hint = "নাম"; setText(name) }; l.addView(n)
        val k = EditText(this).apply { hint = "Google Drive / PDF লিংক"; setText(link ?: "") }
        if (link != null) l.addView(k)
        AlertDialog.Builder(this).setTitle(ttl).setView(l)
            .setPositiveButton("সেভ") { _, _ -> if (n.text.isNotBlank()) ok(n.text.toString().trim(), k.text.toString().trim()) }
            .setNegativeButton("বাতিল", null).show()
    }

    private fun addDialog(arr: JSONArray) {
        AlertDialog.Builder(this).setItems(arrayOf("📂 নতুন ফোল্ডার", "📕 নতুন PDF")) { _, w ->
            form("নতুন", "", if (w == 0) null else "") { n, l ->
                arr.put(JSONObject().put("t", n).put("f", w == 0).put("l", l).put("c", JSONArray())); Store.save(); render() }
        }.show()
    }

    private fun settings(v: View) = PopupMenu(this, v).apply {
        menu.add(0, 1, 0, if (admin) "🔒 Admin বন্ধ করুন" else "👤 Admin")
        menu.add(0, 2, 0, "⬇ ডাটা .json ডাউনলোড")
        menu.add(0, 3, 0, "⬆ ডাটা .json ইমপোর্ট")
        setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> if (admin) { admin = false; render() } else login()
                2 -> exporter.launch("hsc_guide_data.json")
                3 -> importer.launch(arrayOf("*/*"))
            }; true }
        show()
    }

    private fun login() {
        val e = EditText(this).apply { hint = "Password"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        AlertDialog.Builder(this).setTitle("Admin Login").setView(e).setPositiveButton("ঢুকুন") { _, _ ->
            if (Store.sha(e.text.toString()) == Store.ADMIN_HASH) { admin = true; render() } else toast("ভুল পাসওয়ার্ড") }.show()
    }

    // ---- ডাউনলোড ও অফলাইন ফাইল ----
    private fun local(link: String) = File(File(filesDir, "pdfs").apply { mkdirs() }, Store.sha(link).take(16) + ".pdf")

    private fun fetch(url: String, out: File, onProgress: (Int) -> Unit) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.instanceFollowRedirects = true; c.connectTimeout = 20000; c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
        if (c.responseCode !in 200..299) throw Exception("HTTP ${c.responseCode}")
        val total = c.contentLengthLong
        c.inputStream.use { i -> out.outputStream().use { o ->
            val buf = ByteArray(32768); var done = 0L; var last = -1; var n: Int
            while (i.read(buf).also { n = it } >= 0) {
                o.write(buf, 0, n); done += n
                if (total > 0) { val p = (done * 100 / total).toInt(); if (p != last) { last = p; onProgress(p) } }
            } } }
    }

    private fun download(title: String, link: String, then: (File) -> Unit = {}) {
        if (link.isBlank()) { toast("লিংক সেট করা নেই — Admin থেকে লিংক দিন"); return }
        if (!downloading.add(link)) { toast("ডাউনলোড চলছে…"); return }
        toast("ডাউনলোড শুরু: $title"); actViews[link]?.text = "0%"
        thread {
            var err = ""; var ok = false
            val f = local(link); val tmp = File(f.path + ".tmp")
            val id = Regex("(?:/d/|[?&]id=)([\\w-]+)").find(link)?.groupValues?.get(1)
            val urls = if (id != null) listOf(
                "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t",
                "https://drive.google.com/uc?export=download&confirm=t&id=$id") else listOf(link)
            for (u in urls) {
                try {
                    fetch(u, tmp) { p -> runOnUiThread { actViews[link]?.text = "$p%" } }
                    val head = ByteArray(4); tmp.inputStream().use { it.read(head) }
                    if (String(head) == "%PDF") { ok = true; break } else err = "PDF পাওয়া যায়নি (Drive শেয়ার 'Anyone with the link' করুন)"
                } catch (e: Exception) { err = e.message ?: e.javaClass.simpleName }
            }
            runOnUiThread {
                downloading.remove(link)
                if (ok) { tmp.renameTo(f); render(); then(f) } else { tmp.delete(); actViews[link]?.text = "⬇"; toast("ডাউনলোড ব্যর্থ: $err") }
            }
        }
    }

    private fun openPdf(title: String, link: String) {
        val go = { f: File -> startActivity(Intent(this, PdfActivity::class.java).putExtra("path", f.path).putExtra("title", title)) }
        val f = local(link); if (f.exists()) go(f) else download(title, link, go)
    }

    private fun thumb(f: File, iv: ImageView) {
        val t = File(File(filesDir, "thumbs").apply { mkdirs() }, f.name + ".png")
        thread {
            try {
                if (!t.exists()) ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { r -> r.openPage(0).use { p ->
                        val b = Bitmap.createBitmap(160, (160f * p.height / p.width).toInt(), Bitmap.Config.ARGB_8888)
                        b.eraseColor(Color.WHITE); p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        t.outputStream().use { o -> b.compress(Bitmap.CompressFormat.PNG, 90, o) } } } }
                val bm = BitmapFactory.decodeFile(t.path)
                runOnUiThread { iv.setImageBitmap(bm); iv.visibility = View.VISIBLE }
            } catch (e: Exception) { }
        }
    }
}
