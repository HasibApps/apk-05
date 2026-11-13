package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GuideDatabase(
    @Json(name = "subjects")
    val subjects: MutableMap<String, SubjectData> = linkedMapOf()
)

@JsonClass(generateAdapter = true)
data class SubjectData(
    @Json(name = "first")
    val first: PaperData = PaperData(),
    @Json(name = "second")
    val second: PaperData = PaperData()
)

@JsonClass(generateAdapter = true)
data class PaperData(
    @Json(name = "main_book")
    val mainBook: MutableList<ResourceItem> = mutableListOf(),
    @Json(name = "test_paper")
    val testPaper: MutableList<ResourceItem> = mutableListOf()
)

@JsonClass(generateAdapter = true)
data class ResourceItem(
    @Json(name = "title")
    var title: String,
    @Json(name = "isCQ")
    var isCQ: Boolean = false,
    @Json(name = "link")
    var link: String = "",
    @Json(name = "chapters")
    val chapters: MutableList<ChapterItem> = mutableListOf()
)

@JsonClass(generateAdapter = true)
data class ChapterItem(
    @Json(name = "name")
    var name: String,
    @Json(name = "link")
    var link: String
)

@JsonClass(generateAdapter = true)
data class DownloadedPdfMeta(
    @Json(name = "id")
    val id: String,
    @Json(name = "title")
    val title: String,
    @Json(name = "subject")
    val subject: String,
    @Json(name = "sourceUrl")
    val sourceUrl: String,
    @Json(name = "pdfPath")
    val pdfPath: String,
    @Json(name = "thumbnailPath")
    val thumbnailPath: String,
    @Json(name = "fileSizeBytes")
    val fileSizeBytes: Long,
    @Json(name = "pageCount")
    val pageCount: Int,
    @Json(name = "downloadedAtMillis")
    val downloadedAtMillis: Long
)

@JsonClass(generateAdapter = true)
data class DownloadedPdfStore(
    @Json(name = "items")
    val items: MutableList<DownloadedPdfMeta> = mutableListOf()
)
