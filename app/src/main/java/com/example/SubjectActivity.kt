package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.data.GuideRepository
import com.example.data.ResourceItem
import com.example.databinding.ActivitySubjectBinding
import com.example.databinding.ItemPdfFolderBinding
import com.example.databinding.ItemPdfRowBinding
import com.example.databinding.ItemSectionHeaderBinding
import com.example.download.DownloadCenter
import com.example.download.DownloadFormat
import com.example.download.DownloadState
import com.example.download.DownloadStatus
import com.example.download.PdfDownloadService
import kotlinx.coroutines.launch
import java.io.File

class SubjectActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectBinding
    private lateinit var repository: GuideRepository
    private lateinit var settingsController: SettingsController

    private var subjectName: String = ""
    private var isFirstPaper: Boolean = true
    private val expandedFolders = mutableSetOf<String>()

    /** url -> বর্তমানে স্ক্রিনে থাকা সারিগুলো (একই লিংক একাধিক জায়গায় থাকতে পারে) */
    private val rowViews = mutableMapOf<String, MutableList<ItemPdfRowBinding>>()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val downloadListener = DownloadCenter.Listener { state -> onDownloadChanged(state) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySubjectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = GuideRepository.getInstance(this)
        settingsController = SettingsController(this) { renderPaper() }

        subjectName = intent.getStringExtra(EXTRA_SUBJECT).orEmpty().ifBlank { "বাংলা" }
        binding.tvSubjectHeaderTitle.text = "$subjectName (একাদশ-দ্বাদশ)"

        applyWindowInsets()
        setupListeners()
        renderPaper()
    }

    override fun onStart() {
        super.onStart()
        DownloadCenter.addListener(downloadListener)
    }

    override fun onStop() {
        DownloadCenter.removeListener(downloadListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        renderPaper()
    }

    private fun onDownloadChanged(state: DownloadState) {
        when (state.status) {
            DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> {
                rowViews[state.url]?.forEach { applyDownloadingUi(it, state) }
            }
            DownloadStatus.DONE, DownloadStatus.CANCELLED -> renderPaper()
            DownloadStatus.FAILED -> {
                renderPaper()
                Toast.makeText(
                    this,
                    "“${state.title}”: ${state.error ?: "ডাউনলোড করা যায়নি"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootSubject) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.subjectHeader.updatePadding(top = sysBars.top + 14)
            binding.scrollSubjectContent.updatePadding(bottom = sysBars.bottom + 20)
            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSubjectSettings.setOnClickListener { settingsController.showSettingsSheet() }

        binding.tabFirstPaper.setOnClickListener {
            if (!isFirstPaper) {
                isFirstPaper = true
                updatePaperTabStyles()
                renderPaper()
            }
        }
        binding.tabSecondPaper.setOnClickListener {
            if (isFirstPaper) {
                isFirstPaper = false
                updatePaperTabStyles()
                renderPaper()
            }
        }
        updatePaperTabStyles()
    }

    private fun updatePaperTabStyles() {
        if (isFirstPaper) {
            binding.tabFirstPaper.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabFirstPaper.setTextColor(getColor(R.color.white))
            binding.tabSecondPaper.background = null
            binding.tabSecondPaper.setTextColor(getColor(R.color.text_secondary))
        } else {
            binding.tabSecondPaper.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabSecondPaper.setTextColor(getColor(R.color.white))
            binding.tabFirstPaper.background = null
            binding.tabFirstPaper.setTextColor(getColor(R.color.text_secondary))
        }
    }

    private fun renderPaper() {
        val savedScrollY = binding.scrollSubjectContent.scrollY
        rowViews.clear()
        binding.layoutSectionsContainer.removeAllViews()
        val subjectData = repository.getDatabase().subjects[subjectName]
        val paperData = if (isFirstPaper) subjectData?.first else subjectData?.second

        if (paperData == null) return

        if (paperData.mainBook.isNotEmpty()) {
            addSectionHeader(getString(R.string.section_main_book), R.drawable.ic_book)
            paperData.mainBook.forEachIndexed { idx, item ->
                addResourceCard(item, "main_$idx")
            }
        }

        if (paperData.testPaper.isNotEmpty()) {
            addSectionHeader(getString(R.string.section_test_paper), R.drawable.ic_test_paper)
            paperData.testPaper.forEachIndexed { idx, item ->
                addResourceCard(item, "test_$idx")
            }
        }

        if (savedScrollY > 0) {
            binding.scrollSubjectContent.post { binding.scrollSubjectContent.scrollTo(0, savedScrollY) }
        }
    }

    private fun addSectionHeader(title: String, iconRes: Int) {
        val header = ItemSectionHeaderBinding.inflate(layoutInflater, binding.layoutSectionsContainer, false)
        header.ivSectionIcon.setImageResource(iconRes)
        header.tvSectionTitle.text = title
        binding.layoutSectionsContainer.addView(header.root)
    }

    private fun addResourceCard(item: ResourceItem, uniqueKey: String) {
        val folderKey = "${subjectName}_${isFirstPaper}_$uniqueKey"
        if (item.isCQ) {
            val folderBinding = ItemPdfFolderBinding.inflate(layoutInflater, binding.layoutSectionsContainer, false)
            folderBinding.tvFolderTitle.text = item.title
            folderBinding.tvFolderCount.text = "${item.chapters.size}টি অধ্যায় / বিষয়"

            val isExpanded = expandedFolders.contains(folderKey)
            folderBinding.layoutChaptersContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            folderBinding.ivFolderChevron.setImageResource(
                if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )

            folderBinding.layoutFolderHeader.setOnClickListener {
                val nowExpanded = !expandedFolders.contains(folderKey)
                if (nowExpanded) expandedFolders.add(folderKey) else expandedFolders.remove(folderKey)
                folderBinding.layoutChaptersContainer.visibility = if (nowExpanded) View.VISIBLE else View.GONE
                folderBinding.ivFolderChevron.setImageResource(
                    if (nowExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
                )
            }

            folderBinding.layoutChaptersContainer.removeAllViews()
            item.chapters.forEach { chapter ->
                val rowBinding = ItemPdfRowBinding.inflate(layoutInflater, folderBinding.layoutChaptersContainer, false)
                bindPdfRow(rowBinding, chapter.name, chapter.link)
                folderBinding.layoutChaptersContainer.addView(rowBinding.root)
            }

            binding.layoutSectionsContainer.addView(folderBinding.root)
        } else {
            val rowBinding = ItemPdfRowBinding.inflate(layoutInflater, binding.layoutSectionsContainer, false)
            bindPdfRow(rowBinding, item.title, item.link)
            binding.layoutSectionsContainer.addView(rowBinding.root)
        }
    }

    private fun bindPdfRow(rowBinding: ItemPdfRowBinding, title: String, url: String) {
        rowBinding.tvRowTitle.text = title
        if (url.isNotBlank()) {
            rowViews.getOrPut(url) { mutableListOf() }.add(rowBinding)
        }

        val downloaded = repository.findDownloadedByUrl(url)
        val isOffline = downloaded != null && File(downloaded.pdfPath).exists()
        val activeState = DownloadCenter.get(url)?.takeIf { it.isActive }

        rowBinding.progressLinear.visibility = View.GONE

        if (isOffline && downloaded != null) {
            showThumbnail(rowBinding, downloaded)
            val sizeStr = GuideRepository.formatFileSize(downloaded.fileSizeBytes)
            val dateStr = GuideRepository.formatShortDate(downloaded.downloadedAtMillis)
            val pages = if (downloaded.pageCount > 0) " (${downloaded.pageCount} পৃষ্ঠা)" else ""
            rowBinding.tvRowSubtitle.text = "অফলাইন রেডি • $dateStr • $sizeStr$pages"
            rowBinding.tvRowSubtitle.setTextColor(getColor(R.color.cyber_emerald_dark))
            rowBinding.btnDownloadPdf.setImageResource(R.drawable.ic_offline_check)
            rowBinding.btnDownloadPdf.contentDescription = getString(R.string.cd_open_offline)
        } else {
            showPlaceholderThumb(rowBinding)
            if (activeState != null) {
                applyDownloadingUi(rowBinding, activeState)
            } else {
                rowBinding.tvRowSubtitle.text = "অনলাইন PDF • অফলাইনে সেভ করতে ডাউনলোড করুন"
                rowBinding.tvRowSubtitle.setTextColor(getColor(R.color.text_muted))
                rowBinding.btnDownloadPdf.setImageResource(R.drawable.ic_download)
                rowBinding.btnDownloadPdf.contentDescription = getString(R.string.cd_download_pdf)
            }
        }

        rowBinding.cardPdfRow.setOnClickListener {
            if (url.isBlank()) {
                Toast.makeText(this, "PDF লিংক সেট করা হয়নি।", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            openPdf(title, url)
        }

        rowBinding.btnDownloadPdf.setOnClickListener {
            if (url.isBlank()) {
                Toast.makeText(this, "PDF লিংক সেট করা হয়নি।", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val current = repository.findDownloadedByUrl(url)
            if (current != null && File(current.pdfPath).exists()) {
                openPdf(title, url)
                return@setOnClickListener
            }
            if (DownloadCenter.isActive(url)) {
                // চলমান/অপেক্ষমান ডাউনলোড বাতিল
                PdfDownloadService.cancel(this, url)
                return@setOnClickListener
            }
            startBackgroundDownload(url, title)
        }
    }

    private fun startBackgroundDownload(url: String, title: String) {
        ensureNotificationPermission()
        PdfDownloadService.enqueue(this, url, title, subjectName)
        val state = DownloadCenter.get(url) ?: DownloadState(url, title, subjectName)
        rowViews[url]?.forEach { applyDownloadingUi(it, state) }
    }

    private fun openPdf(title: String, url: String) {
        val downloaded = repository.findDownloadedByUrl(url)
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_TITLE, title)
            putExtra(PdfViewerActivity.EXTRA_SUBJECT, subjectName)
            if (downloaded != null && File(downloaded.pdfPath).exists()) {
                putExtra(PdfViewerActivity.EXTRA_LOCAL_PATH, downloaded.pdfPath)
            } else {
                putExtra(PdfViewerActivity.EXTRA_REMOTE_URL, url)
            }
        }
        startActivity(intent)
    }

    /** ডাউনলোড চলাকালীন সারির UI: সাইজ (MB), গতি (MB/s), প্রোগ্রেস বার ও বাতিল বাটন */
    private fun applyDownloadingUi(row: ItemPdfRowBinding, state: DownloadState) {
        row.tvRowSubtitle.text = DownloadFormat.progressLine(state)
        row.tvRowSubtitle.setTextColor(getColor(R.color.cyber_emerald_dark))
        row.progressLinear.visibility = View.VISIBLE
        if (state.status == DownloadStatus.DOWNLOADING && state.totalBytes > 0) {
            row.progressLinear.isIndeterminate = false
            row.progressLinear.max = 100
            row.progressLinear.progress = state.percent
        } else {
            row.progressLinear.isIndeterminate = true
        }
        row.btnDownloadPdf.setImageResource(R.drawable.ic_close)
        row.btnDownloadPdf.contentDescription = getString(R.string.cd_cancel_download)
    }

    private fun showPlaceholderThumb(row: ItemPdfRowBinding) {
        row.ivRowThumb.setPadding(16, 16, 16, 16)
        row.ivRowThumb.scaleType = ImageView.ScaleType.CENTER_INSIDE
        row.ivRowThumb.setImageResource(R.drawable.ic_pdf_doc)
    }

    /** ডাউনলোড করা PDF এর প্রথম পৃষ্ঠার থাম্বনেইল (না থাকলে এখনই বানিয়ে নেয়) */
    private fun showThumbnail(row: ItemPdfRowBinding, meta: com.example.data.DownloadedPdfMeta) {
        val thumbFile = File(meta.thumbnailPath)
        val bmp = if (thumbFile.exists()) BitmapFactory.decodeFile(thumbFile.absolutePath) else null
        if (bmp != null) {
            row.ivRowThumb.setPadding(0, 0, 0, 0)
            row.ivRowThumb.scaleType = ImageView.ScaleType.CENTER_CROP
            row.ivRowThumb.setImageBitmap(bmp)
        } else {
            showPlaceholderThumb(row)
            lifecycleScope.launch {
                if (repository.ensureThumbnail(meta)) {
                    val fresh = BitmapFactory.decodeFile(thumbFile.absolutePath)
                    if (fresh != null) {
                        row.ivRowThumb.setPadding(0, 0, 0, 0)
                        row.ivRowThumb.scaleType = ImageView.ScaleType.CENTER_CROP
                        row.ivRowThumb.setImageBitmap(fresh)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SUBJECT = "extra_subject"
    }
}
