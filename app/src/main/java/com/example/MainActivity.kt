package com.example

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data.DefaultGuideData
import com.example.data.DownloadedPdfMeta
import com.example.data.GuideRepository
import com.example.databinding.ActivityMainBinding
import com.example.databinding.ItemDownloadedPdfBinding
import com.example.databinding.ItemSubjectCardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: GuideRepository
    private lateinit var settingsController: SettingsController

    private var showingOfflineTab = false
    private var allOfflinePdfs: List<DownloadedPdfMeta> = emptyList()
    private lateinit var offlineAdapter: OfflinePdfAdapter
    private lateinit var subjectAdapter: SubjectGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = GuideRepository.getInstance(this)
        settingsController = SettingsController(this) { refreshAll() }

        applyWindowInsets()
        setupHeader()
        setupSubjectsGrid()
        setupOfflineLibrary()
        setupBottomNav()
        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootMain) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.headerContainer.updatePadding(top = sysBars.top + 14)
            val navParams = binding.bottomNavCard.layoutParams as ViewGroup.MarginLayoutParams
            navParams.bottomMargin = sysBars.bottom + 12
            binding.bottomNavCard.layoutParams = navParams
            insets
        }
    }

    private fun setupHeader() {
        binding.btnSettings.setOnClickListener {
            settingsController.showSettingsSheet()
        }
    }

    private fun setupSubjectsGrid() {
        val spanCount = if (resources.configuration.screenWidthDp >= 600) 3 else 2
        binding.recyclerSubjects.layoutManager = GridLayoutManager(this, spanCount)
        subjectAdapter = SubjectGridAdapter { subjectName ->
            val intent = Intent(this, SubjectActivity::class.java).apply {
                putExtra(SubjectActivity.EXTRA_SUBJECT, subjectName)
            }
            startActivity(intent)
        }
        binding.recyclerSubjects.adapter = subjectAdapter
    }

    private fun setupOfflineLibrary() {
        binding.recyclerOfflinePdfs.layoutManager = LinearLayoutManager(this)
        offlineAdapter = OfflinePdfAdapter(
            onOpen = { meta ->
                val intent = Intent(this, PdfViewerActivity::class.java).apply {
                    putExtra(PdfViewerActivity.EXTRA_TITLE, meta.title)
                    putExtra(PdfViewerActivity.EXTRA_SUBJECT, meta.subject)
                    putExtra(PdfViewerActivity.EXTRA_LOCAL_PATH, meta.pdfPath)
                }
                startActivity(intent)
            },
            onDelete = { meta ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("অফলাইন ফাইল মুছবেন?")
                    .setMessage("“${meta.title}” ডাউনলোড লিস্ট থেকে মুছে ফেলতে চান?")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        repository.deleteDownloadedPdf(meta.id)
                        refreshOfflineList()
                    }
                    .show()
            }
        )
        binding.recyclerOfflinePdfs.adapter = offlineAdapter

        binding.etSearchOffline.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterOfflineList(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupBottomNav() {
        binding.navSubjects.setOnClickListener { switchBottomTab(offline = false) }
        binding.navOffline.setOnClickListener { switchBottomTab(offline = true) }
        switchBottomTab(offline = false)
    }

    private fun switchBottomTab(offline: Boolean) {
        showingOfflineTab = offline
        if (offline) {
            binding.recyclerSubjects.visibility = View.GONE
            binding.layoutOfflineContainer.visibility = View.VISIBLE

            binding.navOffline.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tvNavOffline.setTextColor(getColor(R.color.white))
            binding.ivNavOffline.setColorFilter(getColor(R.color.white))

            binding.navSubjects.background = null
            binding.tvNavSubjects.setTextColor(0xFF9CB3BD.toInt())
            binding.ivNavSubjects.setColorFilter(0xFF9CB3BD.toInt())
            refreshOfflineList()
        } else {
            binding.layoutOfflineContainer.visibility = View.GONE
            binding.recyclerSubjects.visibility = View.VISIBLE

            binding.navSubjects.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tvNavSubjects.setTextColor(getColor(R.color.white))
            binding.ivNavSubjects.setColorFilter(getColor(R.color.white))

            binding.navOffline.background = null
            binding.tvNavOffline.setTextColor(0xFF9CB3BD.toInt())
            binding.ivNavOffline.setColorFilter(0xFF9CB3BD.toInt())
        }
    }

    private fun refreshAll() {
        val db = repository.getDatabase()
        val subjectNames = db.subjects.keys.toList().ifEmpty { DefaultGuideData.SUBJECT_ORDER }
        subjectAdapter.submitList(subjectNames)
        refreshOfflineList()
    }

    private fun refreshOfflineList() {
        allOfflinePdfs = repository.getDownloadedPdfs()
        binding.tvOfflineCountBadge.text = "${toBengaliDigits(allOfflinePdfs.size)} অফলাইন"
        filterOfflineList(binding.etSearchOffline.text?.toString().orEmpty())
    }

    private fun filterOfflineList(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            allOfflinePdfs
        } else {
            allOfflinePdfs.filter {
                it.title.lowercase().contains(q) || it.subject.lowercase().contains(q)
            }
        }
        offlineAdapter.submitList(filtered)
        binding.layoutOfflineEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerOfflinePdfs.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun toBengaliDigits(number: Int): String {
        val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return number.toString().map { ch ->
            if (ch in '0'..'9') bn[ch - '0'] else ch
        }.joinToString("")
    }

    private inner class SubjectGridAdapter(
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<SubjectGridAdapter.SubjectVH>() {

        private var items: List<String> = emptyList()

        fun submitList(list: List<String>) {
            items = list
            notifyDataSetChanged()
        }

        inner class SubjectVH(val b: ItemSubjectCardBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectVH {
            val b = ItemSubjectCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SubjectVH(b)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: SubjectVH, position: Int) {
            val subject = items[position]
            val b = holder.b
            b.tvSubjectTitle.text = subject

            when (subject) {
                "বাংলা" -> {
                    b.subjectGradientBox.setBackgroundResource(R.drawable.bg_subject_bangla)
                    b.tvSubjectGlyph.visibility = View.VISIBLE
                    b.tvSubjectGlyph.text = "অ"
                    b.ivSubjectIcon.visibility = View.GONE
                }
                "ইংরেজি" -> {
                    b.subjectGradientBox.setBackgroundResource(R.drawable.bg_subject_english)
                    b.tvSubjectGlyph.visibility = View.VISIBLE
                    b.tvSubjectGlyph.text = "En"
                    b.ivSubjectIcon.visibility = View.GONE
                }
                "আইসিটি" -> {
                    b.subjectGradientBox.setBackgroundResource(R.drawable.bg_subject_ict)
                    b.tvSubjectGlyph.visibility = View.GONE
                    b.ivSubjectIcon.visibility = View.VISIBLE
                    b.ivSubjectIcon.setImageResource(R.drawable.ic_subj_ict)
                }
                "পদার্থবিজ্ঞান" -> {
                    b.subjectGradientBox.setBackgroundResource(R.drawable.bg_subject_physics)
                    b.tvSubjectGlyph.visibility = View.GONE
                    b.ivSubjectIcon.visibility = View.VISIBLE
                    b.ivSubjectIcon.setImageResource(R.drawable.ic_subj_physics)
                }
                "রসায়ন" -> {
                    b.subjectGradientBox.setBackgroundResource(R.drawable.bg_subject_chemistry)
                    b.tvSubjectGlyph.visibility = View.GONE
                    b.ivSubjectIcon.visibility = View.VISIBLE
                    b.ivSubjectIcon.setImageResource(R.drawable.ic_subj_chemistry)
                }
                "জীববিজ্ঞান" -> {
                    b.subjectGradientBox.setBackgroundResource(R.drawable.bg_subject_biology)
                    b.tvSubjectGlyph.visibility = View.GONE
                    b.ivSubjectIcon.visibility = View.VISIBLE
                    b.ivSubjectIcon.setImageResource(R.drawable.ic_subj_biology)
                }
                else -> {
                    b.subjectGradientBox.setBackgroundResource(R.drawable.bg_subject_math)
                    b.tvSubjectGlyph.visibility = View.GONE
                    b.ivSubjectIcon.visibility = View.VISIBLE
                    b.ivSubjectIcon.setImageResource(R.drawable.ic_subj_math)
                }
            }

            b.cardSubject.setOnClickListener { onClick(subject) }
        }
    }

    private inner class OfflinePdfAdapter(
        private val onOpen: (DownloadedPdfMeta) -> Unit,
        private val onDelete: (DownloadedPdfMeta) -> Unit
    ) : RecyclerView.Adapter<OfflinePdfAdapter.OfflineVH>() {

        private var items: List<DownloadedPdfMeta> = emptyList()

        fun submitList(list: List<DownloadedPdfMeta>) {
            items = list
            notifyDataSetChanged()
        }

        inner class OfflineVH(val b: ItemDownloadedPdfBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfflineVH {
            val b = ItemDownloadedPdfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return OfflineVH(b)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: OfflineVH, position: Int) {
            val meta = items[position]
            val b = holder.b
            b.tvPdfTitle.text = meta.title
            val dateStr = GuideRepository.formatShortDate(meta.downloadedAtMillis)
            val sizeStr = GuideRepository.formatFileSize(meta.fileSizeBytes)
            b.tvPdfMeta.text = "$dateStr   $sizeStr   •   ${meta.pageCount} পৃষ্ঠা"
            b.tvPdfSubjectBadge.text = meta.subject

            val thumbFile = File(meta.thumbnailPath)
            if (thumbFile.exists()) {
                val bmp = BitmapFactory.decodeFile(thumbFile.absolutePath)
                if (bmp != null) {
                    b.ivPdfThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                    b.ivPdfThumbnail.setImageBitmap(bmp)
                } else {
                    b.ivPdfThumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    b.ivPdfThumbnail.setImageResource(R.drawable.ic_pdf_doc)
                }
            } else {
                b.ivPdfThumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
                b.ivPdfThumbnail.setImageResource(R.drawable.ic_pdf_doc)
            }

            b.cardDownloadedPdf.setOnClickListener { onOpen(meta) }
            b.btnDeleteOffline.setOnClickListener { onDelete(meta) }
        }
    }
}
