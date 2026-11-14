package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** ইউজার নিজে ডাউনলোড বাতিল করলে এই exception ছোড়া হয়। */
class DownloadCancelledException : IOException("ডাউনলোড বাতিল করা হয়েছে")

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

    private val storeLock = Any()

    /** Google Drive এর confirm-পেজ কুকি ধরে রাখার জন্য ছোট in-memory CookieJar। */
    private class MemoryCookieJar : CookieJar {
        private val store = ConcurrentHashMap<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val old = store[url.host].orEmpty().filter { o -> cookies.none { it.name == o.name } }
            store[url.host] = old + cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            return store[url.host].orEmpty().filter { it.expiresAt > now && it.matches(url) }
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .cookieJar(MemoryCookieJar())
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val activeCalls = ConcurrentHashMap<String, CopyOnWriteArrayList<Call>>()

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

    fun getDownloadedPdfs(): List<DownloadedPdfMeta> = synchronized(storeLock) {
        val store = loadDownloadsStore()
        val valid = store.items.filter { File(it.pdfPath).exists() }
        if (valid.size != store.items.size) {
            saveDownloadsStore(DownloadedPdfStore(valid.toMutableList()))
        }
        valid.sortedByDescending { it.downloadedAtMillis }
    }

    fun findDownloadedByUrl(url: String): DownloadedPdfMeta? {
        val id = urlToId(url)
        return getDownloadedPdfs().firstOrNull { it.id == id }
    }

    fun deleteDownloadedPdf(id: String) {
        synchronized(storeLock) {
            val store = loadDownloadsStore()
            val target = store.items.firstOrNull { it.id == id }
            if (target != null) {
                File(target.pdfPath).delete()
                File(target.thumbnailPath).delete()
                store.items.removeAll { it.id == id }
                saveDownloadsStore(store)
            }
        }
    }

    /** চলমান নেটওয়ার্ক কল (যেটা এই URL এর জন্য) তাৎক্ষণিক বন্ধ করে। */
    fun cancelCalls(url: String) {
        activeCalls.remove(url)?.forEach { it.cancel() }
    }

    /**
     * PDF ডাউনলোড করে অফলাইনে সেভ করে এবং প্রথম পৃষ্ঠার থাম্বনেইল বানায়।
     * [onProgress] এ (এ পর্যন্ত ডাউনলোড হওয়া বাইট, মোট বাইট) আসে; মোট জানা না থাকলে -1।
     */
    suspend fun downloadPdfWithStats(
        url: String,
        title: String,
        subject: String,
        isCancelled: () -> Boolean = { false },
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Result<DownloadedPdfMeta> = withContext(Dispatchers.IO) {
        val id = urlToId(url)
        val targetPdf = File(pdfDir, "$id.pdf")
        val targetThumb = File(thumbDir, "$id.jpg")
        val tempFile = File(pdfDir, "$id.${System.nanoTime()}.tmp")
        val job: Job? = coroutineContext[Job]
        val shouldStop: () -> Boolean = { isCancelled() || (job != null && !job.isActive) }

        try {
            fetchPdfToFile(url, url, tempFile, onProgress, shouldStop)

            if (targetPdf.exists()) targetPdf.delete()
            if (!tempFile.renameTo(targetPdf)) {
                tempFile.copyTo(targetPdf, overwrite = true)
                tempFile.delete()
            }

            val pageCount = runCatching { generateThumbnailAndCountPages(targetPdf, targetThumb) }
                .getOrDefault(0)

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

            synchronized(storeLock) {
                val store = loadDownloadsStore()
                store.items.removeAll { it.id == id }
                store.items.add(0, meta)
                saveDownloadsStore(store)
            }

            Result.success(meta)
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Throwable) {
            tempFile.delete()
            Result.failure(e)
        } finally {
            activeCalls.remove(url)
        }
    }

    /** পুরনো সিগনেচার (শুধু শতকরা হার) — PdfViewer এর সাথে সামঞ্জস্যের জন্য। */
    suspend fun downloadPdfForOffline(
        url: String,
        title: String,
        subject: String,
        onProgress: (Int) -> Unit
    ): Result<DownloadedPdfMeta> = downloadPdfWithStats(url, title, subject) { d, t ->
        if (t > 0) onProgress(((d * 100) / t).toInt().coerceIn(0, 100))
    }

    /** থাম্বনেইল না থাকলে (যেমন আগের ভার্সনে ডাউনলোড করা PDF) নতুন করে বানায়। */
    suspend fun ensureThumbnail(meta: DownloadedPdfMeta): Boolean = withContext(Dispatchers.IO) {
        val thumb = File(meta.thumbnailPath)
        if (thumb.exists() && thumb.length() > 0) return@withContext true
        val pdf = File(meta.pdfPath)
        if (!pdf.exists()) return@withContext false
        runCatching {
            generateThumbnailAndCountPages(pdf, thumb)
            thumb.exists() && thumb.length() > 0
        }.getOrDefault(false)
    }

    // ---------------------------------------------------------------------------------------
    // Download internals
    // ---------------------------------------------------------------------------------------

    private fun trackedCall(key: String, request: Request): Call {
        val call = httpClient.newCall(request)
        activeCalls.getOrPut(key) { CopyOnWriteArrayList() }.add(call)
        return call
    }

    private fun fetchPdfToFile(
        key: String,
        sourceUrl: String,
        outFile: File,
        onProgress: (Long, Long) -> Unit,
        shouldStop: () -> Boolean
    ) {
        val driveId = extractGoogleDriveId(sourceUrl)
        val candidates = if (driveId != null) {
            listOf(
                "https://drive.usercontent.google.com/download?id=$driveId&export=download&confirm=t&authuser=0",
                "https://drive.google.com/uc?export=download&id=$driveId"
            )
        } else {
            listOf(sourceUrl.trim())
        }

        var lastError: Throwable? = null
        for (candidate in candidates) {
            for (attempt in 1..2) {
                try {
                    if (shouldStop()) throw DownloadCancelledException()
                    outFile.delete()
                    downloadFromUrl(key, candidate, outFile, onProgress, shouldStop, 0)
                    if (isValidPdfFile(outFile)) return
                    lastError = IllegalStateException(NOT_PDF_MESSAGE)
                    break
                } catch (e: DownloadCancelledException) {
                    throw e
                } catch (e: IOException) {
                    if (shouldStop()) throw DownloadCancelledException()
                    lastError = e
                } catch (e: IllegalStateException) {
                    lastError = e
                    break
                }
            }
        }
        throw lastError ?: IllegalStateException(NOT_PDF_MESSAGE)
    }

    private fun downloadFromUrl(
        key: String,
        url: String,
        outFile: File,
        onProgress: (Long, Long) -> Unit,
        shouldStop: () -> Boolean,
        hop: Int
    ) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            // gzip বন্ধ রাখলে সার্ভারের আসল Content-Length (ফাইলের সাইজ) পাওয়া যায়
            .header("Accept-Encoding", "identity")
            .build()

        trackedCall(key, request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("ডাউনলোড ব্যর্থ হয়েছে (Code: ${response.code})")
            }
            val body = response.body ?: throw IllegalStateException("ফাইল খালি রয়েছে")
            val contentType = body.contentType()?.toString()?.lowercase(Locale.ROOT).orEmpty()

            if (contentType.contains("text/html")) {
                if (hop >= 2 || extractGoogleDriveId(url) == null) {
                    throw IllegalStateException(
                        if (extractGoogleDriveId(url) == null) NOT_PDF_MESSAGE else DRIVE_FAIL_MESSAGE
                    )
                }
                val html = body.string()
                checkDriveHtmlErrors(html)
                val confirmUrl = extractDriveConfirmUrl(html, url)
                    ?: throw IllegalStateException(DRIVE_FAIL_MESSAGE)
                downloadFromUrl(key, confirmUrl, outFile, onProgress, shouldStop, hop + 1)
            } else {
                streamToFile(body.byteStream(), body.contentLength(), outFile, onProgress, shouldStop)
            }
        }
    }

    private fun checkDriveHtmlErrors(html: String) {
        if (html.contains("Quota exceeded", ignoreCase = true) ||
            html.contains("Too many users have viewed or downloaded", ignoreCase = true)
        ) {
            throw IllegalStateException("Google Drive এর ডাউনলোড সীমা শেষ হয়েছে। কিছুক্ষণ পরে আবার চেষ্টা করুন।")
        }
        if (html.contains("accounts.google.com/ServiceLogin", ignoreCase = true) &&
            !html.contains("download-form", ignoreCase = true)
        ) {
            throw IllegalStateException("ফাইলটি পাবলিক নয়। Drive এ “Anyone with the link” সেট করুন।")
        }
    }

    private fun streamToFile(
        input: java.io.InputStream,
        totalBytes: Long,
        outFile: File,
        onProgress: (Long, Long) -> Unit,
        shouldStop: () -> Boolean
    ) {
        input.use { stream ->
            FileOutputStream(outFile).use { fos ->
                val buffer = ByteArray(64 * 1024)
                var downloaded = 0L
                while (true) {
                    if (shouldStop()) throw DownloadCancelledException()
                    val read = stream.read(buffer)
                    if (read == -1) break
                    fos.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded, totalBytes)
                }
                fos.flush()
                if (totalBytes > 0 && downloaded < totalBytes) {
                    throw IOException("ডাউনলোড অসম্পূর্ণ থেকে গেছে")
                }
                onProgress(downloaded, if (totalBytes > 0) totalBytes else downloaded)
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
            params.add("confirm=${confirmMatch?.groupValues?.get(1) ?: "t"}")
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
            return "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t&authuser=0"
        }
        return null
    }

    private fun isValidPdfFile(file: File): Boolean {
        if (!file.exists() || file.length() < 5) return false
        val header = ByteArray(5)
        file.inputStream().use { it.read(header) }
        return String(header, Charsets.US_ASCII).startsWith("%PDF")
    }

    /**
     * প্রথম পৃষ্ঠা থেকে বর্গাকার থাম্বনেইল বানায় (ফাইল-ম্যানেজারের মতো: ছবি পুরো ঘর ভরে থাকে)।
     * পোর্ট্রেট পৃষ্ঠার ওপরের অংশ, ল্যান্ডস্কেপ পৃষ্ঠার মাঝের অংশ দেখানো হয়।
     */
    private fun generateThumbnailAndCountPages(pdfFile: File, thumbFile: File): Int {
        var pageCount = 0
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                pageCount = renderer.pageCount
                if (pageCount > 0) {
                    renderer.openPage(0).use { page ->
                        val target = THUMB_SIZE
                        val shortSide = minOf(page.width, page.height).coerceAtLeast(1)
                        val scale = target.toFloat() / shortSide
                        val renderW = (page.width * scale).toInt().coerceAtLeast(1)
                        val renderH = (page.height * scale).toInt().coerceAtLeast(1)

                        val pageBmp = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                        pageBmp.eraseColor(Color.WHITE)
                        page.render(pageBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val out = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(out)
                        canvas.drawColor(Color.WHITE)
                        val left = -((renderW - target) / 2f)
                        val top = if (renderH > renderW) 0f else -((renderH - target) / 2f)
                        canvas.drawBitmap(pageBmp, left, top, null)
                        pageBmp.recycle()

                        thumbFile.parentFile?.mkdirs()
                        val format = if (thumbFile.name.endsWith(".png", ignoreCase = true)) {
                            Bitmap.CompressFormat.PNG
                        } else {
                            Bitmap.CompressFormat.JPEG
                        }
                        FileOutputStream(thumbFile).use { fos ->
                            out.compress(format, 90, fos)
                        }
                        out.recycle()
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
        private const val THUMB_SIZE = 320
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
        private const val NOT_PDF_MESSAGE =
            "এই লিংকটি সরাসরি PDF ফাইল নয়। সঠিক Google Drive বা PDF লিংক ব্যবহার করুন।"
        private const val DRIVE_FAIL_MESSAGE =
            "Google Drive থেকে PDF ডাউনলোড করা যায়নি (ফাইলটি পাবলিক কিনা পরীক্ষা করুন)।"

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
