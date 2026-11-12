package com.hasib.guide

import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
import android.text.InputType
import android.view.*
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
    private lateinit var content: LinearLayout
    private lateinit var title: TextView
    private var subject: String? = null
    private var paper = "first"
    private var admin = false
    private val expanded = HashSet<String>()

    private val exporter = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        u?.let { contentResolver.openOutputStream(it)?.use { o -> o.write(Store.root.toString(2).toByteArray()) }; toast("JSON ডাউনলোড হয়েছে") } }
    private val importer = registerForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        u?.let { try { Store.import(contentResolver.openInputStream(it)!!.bufferedReader().readText()); render(); toast("ইমপোর্ট হয়েছে") } catch (e: Exception) { toast("ভুল JSON ফাইল") } } }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); setContentView(R.layout.activity_main)
        Store.init(filesDir)
        content = findViewById(R.id.content); title = findViewById(R.id.title)
        findViewById<View>(R.id.btnSettings).setOnClickListener { settings(it) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (subject != null) { subject = null; render() } else finish() } })
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun shape(c: Int, r: Int = 16, stroke: Int = 0) = GradientDrawable().apply {
        setColor(c); cornerRadius = dp(r).toFloat(); if (stroke != 0) setStroke(dp(1), stroke) }

    private fun render() {
        content.removeAllViews()
        val s = subject
        title.text = (s ?: "HSC গাইড ২০২৭") + if (admin) " • ADMIN" else ""
        if (s == null) home() else subjectView(s)
    }

    private fun home() {
        val grads = listOf(0xFFFF6B6B, 0xFF0ABDE3, 0xFF10AC84, 0xFFF368E0, 0xFF5F27CD, 0xFF01A3A4, 0xFF2E86DE)
        val icons = listOf("অ", "En", "💻", "⚛", "🧪", "🧬", "∑")
        val g = GridLayout(this).apply { columnCount = 2 }
        Store.subjects.forEachIndexed { i, s ->
            val t = TextView(this).apply {
                text = "${icons[i]}\n$s"; gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(grads[i].toInt(), 0xFF0B1220.toInt())).apply { cornerRadius = dp(20).toFloat() }
                setOnClickListener { subject = s; render() }
            }
            val lp = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f))
            lp.width = 0; lp.height = dp(130); lp.setMargins(dp(6), dp(6), dp(6), dp(6)); g.addView(t, lp)
        }
        content.addView(g)
    }

    private fun subjectView(s: String) {
        val tabs = LinearLayout(this)
        listOf("first" to "1st Paper", "second" to "2nd Paper").forEach { (k, n) ->
            tabs.addView(TextView(this).apply {
                text = n; gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(12)); textSize = 15f
                setTextColor(if (paper == k) 0xFF06121F.toInt() else 0xFFAAB6CC.toInt())
                background = shape(if (paper == k) ACC else CARD, 14); setOnClickListener { paper = k; render() }
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        }
        content.addView(tabs)
        listOf("main" to "📗 মূল বই", "test" to "📋 Test Paper").forEach { (sec, h) ->
            content.addView(TextView(this).apply { text = h; setTextColor(ACC); textSize = 16f; setTypeface(typeface, Typeface.BOLD); setPadding(dp(4), dp(18), 0, dp(4)) })
            val arr = Store.list(s, paper, sec)
            for (i in 0 until arr.length()) addItem(arr, i, 0)
            if (admin) content.addView(TextView(this).apply {
                text = "＋ নতুন যোগ করুন"; setTextColor(ACC); setPadding(dp(8), dp(10), 0, dp(10)); setOnClickListener { addDialog(arr) } })
        }
    }

    private fun addItem(arr: JSONArray, i: Int, indent: Int) {
        val o = arr.getJSONObject(i); val folder = o.optBoolean("f"); val t = o.getString("t"); val link = o.optString("l")
        val v = layoutInflater.inflate(R.layout.item_row, content, false)
        v.background = shape(CARD, 14, LINE)
        (v.layoutParams as LinearLayout.LayoutParams).setMargins(dp(indent * 16), dp(5), 0, dp(5))
        val name = v.findViewById<TextView>(R.id.name); val act = v.findViewById<TextView>(R.id.act)
        val adm = v.findViewById<LinearLayout>(R.id.adm); val iv = v.findViewById<ImageView>(R.id.thumb)
        val key = "$subject$paper$t$indent"
        if (folder) {
            name.text = "📂 $t"; act.text = if (key in expanded) "⌃" else "⌄"
            v.setOnClickListener { if (!expanded.remove(key)) expanded.add(key); render() }
        } else {
            name.text = t; val f = local(link)
            if (f.exists()) { act.text = "✓"; thumb(f, iv) } else { act.text = "⬇"; act.setOnClickListener { download(t, link) } }
            v.setOnClickListener { openPdf(t, link) }
        }
        if (admin) {
            fun btn(s: String, c: () -> Unit) = adm.addView(TextView(this).apply { text = s; textSize = 18f; setTextColor(Color.WHITE); setPadding(dp(8), 0, dp(8), 0); setOnClickListener { c() } })
            btn("✎") { form("এডিট", t, if (folder) null else link) { n, l -> o.put("t", n); if (!folder) o.put("l", l); Store.save(); render() } }
            btn("✕") { AlertDialog.Builder(this).setMessage("মুছে ফেলবেন?").setPositiveButton("হ্যাঁ") { _, _ -> arr.remove(i); Store.save(); render() }.setNegativeButton("না", null).show() }
            if (folder) btn("＋") { addDialog(o.optJSONArray("c") ?: JSONArray().also { o.put("c", it) }) }
        }
        content.addView(v)
        if (folder && key in expanded) { val c = o.optJSONArray("c") ?: JSONArray(); for (j in 0 until c.length()) addItem(c, j, indent + 1) }
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
    private fun direct(l: String) = Regex("/d/([\\w-]+)").find(l)?.let { "https://drive.google.com/uc?export=download&confirm=t&id=${it.groupValues[1]}" } ?: l

    private fun download(title: String, link: String, then: (File) -> Unit = {}) {
        if (link.isBlank()) { toast("লিংক সেট করা নেই"); return }
        toast("ডাউনলোড শুরু: $title")
        thread {
            try {
                val f = local(link); val tmp = File(f.path + ".tmp")
                val c = URL(direct(link)).openConnection() as HttpURLConnection
                c.instanceFollowRedirects = true; c.connectTimeout = 15000
                c.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                val head = ByteArray(4); tmp.inputStream().use { it.read(head) }
                if (String(head) != "%PDF") { tmp.delete(); throw Exception("not pdf") }
                tmp.renameTo(f); runOnUiThread { render(); then(f) }
            } catch (e: Exception) { runOnUiThread { toast("ডাউনলোড ব্যর্থ") } }
        }
    }

    private fun openPdf(title: String, link: String) {
        val go = { f: File -> startActivity(Intent(this, PdfActivity::class.java).putExtra("path", f.path).putExtra("title", title)) }
        val f = local(link); if (f.exists()) go(f) else download(title, link, go)
    }

    // PDF-এর প্রথম পৃষ্ঠা থেকে থাম্বনেইল
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
