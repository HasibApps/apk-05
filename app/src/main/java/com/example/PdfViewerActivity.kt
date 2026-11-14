package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data.GuideRepository
import com.example.databinding.ActivityPdfViewerBinding
import com.example.databinding.ItemPdfPageBinding
import com.example.download.DownloadCenter
import com.example.download.DownloadFormat
import com.example.download.DownloadState
import com.example.download.DownloadStatus
import com.example.download.PdfDownloadService
import com.example.pdf.PdfPasswordStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private lateinit var repository: GuideRepository

    private var pdfFileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private val rendererLock = Any()
    private var totalPages = 0
    private var toolbarsVisible = true
    private var remoteDownloadListener: DownloadCenter.Listener? = null
    private lateinit var passwordStore: PdfPasswordStore
    private var decryptedTempFile: File? = null
    private var passwordDialog: androidx.appcompat.app.AlertDialog? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val pageCache = object : LruCache<Int, Bitmap>(16) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && !oldValue.isRecycled) {
                // Allow GC safely
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = GuideRepository.getInstance(this)
        passwordStore = PdfPasswordStore(this)
        applyWindowInsets()

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "PDF Reader" }
        val localPath = intent.getStringExtra(EXTRA_LOCAL_PATH)
        val remoteUrl = intent.getStringExtra(EXTRA_REMOTE_URL)
        val subject = intent.getStringExtra(EXTRA_SUBJECT).orEmpty().ifBlank { "গাইড" }

        binding.tvViewerTitle.text = title
        setupControls()

        when {
            !localPath.isNullOrBlank() && File(localPath).exists() -> {
                openLocalPdf(File(localPath))
            }
            !remoteUrl.isNullOrBlank() -> {
                val cached = repository.findDownloadedByUrl(remoteUrl)
                if (cached != null && File(cached.pdfPath).exists()) {
                    openLocalPdf(File(cached.pdfPath))
                } else {
                    downloadOrPreviewRemote(remoteUrl, title, subject)
                }
            }
            else -> {
                Toast.makeText(this, "PDF ফাইল পাওয়া যায়নি।", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootPdfViewer) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.pdfTopBar.updatePadding(top = sysBars.top + 10)
            val params = binding.pdfBottomBar.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = sysBars.bottom + 10
            binding.pdfBottomBar.layoutParams = params
            insets
        }
    }

    private fun setupControls() {
        binding.btnViewerBack.setOnClickListener { finish() }

        binding.btnTogglePageSearch.setOnClickListener {
            val isVisible = binding.layoutPageSearchBar.visibility == View.VISIBLE
            binding.layoutPageSearchBar.visibility = if (isVisible) View.GONE else View.VISIBLE
            if (!isVisible) {
                binding.etPageNumber.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.etPageNumber, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        binding.btnGoToPage.setOnClickListener { jumpToEnteredPage() }
        binding.etPageNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                jumpToEnteredPage()
                true
            } else false
        }

        binding.recyclerPdfPages.onZoomChanged = { scale ->
            binding.tvZoomLevel.text = String.format(Locale.US, "%.1fx", scale)
        }

        binding.recyclerPdfPages.onSingleTap = {
            toggleBars()
        }

        binding.seekBarPage.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && totalPages > 0) {
                    jumpToPageIndex(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun toggleBars() {
        toolbarsVisible = !toolbarsVisible
        val vis = if (toolbarsVisible) View.VISIBLE else View.GONE
        binding.pdfTopBar.visibility = vis
        binding.pdfBottomBar.visibility = vis
    }

    private fun jumpToEnteredPage() {
        val raw = binding.etPageNumber.text?.toString()?.trim().orEmpty()
        val page = raw.toIntOrNull()
        if (page == null || page < 1 || page > totalPages) {
            Toast.makeText(
                this,
                "১ থেকে $totalPages এর মধ্যে পৃষ্ঠা নম্বর লিখুন",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        jumpToPageIndex(page - 1)
        binding.layoutPageSearchBar.visibility = View.GONE
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etPageNumber.windowToken, 0)
    }

    private fun jumpToPageIndex(index: Int) {
        if (totalPages <= 0) return
        val clamped = index.coerceIn(0, totalPages - 1)
        val lm = binding.recyclerPdfPages.layoutManager as? LinearLayoutManager
        lm?.scrollToPositionWithOffset(clamped, 0)
        updatePageIndicator(clamped)
    }

    private fun updatePageIndicator(pageIndex: Int) {
        val current = (pageIndex + 1).coerceIn(1, totalPages.coerceAtLeast(1))
        binding.tvViewerPageInfo.text = "পৃষ্ঠা $current / $totalPages • ২ আঙুলে জুম"
        binding.tvBottomPageCount.text = "$current / $totalPages"
        if (binding.seekBarPage.progress != pageIndex) {
            binding.seekBarPage.progress = pageIndex
        }
    }

    /**
     * PDF ডাউনলোড Service এর মাধ্যমে হয়, তাই এখান থেকে বের হয়ে গেলেও ডাউনলোড চলতে থাকে।
     * ডাউনলোড শেষ হলে এখানে নিজে থেকেই PDF খুলে যায়।
     */
    private fun downloadOrPreviewRemote(url: String, title: String, subject: String) {
        binding.layoutViewerLoading.visibility = View.VISIBLE
        binding.tvLoadingStatus.text = "অফলাইন রিডারের জন্য PDF ডাউনলোড হচ্ছে…"

        val listener = DownloadCenter.Listener { state: DownloadState ->
            if (state.url == url) {
                when (state.status) {
                    DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> {
                        binding.tvLoadingStatus.text = DownloadFormat.progressLine(state)
                    }
                    DownloadStatus.DONE -> {
                        detachRemoteListener()
                        binding.layoutViewerLoading.visibility = View.GONE
                        val meta = repository.findDownloadedByUrl(url)
                        if (meta != null && File(meta.pdfPath).exists()) {
                            openLocalPdf(File(meta.pdfPath))
                        } else {
                            openWebFallback(url)
                        }
                    }
                    DownloadStatus.FAILED -> {
                        detachRemoteListener()
                        binding.layoutViewerLoading.visibility = View.GONE
                        // Drive প্রিভিউ বা ওয়েব পেজ হলে ওয়েবভিউতে দেখাও
                        openWebFallback(url)
                    }
                    DownloadStatus.CANCELLED -> {
                        detachRemoteListener()
                        finish()
                    }
                }
            }
        }
        remoteDownloadListener = listener
        DownloadCenter.addListener(listener)

        if (!DownloadCenter.isActive(url)) {
            ensureNotificationPermission()
            PdfDownloadService.enqueue(this, url, title, subject)
        } else {
            DownloadCenter.get(url)?.let { binding.tvLoadingStatus.text = DownloadFormat.progressLine(it) }
        }
    }

    private fun detachRemoteListener() {
        remoteDownloadListener?.let { DownloadCenter.removeListener(it) }
        remoteDownloadListener = null
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun openWebFallback(url: String) {
        binding.recyclerPdfPages.visibility = View.GONE
        binding.pdfBottomBar.visibility = View.GONE
        binding.btnTogglePageSearch.visibility = View.GONE
        binding.webViewOnlinePreview.visibility = View.VISIBLE
        binding.tvViewerPageInfo.text = "অনলাইন প্রিভিউ মোড"

        binding.webViewOnlinePreview.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = false
            }
            loadUrl(url)
        }
    }

    private fun openLocalPdf(file: File) {
        binding.layoutViewerLoading.visibility = View.VISIBLE
        binding.tvLoadingStatus.text = "অফলাইন PDF খুলছে…"

        lifecycleScope.launch {
            val opened = withContext(Dispatchers.IO) { runCatching { openRenderer(file) } }
            opened.onSuccess {
                onPdfOpened()
            }.onFailure { err ->
                if (isPasswordError(err)) {
                    // PDF এ পাসওয়ার্ড আছে — সেভ করা পাসওয়ার্ড থাকলে সেটা দিয়ে, নইলে ইউজারের কাছে চাওয়া হবে
                    val saved = passwordStore.get(pdfKey(file))
                    if (saved != null) {
                        unlockWithPassword(file, saved, fromSaved = true)
                    } else {
                        promptForPassword(file, null)
                    }
                } else {
                    failOpen(err)
                }
            }
        }
    }

    private fun pdfKey(file: File): String = file.nameWithoutExtension

    private fun isPasswordError(err: Throwable): Boolean =
        err is SecurityException ||
            err.message?.contains("password", ignoreCase = true) == true

    /** [file] দিয়ে PdfRenderer খোলে। পাসওয়ার্ড লাগলে SecurityException ছুঁড়ে। */
    private fun openRenderer(file: File) {
        closeRenderer()
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            val renderer = PdfRenderer(pfd)
            synchronized(rendererLock) {
                pdfFileDescriptor = pfd
                pdfRenderer = renderer
                totalPages = renderer.pageCount
            }
        } catch (t: Throwable) {
            runCatching { pfd.close() }
            throw t
        }
    }

    private fun closeRenderer() {
        synchronized(rendererLock) {
            runCatching { pdfRenderer?.close() }
            runCatching { pdfFileDescriptor?.close() }
            pdfRenderer = null
            pdfFileDescriptor = null
        }
    }

    private fun onPdfOpened() {
        binding.layoutViewerLoading.visibility = View.GONE
        if (totalPages <= 0) {
            Toast.makeText(this, "PDF এ কোনো পৃষ্ঠা নেই।", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.seekBarPage.max = (totalPages - 1).coerceAtLeast(0)
        setupPdfRecycler()
        updatePageIndicator(0)
    }

    private fun failOpen(err: Throwable) {
        binding.layoutViewerLoading.visibility = View.GONE
        Toast.makeText(
            this,
            "PDF খুলতে সমস্যা হয়েছে: ${err.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    /** পাসওয়ার্ড ইনপুট ডায়ালগ। ভুল হলে [errorText] সহ আবার দেখানো হয়। */
    private fun promptForPassword(file: File, errorText: String?) {
        if (isFinishing || isDestroyed) return
        binding.layoutViewerLoading.visibility = View.GONE

        val til = TextInputLayout(this).apply {
            hint = "PDF এর পাসওয়ার্ড"
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            error = errorText
        }
        val et = TextInputEditText(til.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        til.addView(et)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val container = android.widget.FrameLayout(this).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(til)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("PDF পাসওয়ার্ড দিন")
            .setMessage("এই PDF টি পাসওয়ার্ড দিয়ে সুরক্ষিত। একবার সঠিক পাসওয়ার্ড দিলে অ্যাপ এটা মনে রাখবে।")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("খুলুন", null)
            .setNegativeButton("বাতিল") { _, _ -> finish() }
            .create()
        passwordDialog = dialog

        dialog.setOnShowListener {
            val submit = {
                val pwd = et.text?.toString().orEmpty()
                if (pwd.isEmpty()) {
                    til.error = "পাসওয়ার্ড লিখুন"
                } else {
                    dialog.dismiss()
                    unlockWithPassword(file, pwd, fromSaved = false)
                }
            }
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener { submit() }
            et.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
            }
            et.requestFocus()
            dialog.window?.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            )
        }
        dialog.show()
    }

    /**
     * পাসওয়ার্ড দিয়ে PDF এর সুরক্ষা সরিয়ে একটি অস্থায়ী (cache) কপি বানায়, তারপর সেটা খোলে।
     * আসল ডাউনলোড ফাইল অপরিবর্তিত থাকে; অস্থায়ী কপি অ্যাপ বন্ধ হলে মুছে যায়।
     * সঠিক হলে পাসওয়ার্ড সেভ হয়।
     */
    private fun unlockWithPassword(file: File, password: String, fromSaved: Boolean) {
        binding.layoutViewerLoading.visibility = View.VISIBLE
        binding.tvLoadingStatus.text = "PDF আনলক হচ্ছে…"

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    PDFBoxResourceLoader.init(applicationContext)
                    val dir = File(cacheDir, "decrypted_pdfs").apply { mkdirs() }
                    val out = File(dir, "${pdfKey(file)}.pdf")
                    PDDocument.load(file, password, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                        doc.setAllSecurityToBeRemoved(true)
                        doc.save(out)
                    }
                    openRenderer(out)
                    out
                }
            }
            result.onSuccess { out ->
                decryptedTempFile = out
                passwordStore.save(pdfKey(file), password)
                onPdfOpened()
            }.onFailure { err ->
                if (err is InvalidPasswordException || isPasswordError(err)) {
                    passwordStore.remove(pdfKey(file))
                    promptForPassword(
                        file,
                        if (fromSaved) "সংরক্ষিত পাসওয়ার্ডটি আর কাজ করছে না, নতুন পাসওয়ার্ড দিন"
                        else "পাসওয়ার্ড ভুল, আবার চেষ্টা করুন"
                    )
                } else {
                    failOpen(err)
                }
            }
        }
    }

    private fun setupPdfRecycler() {
        val lm = LinearLayoutManager(this)
        binding.recyclerPdfPages.layoutManager = lm
        val adapter = PdfPagesAdapter()
        binding.recyclerPdfPages.adapter = adapter

        binding.recyclerPdfPages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val firstVisible = lm.findFirstVisibleItemPosition()
                if (firstVisible != RecyclerView.NO_POSITION) {
                    updatePageIndicator(firstVisible)
                }
            }
        })
    }

    private inner class PdfPagesAdapter : RecyclerView.Adapter<PdfPagesAdapter.PageVH>() {

        inner class PageVH(val itemBinding: ItemPdfPageBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            var boundPage = -1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
            val b = ItemPdfPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PageVH(b)
        }

        override fun getItemCount(): Int = totalPages

        override fun onBindViewHolder(holder: PageVH, position: Int) {
            holder.boundPage = position
            holder.itemBinding.tvPageBadge.text = "${position + 1} / $totalPages"

            val cached = pageCache.get(position)
            if (cached != null && !cached.isRecycled) {
                holder.itemBinding.ivPdfPage.setImageBitmap(cached)
                return
            }

            holder.itemBinding.ivPdfPage.setImageDrawable(null)
            val displayWidth = resources.displayMetrics.widthPixels

            lifecycleScope.launch(Dispatchers.IO) {
                val bmp = renderPageBitmap(position, (displayWidth * 1.6f).toInt().coerceAtMost(2048))
                if (bmp != null) {
                    pageCache.put(position, bmp)
                    withContext(Dispatchers.Main) {
                        if (holder.boundPage == position) {
                            holder.itemBinding.ivPdfPage.setImageBitmap(bmp)
                        }
                    }
                }
            }
        }
    }

    private fun renderPageBitmap(pageIndex: Int, targetWidth: Int): Bitmap? {
        val renderer = pdfRenderer ?: return null
        return synchronized(rendererLock) {
            runCatching {
                renderer.openPage(pageIndex).use { page ->
                    val ratio = page.height.toFloat() / page.width.coerceAtLeast(1)
                    val w = targetWidth.coerceAtLeast(300)
                    val h = (w * ratio).toInt().coerceAtLeast(300)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }.getOrNull()
        }
    }

    override fun onDestroy() {
        detachRemoteListener()
        super.onDestroy()
        passwordDialog?.dismiss()
        closeRenderer()
        runCatching { decryptedTempFile?.delete() }
        pageCache.evictAll()
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_LOCAL_PATH = "extra_local_path"
        const val EXTRA_REMOTE_URL = "extra_remote_url"
        const val EXTRA_SUBJECT = "extra_subject"
    }
}
