package com.example

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.launch
import java.io.File

class SubjectActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectBinding
    private lateinit var repository: GuideRepository
    private lateinit var settingsController: SettingsController

    private var subjectName: String = ""
    private var isFirstPaper: Boolean = true
    private val expandedFolders = mutableSetOf<String>()
    private val downloadingUrls = mutableSetOf<String>()

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

    override fun onResume() {
        super.onResume()
        renderPaper()
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
        val downloaded = repository.findDownloadedByUrl(url)
        val isDownloading = downloadingUrls.contains(url)

        if (downloaded != null && File(downloaded.pdfPath).exists()) {
            // Show auto thumbnail, date & size
            val thumbFile = File(downloaded.thumbnailPath)
            if (thumbFile.exists()) {
                val bmp = BitmapFactory.decodeFile(thumbFile.absolutePath)
                if (bmp != null) {
                    rowBinding.ivRowThumb.setPadding(0, 0, 0, 0)
                    rowBinding.ivRowThumb.scaleType = ImageView.ScaleType.CENTER_CROP
                    rowBinding.ivRowThumb.setImageBitmap(bmp)
                }
            }
            val sizeStr = GuideRepository.formatFileSize(downloaded.fileSizeBytes)
            val dateStr = GuideRepository.formatShortDate(downloaded.downloadedAtMillis)
            rowBinding.tvRowSubtitle.text = "অফলাইন রেডি • $dateStr • $sizeStr (${downloaded.pageCount} পৃষ্ঠা)"
            rowBinding.tvRowSubtitle.setTextColor(getColor(R.color.cyber_emerald_dark))
            rowBinding.btnDownloadPdf.setImageResource(R.drawable.ic_offline_check)
            rowBinding.btnDownloadPdf.contentDescription = getString(R.string.cd_open_offline)
        } else {
            rowBinding.ivRowThumb.setPadding(16, 16, 16, 16)
            rowBinding.ivRowThumb.scaleType = ImageView.ScaleType.CENTER_INSIDE
            rowBinding.ivRowThumb.setImageResource(R.drawable.ic_pdf_doc)
            rowBinding.tvRowSubtitle.text = "অনলাইন PDF • অফলাইনে সেভ করতে ডাউনলোড করুন"
            rowBinding.tvRowSubtitle.setTextColor(getColor(R.color.text_muted))
            rowBinding.btnDownloadPdf.setImageResource(R.drawable.ic_download)
            rowBinding.btnDownloadPdf.contentDescription = getString(R.string.cd_download_pdf)
        }

        rowBinding.progressDownload.visibility = if (isDownloading) View.VISIBLE else View.GONE
        rowBinding.btnDownloadPdf.visibility = if (isDownloading) View.INVISIBLE else View.VISIBLE

        rowBinding.cardPdfRow.setOnClickListener {
            if (url.isBlank()) {
                Toast.makeText(this, "PDF লিংক সেট করা হয়নি।", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
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

        rowBinding.btnDownloadPdf.setOnClickListener {
            if (url.isBlank()) {
                Toast.makeText(this, "PDF লিংক সেট করা হয়নি।", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (downloaded != null && File(downloaded.pdfPath).exists()) {
                // Already downloaded -> open directly in Offline Reader
                val intent = Intent(this, PdfViewerActivity::class.java).apply {
                    putExtra(PdfViewerActivity.EXTRA_TITLE, title)
                    putExtra(PdfViewerActivity.EXTRA_SUBJECT, subjectName)
                    putExtra(PdfViewerActivity.EXTRA_LOCAL_PATH, downloaded.pdfPath)
                }
                startActivity(intent)
                return@setOnClickListener
            }
            if (downloadingUrls.contains(url)) return@setOnClickListener

            downloadingUrls.add(url)
            rowBinding.progressDownload.visibility = View.VISIBLE
            rowBinding.btnDownloadPdf.visibility = View.INVISIBLE
            rowBinding.tvRowSubtitle.text = "ডাউনলোড হচ্ছে…"

            lifecycleScope.launch {
                val res = repository.downloadPdfForOffline(url, title, subjectName) { pct ->
                    runOnUiThread {
                        rowBinding.tvRowSubtitle.text = "অ্যাপে ডাউনলোড হচ্ছে… $pct%"
                    }
                }
                downloadingUrls.remove(url)
                res.onSuccess {
                    Toast.makeText(
                        this@SubjectActivity,
                        "“$title” অফলাইন রিডিং এর জন্য সেভ হয়েছে!",
                        Toast.LENGTH_SHORT
                    ).show()
                    renderPaper()
                }.onFailure { err ->
                    Toast.makeText(
                        this@SubjectActivity,
                        err.localizedMessage ?: "ডাউনলোড করা যায়নি",
                        Toast.LENGTH_LONG
                    ).show()
                    renderPaper()
                }
            }
        }
    }

    companion object {
        const val EXTRA_SUBJECT = "extra_subject"
    }
}
