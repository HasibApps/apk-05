package com.hasib.guide

import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.concurrent.Executors

class PdfActivity : AppCompatActivity() {
    private var renderer: PdfRenderer? = null
    private var pfd: ParcelFileDescriptor? = null
    private val ex = Executors.newSingleThreadExecutor()
    private var zoom = 1f
    private lateinit var scale: ScaleGestureDetector
    private lateinit var rv: RecyclerView
    private lateinit var adapter: PageAdapter
    private var sw = 1080

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); setContentView(R.layout.activity_pdf)
        findViewById<TextView>(R.id.pTitle).text = intent.getStringExtra("title")
        sw = resources.displayMetrics.widthPixels
        pfd = ParcelFileDescriptor.open(File(intent.getStringExtra("path")!!), ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(pfd!!); val n = renderer!!.pageCount
        rv = findViewById(R.id.pages); val lm = LinearLayoutManager(this)
        rv.layoutManager = lm; adapter = PageAdapter(n); rv.adapter = adapter
        val et = findViewById<EditText>(R.id.pageNo); et.hint = "১-$n"
        findViewById<View>(R.id.go).setOnClickListener {
            val p = et.text.toString().toIntOrNull()
            if (p != null && p in 1..n) {
                lm.scrollToPositionWithOffset(p - 1, 0)
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(et.windowToken, 0)
            } else Toast.makeText(this, "১ থেকে $n এর মধ্যে লিখুন", Toast.LENGTH_SHORT).show()
        }
        // দুই আঙুলে পিঞ্চ-জুম
        scale = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                zoom = (zoom * d.scaleFactor).coerceIn(1f, 4f)
                rv.layoutParams.width = (sw * zoom).toInt(); rv.requestLayout(); return true }
            override fun onScaleEnd(d: ScaleGestureDetector) { adapter.rw = minOf((sw * zoom).toInt(), 2400); adapter.notifyDataSetChanged() }
        })
    }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean { scale.onTouchEvent(e); return super.dispatchTouchEvent(e) }

    inner class PageAdapter(val n: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var rw = 1200
        override fun getItemCount() = n
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = object : RecyclerView.ViewHolder(ImageView(p.context).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -2); adjustViewBounds = true; setBackgroundColor(Color.WHITE) }) {}
        override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
            val iv = h.itemView as ImageView; iv.setImageDrawable(null); iv.tag = i; val w = rw
            iv.minimumHeight = (sw * zoom * 1.41f).toInt()
            ex.execute {
                val bm = renderPage(i, w) ?: return@execute
                runOnUiThread { if (iv.tag == i) iv.setImageBitmap(bm) }
            }
        }
    }

    private fun renderPage(i: Int, w: Int): Bitmap? = synchronized(this) {
        val r = renderer ?: return null
        try {
            r.openPage(i).use { p ->
                val b = Bitmap.createBitmap(w, (w.toFloat() * p.height / p.width).toInt(), Bitmap.Config.ARGB_8888)
                b.eraseColor(Color.WHITE)
                p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                b
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() { synchronized(this) { renderer?.close(); renderer = null }; pfd?.close(); ex.shutdown(); super.onDestroy() }
}
