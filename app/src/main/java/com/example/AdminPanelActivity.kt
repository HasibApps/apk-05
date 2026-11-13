package com.example

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.data.ChapterItem
import com.example.data.DefaultGuideData
import com.example.data.GuideDatabase
import com.example.data.PaperData
import com.example.data.ResourceItem
import com.example.data.SubjectData
import com.example.data.GuideRepository
import com.example.databinding.ActivityAdminPanelBinding
import com.example.databinding.DialogEditFolderBinding
import com.example.databinding.DialogEditPdfBinding
import com.example.databinding.ItemAdminChapterEditBinding
import com.example.databinding.ItemAdminResourceCardBinding
import com.example.databinding.ItemSectionHeaderBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminPanelBinding
    private lateinit var repository: GuideRepository
    private lateinit var workingDb: GuideDatabase

    private var selectedSubject: String = DefaultGuideData.SUBJECT_ORDER.first()
    private var isFirstPaper: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = GuideRepository.getInstance(this)
        workingDb = deepCopyDatabase(repository.getDatabase())

        applyWindowInsets()
        setupHeaderActions()
        setupSubjectChips()
        setupPaperTabs()
        renderPaperContent()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootAdminPanel) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.adminHeader.updatePadding(top = sysBars.top + 14)
            binding.rootAdminPanel.updatePadding(bottom = sysBars.bottom)
            insets
        }
    }

    private fun setupHeaderActions() {
        binding.btnAdminBack.setOnClickListener { finish() }

        binding.btnAdminSaveAll.setOnClickListener {
            repository.saveDatabase(workingDb)
            Toast.makeText(this, getString(R.string.admin_saved_toast), Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnAdminReset.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("ডিফল্ট রিসেট করবেন?")
                .setMessage("সব ৭টি বিষয়ের ডাটা আগের ডিফল্ট অবস্থায় ফিরে যাবে।")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("রিসেট করুন") { _, _ ->
                    workingDb = deepCopyDatabase(repository.resetToDefault())
                    renderPaperContent()
                    Toast.makeText(this, "ডিফল্ট ডাটা রিসেট হয়েছে।", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun setupSubjectChips() {
        binding.chipGroupSubjects.removeAllViews()
        val subjects = workingDb.subjects.keys.toList().ifEmpty { DefaultGuideData.SUBJECT_ORDER }
        selectedSubject = subjects.first()

        subjects.forEachIndexed { idx, subj ->
            val chip = Chip(this).apply {
                text = subj
                isCheckable = true
                isChecked = idx == 0
                setOnClickListener {
                    selectedSubject = subj
                    renderPaperContent()
                }
            }
            binding.chipGroupSubjects.addView(chip)
        }
    }

    private fun setupPaperTabs() {
        binding.adminTabFirst.setOnClickListener {
            if (!isFirstPaper) {
                isFirstPaper = true
                updatePaperTabStyle()
                renderPaperContent()
            }
        }
        binding.adminTabSecond.setOnClickListener {
            if (isFirstPaper) {
                isFirstPaper = false
                updatePaperTabStyle()
                renderPaperContent()
            }
        }
        updatePaperTabStyle()
    }

    private fun updatePaperTabStyle() {
        if (isFirstPaper) {
            binding.adminTabFirst.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.adminTabFirst.setTextColor(getColor(R.color.white))
            binding.adminTabSecond.background = null
            binding.adminTabSecond.setTextColor(getColor(R.color.text_secondary))
        } else {
            binding.adminTabSecond.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.adminTabSecond.setTextColor(getColor(R.color.white))
            binding.adminTabFirst.background = null
            binding.adminTabFirst.setTextColor(getColor(R.color.text_secondary))
        }
    }

    private fun getCurrentPaperData(): PaperData {
        val subjData = workingDb.subjects.getOrPut(selectedSubject) { SubjectData() }
        return if (isFirstPaper) subjData.first else subjData.second
    }

    private fun renderPaperContent() {
        binding.layoutAdminSections.removeAllViews()
        val paperData = getCurrentPaperData()

        // 1. Main Book Section
        addSectionBlock(
            title = "📗 মূল বই ($selectedSubject • ${if (isFirstPaper) "1st Paper" else "2nd Paper"})",
            iconRes = R.drawable.ic_book,
            items = paperData.mainBook
        )

        // 2. Test Paper Section
        addSectionBlock(
            title = "📋 Test Paper ($selectedSubject • ${if (isFirstPaper) "1st Paper" else "2nd Paper"})",
            iconRes = R.drawable.ic_test_paper,
            items = paperData.testPaper
        )
    }

    private fun addSectionBlock(
        title: String,
        iconRes: Int,
        items: MutableList<ResourceItem>
    ) {
        val headerBinding = ItemSectionHeaderBinding.inflate(layoutInflater, binding.layoutAdminSections, false)
        headerBinding.ivSectionIcon.setImageResource(iconRes)
        headerBinding.tvSectionTitle.text = title
        binding.layoutAdminSections.addView(headerBinding.root)

        items.forEachIndexed { index, item ->
            val cardBinding = ItemAdminResourceCardBinding.inflate(layoutInflater, binding.layoutAdminSections, false)
            cardBinding.tvAdminItemTitle.text = item.title
            if (item.isCQ) {
                cardBinding.ivAdminItemType.setImageResource(R.drawable.ic_folder)
                cardBinding.tvAdminItemSub.text = "ফোল্ডার • ${item.chapters.size} টি অধ্যায়/বিষয়"
            } else {
                cardBinding.ivAdminItemType.setImageResource(R.drawable.ic_pdf_doc)
                cardBinding.tvAdminItemSub.text = item.link.ifBlank { "লিংক সেট করা হয়নি" }
            }

            cardBinding.btnAdminEditItem.setOnClickListener {
                if (item.isCQ) {
                    openFolderEditorDialog(item) { renderPaperContent() }
                } else {
                    openSinglePdfEditorDialog(item) { renderPaperContent() }
                }
            }

            cardBinding.btnAdminDeleteItem.setOnClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle("আইটেম মুছবেন?")
                    .setMessage("“${item.title}” মুছে ফেলতে চান?")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        items.removeAt(index)
                        renderPaperContent()
                    }
                    .show()
            }

            binding.layoutAdminSections.addView(cardBinding.root)
        }

        // Action row: + Add Folder | + Add Single PDF
        val actionRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 14)
        }

        val btnAddFolder = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(R.string.admin_add_folder)
            textSize = 12.5f
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            setOnClickListener {
                val newFolder = ResourceItem(
                    title = "নতুন ফোল্ডার",
                    isCQ = true,
                    chapters = mutableListOf(ChapterItem("অধ্যায় ১", ""))
                )
                openFolderEditorDialog(newFolder) {
                    items.add(newFolder)
                    renderPaperContent()
                }
            }
        }

        val btnAddPdf = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(R.string.admin_add_pdf)
            textSize = 12.5f
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 8
            }
            setOnClickListener {
                val newPdf = ResourceItem(
                    title = "নতুন PDF গাইড",
                    isCQ = false,
                    link = ""
                )
                openSinglePdfEditorDialog(newPdf) {
                    items.add(newPdf)
                    renderPaperContent()
                }
            }
        }

        actionRow.addView(btnAddFolder)
        actionRow.addView(btnAddPdf)
        binding.layoutAdminSections.addView(actionRow)
    }

    private fun openFolderEditorDialog(folder: ResourceItem, onSaved: () -> Unit) {
        val dlgBinding = DialogEditFolderBinding.inflate(layoutInflater)
        dlgBinding.etFolderTitle.setText(folder.title)
        dlgBinding.etChapterCount.setText(folder.chapters.size.toString())

        val workingChapters = folder.chapters.map { ChapterItem(it.name, it.link) }.toMutableList()
        val rowBindings = mutableListOf<ItemAdminChapterEditBinding>()

        fun rebuildChapterRows() {
            dlgBinding.layoutFolderChaptersEdit.removeAllViews()
            rowBindings.clear()
            workingChapters.forEachIndexed { idx, ch ->
                val row = ItemAdminChapterEditBinding.inflate(layoutInflater, dlgBinding.layoutFolderChaptersEdit, false)
                row.tvChapterIndex.text = "বিষয় / অধ্যায় ${idx + 1}"
                row.etChapterName.setText(ch.name)
                row.etChapterLink.setText(ch.link)
                row.btnRemoveChapter.setOnClickListener {
                    syncRowsInto(workingChapters, rowBindings)
                    if (idx < workingChapters.size) {
                        workingChapters.removeAt(idx)
                        dlgBinding.etChapterCount.setText(workingChapters.size.toString())
                        rebuildChapterRows()
                    }
                }
                rowBindings.add(row)
                dlgBinding.layoutFolderChaptersEdit.addView(row.root)
            }
        }

        rebuildChapterRows()

        dlgBinding.btnApplyChapterCount.setOnClickListener {
            syncRowsInto(workingChapters, rowBindings)
            val targetCount = dlgBinding.etChapterCount.text?.toString()?.trim()?.toIntOrNull()
            if (targetCount == null || targetCount < 0 || targetCount > 50) {
                Toast.makeText(this, "০ থেকে ৫০ এর মধ্যে সংখ্যা দিন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            while (workingChapters.size < targetCount) {
                val n = workingChapters.size + 1
                workingChapters.add(ChapterItem("অধ্যায় $n", ""))
            }
            while (workingChapters.size > targetCount) {
                workingChapters.removeAt(workingChapters.lastIndex)
            }
            rebuildChapterRows()
        }

        dlgBinding.btnAddChapterRow.setOnClickListener {
            syncRowsInto(workingChapters, rowBindings)
            val n = workingChapters.size + 1
            workingChapters.add(ChapterItem("অধ্যায় $n", ""))
            dlgBinding.etChapterCount.setText(workingChapters.size.toString())
            rebuildChapterRows()
        }

        MaterialAlertDialogBuilder(this)
            .setView(dlgBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                syncRowsInto(workingChapters, rowBindings)
                val newTitle = dlgBinding.etFolderTitle.text?.toString()?.trim().orEmpty()
                if (newTitle.isNotEmpty()) {
                    folder.title = newTitle
                }
                folder.chapters.clear()
                folder.chapters.addAll(workingChapters)
                onSaved()
            }
            .show()
    }

    private fun syncRowsInto(
        list: MutableList<ChapterItem>,
        rows: List<ItemAdminChapterEditBinding>
    ) {
        rows.forEachIndexed { idx, row ->
            if (idx < list.size) {
                list[idx].name = row.etChapterName.text?.toString()?.trim().orEmpty().ifBlank { "অধ্যায় ${idx + 1}" }
                list[idx].link = row.etChapterLink.text?.toString()?.trim().orEmpty()
            }
        }
    }

    private fun openSinglePdfEditorDialog(item: ResourceItem, onSaved: () -> Unit) {
        val dlgBinding = DialogEditPdfBinding.inflate(layoutInflater)
        dlgBinding.etPdfItemTitle.setText(item.title)
        dlgBinding.etPdfItemLink.setText(item.link)

        MaterialAlertDialogBuilder(this)
            .setView(dlgBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = dlgBinding.etPdfItemTitle.text?.toString()?.trim().orEmpty()
                val link = dlgBinding.etPdfItemLink.text?.toString()?.trim().orEmpty()
                if (title.isNotEmpty()) item.title = title
                item.link = link
                onSaved()
            }
            .show()
    }

    private fun deepCopyDatabase(src: GuideDatabase): GuideDatabase {
        val copyMap = linkedMapOf<String, SubjectData>()
        src.subjects.forEach { (key, subj) ->
            copyMap[key] = SubjectData(
                first = copyPaper(subj.first),
                second = copyPaper(subj.second)
            )
        }
        return GuideDatabase(copyMap)
    }

    private fun copyPaper(src: PaperData): PaperData {
        return PaperData(
            mainBook = src.mainBook.map { copyResource(it) }.toMutableList(),
            testPaper = src.testPaper.map { copyResource(it) }.toMutableList()
        )
    }

    private fun copyResource(src: ResourceItem): ResourceItem {
        return ResourceItem(
            title = src.title,
            isCQ = src.isCQ,
            link = src.link,
            chapters = src.chapters.map { ChapterItem(it.name, it.link) }.toMutableList()
        )
    }
}
