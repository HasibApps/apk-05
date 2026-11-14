package com.example.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.PdfViewerActivity
import com.example.data.DownloadCancelledException
import com.example.data.DownloadedPdfMeta
import com.example.data.GuideRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

/**
 * Foreground Service: অ্যাপ থেকে বের হয়ে গেলেও PDF ডাউনলোড চলতে থাকে এবং
 * নোটিফিকেশনে প্রোগ্রেস (MB, MB/s) দেখায়। একসাথে সর্বোচ্চ ১০টি PDF সমান্তরালে নামে,
 * বাকিগুলো লাইনে অপেক্ষা করে।
 */
class PdfDownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(MAX_PARALLEL)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val lock = Any()

    @Volatile
    private var lastStartId = 0
    private var tickerJob: Job? = null

    private lateinit var repository: GuideRepository
    private lateinit var manager: NotificationManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = GuideRepository.getInstance(this)
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        val action = intent?.action
        val url = intent?.getStringExtra(EXTRA_URL).orEmpty()

        when (action) {
            ACTION_CANCEL -> {
                if (url.isNotBlank()) cancelDownload(url)
                maybeStop()
            }
            ACTION_CANCEL_ALL -> {
                DownloadCenter.activeStates().forEach { cancelDownload(it.url) }
                maybeStop()
            }
            else -> {
                synchronized(lock) {
                    // startForegroundService() এর ৫ সেকেন্ডের মধ্যে এটা অবশ্যই কল করতে হয়
                    promoteToForeground()
                    if (url.isNotBlank()) {
                        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "PDF" }
                        val subject = intent?.getStringExtra(EXTRA_SUBJECT).orEmpty()
                        enqueueLocked(url, title, subject)
                    }
                }
                ensureTicker()
                maybeStop()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------------
    // Queue
    // ---------------------------------------------------------------------------------------

    private fun enqueueLocked(url: String, title: String, subject: String) {
        if (jobs.containsKey(url)) return
        val already = repository.findDownloadedByUrl(url)
        if (already != null) {
            DownloadCenter.update(
                DownloadState(url, title, subject, DownloadStatus.DONE, already.fileSizeBytes, already.fileSizeBytes)
            )
            return
        }
        DownloadCenter.clearCancelRequest(url)
        DownloadCenter.update(DownloadState(url, title, subject, DownloadStatus.QUEUED))
        jobs[url] = scope.launch { runDownload(url, title, subject) }
    }

    private fun cancelDownload(url: String) {
        DownloadCenter.requestCancel(url)
        repository.cancelCalls(url)
        jobs[url]?.cancel()
    }

    private suspend fun runDownload(url: String, title: String, subject: String) {
        var lastTick = 0L
        var lastBytes = 0L
        var speedEma = 0.0
        try {
            semaphore.withPermit {
                if (DownloadCenter.isCancelRequested(url)) throw DownloadCancelledException()
                DownloadCenter.update(DownloadState(url, title, subject, DownloadStatus.DOWNLOADING))
                lastTick = System.nanoTime()

                val result = repository.downloadPdfWithStats(
                    url = url,
                    title = title,
                    subject = subject,
                    isCancelled = { DownloadCenter.isCancelRequested(url) }
                ) { downloaded, total ->
                    val now = System.nanoTime()
                    val dt = (now - lastTick) / 1_000_000_000.0
                    if (dt >= 0.5) {
                        val instant = (downloaded - lastBytes) / dt
                        speedEma = if (speedEma <= 0.0) instant else speedEma * 0.6 + instant * 0.4
                        lastTick = now
                        lastBytes = downloaded
                        DownloadCenter.update(
                            DownloadState(
                                url, title, subject, DownloadStatus.DOWNLOADING,
                                downloaded, total, speedEma.toLong()
                            )
                        )
                    }
                }

                val meta = result.getOrNull()
                if (meta != null) {
                    DownloadCenter.update(
                        DownloadState(url, title, subject, DownloadStatus.DONE, meta.fileSizeBytes, meta.fileSizeBytes)
                    )
                    postDoneNotification(url, meta)
                } else {
                    val error = result.exceptionOrNull()
                    if (error is DownloadCancelledException || DownloadCenter.isCancelRequested(url)) {
                        DownloadCenter.update(DownloadState(url, title, subject, DownloadStatus.CANCELLED))
                        manager.cancel(fileNotificationId(url))
                    } else {
                        val msg = friendlyError(error)
                        DownloadCenter.update(
                            DownloadState(url, title, subject, DownloadStatus.FAILED, error = msg)
                        )
                        postFailedNotification(url, title, msg)
                    }
                }
            }
        } catch (e: CancellationException) {
            DownloadCenter.update(DownloadState(url, title, subject, DownloadStatus.CANCELLED))
            manager.cancel(fileNotificationId(url))
            throw e
        } catch (e: DownloadCancelledException) {
            DownloadCenter.update(DownloadState(url, title, subject, DownloadStatus.CANCELLED))
            manager.cancel(fileNotificationId(url))
        } catch (e: Throwable) {
            val msg = friendlyError(e)
            DownloadCenter.update(DownloadState(url, title, subject, DownloadStatus.FAILED, error = msg))
            postFailedNotification(url, title, msg)
        } finally {
            DownloadCenter.clearCancelRequest(url)
            synchronized(lock) { jobs.remove(url) }
            maybeStop()
        }
    }

    private fun friendlyError(t: Throwable?): String = when (t) {
        null -> "ডাউনলোড করা যায়নি"
        is UnknownHostException -> "ইন্টারনেট সংযোগ পাওয়া যায়নি"
        is SocketTimeoutException -> "সংযোগের সময় শেষ হয়েছে, আবার চেষ্টা করুন"
        else -> t.localizedMessage ?: "ডাউনলোড করা যায়নি"
    }

    private fun maybeStop() {
        synchronized(lock) {
            if (jobs.isEmpty()) {
                tickerJob?.cancel()
                tickerJob = null
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf(lastStartId)
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Notifications
    // ---------------------------------------------------------------------------------------

    private fun ensureTicker() {
        synchronized(lock) {
            if (tickerJob?.isActive == true) return
            tickerJob = scope.launch {
                while (isActive) {
                    val active = DownloadCenter.activeStates()
                    if (active.isEmpty()) break
                    refreshNotifications(active)
                    delay(1000)
                }
            }
        }
    }

    private fun refreshNotifications(active: List<DownloadState>) {
        if (active.size == 1) {
            val s = active[0]
            manager.cancel(fileNotificationId(s.url))
            manager.notify(FOREGROUND_ID, buildFileNotification(s))
        } else {
            manager.notify(FOREGROUND_ID, buildSummaryNotification(active))
            active.forEach { manager.notify(fileNotificationId(it.url), buildFileNotification(it)) }
        }
    }

    private fun promoteToForeground() {
        val notification = buildSummaryNotification(DownloadCenter.activeStates())
        try {
            ServiceCompat.startForeground(
                this,
                FOREGROUND_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } catch (e: Exception) {
            // Foreground এ যেতে না পারলেও ডাউনলোড চেষ্টা চলবে
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PDF ডাউনলোড",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "PDF ডাউনলোডের অগ্রগতি দেখায়"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun cancelPendingIntent(url: String?): PendingIntent {
        val intent = Intent(this, PdfDownloadService::class.java).apply {
            if (url == null) {
                action = ACTION_CANCEL_ALL
            } else {
                action = ACTION_CANCEL
                putExtra(EXTRA_URL, url)
            }
        }
        val requestCode = if (url == null) 1 else fileNotificationId(url)
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun buildFileNotification(s: DownloadState): Notification {
        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(s.title)
            .setContentText(DownloadFormat.progressLine(s))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent())
            .addAction(0, "বাতিল", cancelPendingIntent(s.url))
        if (s.status == DownloadStatus.DOWNLOADING && s.totalBytes > 0) {
            b.setProgress(100, s.percent, false)
        } else {
            b.setProgress(0, 0, true)
        }
        return b.build()
    }

    private fun buildSummaryNotification(active: List<DownloadState>): Notification {
        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent())

        if (active.isEmpty()) {
            b.setContentTitle("ডাউনলোড শুরু হচ্ছে…").setProgress(0, 0, true)
            return b.build()
        }

        val totalSpeed = active.sumOf { it.speedBytesPerSec }
        val downloaded = active.sumOf { it.downloadedBytes }
        val allKnown = active.all { it.totalBytes > 0 }
        val total = if (allKnown) active.sumOf { it.totalBytes } else -1L

        b.setContentTitle("${active.size}টি PDF ডাউনলোড হচ্ছে")
        val sizeText = if (total > 0) {
            "${GuideRepository.formatFileSize(downloaded)} / ${GuideRepository.formatFileSize(total)}"
        } else {
            GuideRepository.formatFileSize(downloaded)
        }
        val speedText = if (totalSpeed > 0) " • ${DownloadFormat.speed(totalSpeed)}" else ""
        b.setContentText(sizeText + speedText)

        if (total > 0) {
            b.setProgress(100, ((downloaded * 100) / total).toInt().coerceIn(0, 100), false)
        } else {
            b.setProgress(0, 0, true)
        }
        b.addAction(0, "সব বাতিল", cancelPendingIntent(null))
        return b.build()
    }

    private fun postDoneNotification(url: String, meta: DownloadedPdfMeta) {
        val open = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_TITLE, meta.title)
            putExtra(PdfViewerActivity.EXTRA_SUBJECT, meta.subject)
            putExtra(PdfViewerActivity.EXTRA_LOCAL_PATH, meta.pdfPath)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pi = PendingIntent.getActivity(
            this, fileNotificationId(url), open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(meta.title)
            .setContentText("ডাউনলোড সম্পন্ন • ${GuideRepository.formatFileSize(meta.fileSizeBytes)} • খুলতে ট্যাপ করুন")
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build()
        manager.notify(fileNotificationId(url), n)
    }

    private fun postFailedNotification(url: String, title: String, message: String) {
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("ডাউনলোড ব্যর্থ: $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent())
            .build()
        manager.notify(fileNotificationId(url), n)
    }

    companion object {
        const val MAX_PARALLEL = 10

        private const val CHANNEL_ID = "pdf_downloads"
        private const val FOREGROUND_ID = 1001

        const val ACTION_START = "com.example.download.START"
        const val ACTION_CANCEL = "com.example.download.CANCEL"
        const val ACTION_CANCEL_ALL = "com.example.download.CANCEL_ALL"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBJECT = "extra_subject"

        private fun fileNotificationId(url: String): Int = (url.hashCode() and 0x7fffffff) % 100000 + 2000

        /** ডাউনলোড লাইনে দেয় (সর্বোচ্চ ১০টা একসাথে চলে, বাকিগুলো অপেক্ষায়)। */
        fun enqueue(context: Context, url: String, title: String, subject: String) {
            if (url.isBlank() || DownloadCenter.isActive(url)) return
            DownloadCenter.update(DownloadState(url, title, subject, DownloadStatus.QUEUED))
            val intent = Intent(context, PdfDownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SUBJECT, subject)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context, url: String) {
            val intent = Intent(context, PdfDownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_URL, url)
            }
            context.startService(intent)
        }
    }
}
