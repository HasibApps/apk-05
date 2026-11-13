package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GuideRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dbFile = File(appContext.filesDir, "guide_database.json")
    private val downloadsMetaFile = File(appContext.filesDir, "downloaded_pdfs.json")
    private val pdfDir = File(appContext.filesDir, "pdfs").apply { mkdirs() }
    private val thumbDir = File(appContext.filesDir, "thumbnails").apply { mkdirs() }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val dbAdapter = moshi.adapter(GuideDatabase::class.java).indent("  ")
    private val downloadsAdapter = moshi.adapter(DownloadedPdfStore::class.java).indent("  ")

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Volatile
    private var cachedDb: GuideDatabase? = null

    fun getDatabase(): GuideDatabase {
        cachedDb?.let { return it }
        val loaded = if (dbFile.exists()) {
            runCatching {
                dbAdapter.fromJson(dbFile.readText())
            }.getOrNull()
        } else null

        val db = if (loaded != null && loaded.subjects.isNotEmpty()) {
            loaded
        } else {
            DefaultGuideData.create().also { saveDatabase(it) }
        }
        cachedDb = db
        return db
    }

    fun saveDatabase(db: GuideDatabase) {
        cachedDb = db
        dbFile.writeText(dbAdapter.toJson(db))
    }

    fun exportJsonString(): String {
        return dbAdapter.toJson(getDatabase())
    }

    fun importJsonString(json: String): Boolean {
        return runCatching {
            val parsed = dbAdapter.fromJson(json) ?: return false
            if (parsed.subjects.isEmpty()) return false
            saveDatabase(parsed)
            true
        }.getOrDefault(false)
    }

    fun resetToDefault(): GuideDatabase {
        val fresh = DefaultGuideData.create()
        saveDatabase(fresh)
        return fresh
    }

    fun getDownloadedPdfs(): List<DownloadedPdfMeta> {
        val store = loadDownloadsStore()
        val valid = store.items.filter { File(it.pdfPath).exists() }
        if (valid.size != store.items.size) {
            saveDownloadsStore(DownloadedPdfStore(valid.toMutableList()))
        }
        return valid.sortedByDescending { it.downloadedAtMillis }
    }

    fun findDownloadedByUrl(url: String): DownloadedPdfMeta? {
        val id = urlToId(url)
        return getDownloadedPdfs().firstOrNull { it.id == id }
    }

    fun deleteDownloadedPdf(id: String) {
        val store = loadDownloadsStore()
        val target = store.items.firstOrNull { it.id == id }
        if (target != null) {
            File(target.pdfPath).delete()
            File(target.thumbnailPath).delete()
            store.items.removeAll { it.id == id }
            saveDownloadsStore(store)
        }
    }

    suspend fun downloadPdfForOffline(
        url: String,
        title: String,
        subject: String,
        onProgress: (Int) -> Unit
    ): Result<DownloadedPdfMeta> = withContext(Dispatchers.IO) {
        runCatching {
            val id = urlToId(url)
            val targetPdf = File(pdfDir, "$id.pdf")
            val targetThumb = File(thumbDir, "$id.png")
            val tempFile = File(pdfDir, "$id.tmp")

            val downloadUrl = resolveDirectDownloadUrl(url)
            downloadWithDriveBypass(downloadUrl, tempFile, onProgress)

            if (!isValidPdfFile(tempFile)) {
                tempFile.delete()
                throw IllegalStateException("এই লিংকটি সরাসরি PDF ফাইল নয়। সঠিক Google Drive বা PDF লিংক ব্যবহার করুন।")
            }

            if (targetPdf.exists()) targetPdf.delete()
            tempFile.renameTo(targetPdf)

            val pageCount = generateThumbnailAndCountPages(targetPdf, targetThumb)

            val meta = DownloadedPdfMeta(
                id = id,
                title = title,
                subject = subject,
                sourceUrl = url,
                pdfPath = targetPdf.absolutePath,
                thumbnailPath = targetThumb.absolutePath,
                fileSizeBytes = targetPdf.length(),
                pageCount = pageCount,
                downloadedAtMillis = System.currentTimeMillis()
            )

            val store = loadDownloadsStore()
            store.items.removeAll { it.id == id }
            store.items.add(0, meta)
            saveDownloadsStore(store)

            meta
        }
    }

    private fun downloadWithDriveBypass(
        url: String,
        outFile: File,
        onProgress: (Int) -> Unit
    ) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile) AppleWebKit/537.36")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("ডাউনলোড ব্যর্থ হয়েছে (Code: ${response.code})")
            }
            val body = response.body ?: throw IllegalStateException("ফাইল খালি রয়েছে")
            val contentType = body.contentType()?.toString()?.lowercase(Locale.ROOT).orEmpty()

            if (contentType.contains("text/html")) {
                val html = body.string()
                val confirmUrl = extractDriveConfirmUrl(html, url)
                    ?: throw IllegalStateException("Google Drive থেকে PDF ডাউনলোড করা যায়নি (পাবলিক অ্যাক্সেস পরীক্ষা করুন)।")

                val confirmReq = Request.Builder()
                    .url(confirmUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile) AppleWebKit/537.36")
                    .build()

                httpClient.newCall(confirmReq).execute().use { confirmResp ->
                    if (!confirmResp.isSuccessful) {
                        throw IllegalStateException("ডাউনলোড ব্যর্থ হয়েছে (Code: ${confirmResp.code})")
                    }
                    val confirmBody = confirmResp.body ?: throw IllegalStateException("ফাইল পাওয়া যায়নি")
                    streamToFile(confirmBody.byteStream(), confirmBody.contentLength(), outFile, onProgress)
                }
            } else {
                streamToFile(body.byteStream(), body.contentLength(), outFile, onProgress)
            }
        }
    }

    private fun streamToFile(
        input: java.io.InputStream,
        totalBytes: Long,
        outFile: File,
        onProgress: (Int) -> Unit
    ) {
        input.use { stream ->
            FileOutputStream(outFile).use { fos ->
                val buffer = ByteArray(16 * 1024)
                var downloaded = 0L
                var read: Int
                var lastPct = -1
                while (stream.read(buffer).also { read = it } != -1) {
                    fos.write(buffer, 0, read)
                    downloaded += read
                    if (totalBytes > 0) {
                        val pct = ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                        if (pct != lastPct) {
                            lastPct = pct
                            onProgress(pct)
                        }
                    }
                }
                fos.flush()
                onProgress(100)
            }
        }
    }

    private fun extractDriveConfirmUrl(html: String, originalUrl: String): String? {
        val actionRegex = Regex("""action="(https://drive\.usercontent\.google\.com/download[^"]*)"[^>]*>""")
        val actionMatch = actionRegex.find(html)
        if (actionMatch != null) {
            val baseAction = actionMatch.groupValues[1].replace("&amp;", "&")
            val idMatch = Regex("""name="id"\s+value="([^"]+)"""").find(html)
            val confirmMatch = Regex("""name="confirm"\s+value="([^"]+)"""").find(html)
            val uuidMatch = Regex("""name="uuid"\s+value="([^"]+)"""").find(html)
            val params = mutableListOf<String>()
            idMatch?.let { params.add("id=${it.groupValues[1]}") }
            params.add("export=download")
            confirmMatch?.let { params.add("confirm=${it.groupValues[1]}") }
            uuidMatch?.let { params.add("uuid=${it.groupValues[1]}") }
            val sep = if (baseAction.contains("?")) "&" else "?"
            return "$baseAction$sep${params.joinToString("&")}"
        }
        val hrefMatch = Regex("""href="(/uc\?export=download[^"]+)"""").find(html)
        if (hrefMatch != null) {
            return "https://drive.google.com" + hrefMatch.groupValues[1].replace("&amp;", "&")
        }
        val fileId = extractGoogleDriveId(originalUrl)
        if (fileId != null) {
            return "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
        }
        return null
    }

    private fun isValidPdfFile(file: File): Boolean {
        if (!file.exists() || file.length() < 5) return false
        val header = ByteArray(5)
        file.inputStream().use { it.read(header) }
        return String(header, Charsets.US_ASCII).startsWith("%PDF")
    }

    private fun generateThumbnailAndCountPages(pdfFile: File, thumbFile: File): Int {
        var pageCount = 0
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                pageCount = renderer.pageCount
                if (pageCount > 0) {
                    renderer.openPage(0).use { page ->
                        val targetSize = 240
                        val aspect = page.width.toFloat() / page.height.coerceAtLeast(1)
                        val renderW: Int
                        val renderH: Int
                        if (aspect >= 1f) {
                            renderW = targetSize
                            renderH = (targetSize / aspect).toInt().coerceAtLeast(1)
                        } else {
                            renderH = targetSize
                            renderW = (targetSize * aspect).toInt().coerceAtLeast(1)
                        }
                        val bmp = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        canvas.drawColor(Color.WHITE)

                        val pageBmp = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                        pageBmp.eraseColor(Color.WHITE)
                        page.render(pageBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val left = (targetSize - renderW) / 2f
                        val top = (targetSize - renderH) / 2f
                        canvas.drawBitmap(pageBmp, left, top, null)
                        pageBmp.recycle()

                        FileOutputStream(thumbFile).use { fos ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 92, fos)
                        }
                        bmp.recycle()
                    }
                }
            }
        }
        return pageCount
    }

    private fun loadDownloadsStore(): DownloadedPdfStore {
        if (!downloadsMetaFile.exists()) return DownloadedPdfStore()
        return runCatching {
            downloadsAdapter.fromJson(downloadsMetaFile.readText()) ?: DownloadedPdfStore()
        }.getOrDefault(DownloadedPdfStore())
    }

    private fun saveDownloadsStore(store: DownloadedPdfStore) {
        downloadsMetaFile.writeText(downloadsAdapter.toJson(store))
    }

    companion object {
        @Volatile
        private var instance: GuideRepository? = null

        fun getInstance(context: Context): GuideRepository {
            return instance ?: synchronized(this) {
                instance ?: GuideRepository(context).also { instance = it }
            }
        }

        fun extractGoogleDriveId(url: String): String? {
            val regex1 = Regex("""/file/d/([a-zA-Z0-9_-]+)""")
            regex1.find(url)?.let { return it.groupValues[1] }
            val regex2 = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")
            regex2.find(url)?.let { return it.groupValues[1] }
            return null
        }

        fun resolveDirectDownloadUrl(url: String): String {
            val driveId = extractGoogleDriveId(url)
            return if (driveId != null) {
                "https://drive.google.com/uc?export=download&id=$driveId"
            } else {
                url.trim()
            }
        }

        fun urlToId(url: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(url.trim().toByteArray())
            return digest.take(12).joinToString("") { "%02x".format(it) }
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            return String.format(Locale.US, "%.2f MB", mb)
        }

        fun formatShortDate(millis: Long): String {
            val fmt = SimpleDateFormat("MMM dd", Locale.ENGLISH)
            return fmt.format(Date(millis))
        }
    }
}
