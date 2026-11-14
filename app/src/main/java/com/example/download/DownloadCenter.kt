package com.example.download

import android.os.Handler
import android.os.Looper
import com.example.data.GuideRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

enum class DownloadStatus { QUEUED, DOWNLOADING, DONE, FAILED, CANCELLED }

data class DownloadState(
    val url: String,
    val title: String,
    val subject: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val speedBytesPerSec: Long = 0L,
    val error: String? = null
) {
    val isActive: Boolean
        get() = status == DownloadStatus.QUEUED || status == DownloadStatus.DOWNLOADING

    val percent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}

/**
 * সব ডাউনলোডের অবস্থা এক জায়গায়। Service লেখে, Activity গুলো শোনে (main thread এ)।
 */
object DownloadCenter {

    fun interface Listener {
        fun onDownloadChanged(state: DownloadState)
    }

    private val states = ConcurrentHashMap<String, DownloadState>()
    private val cancelRequested = ConcurrentHashMap.newKeySet<String>()
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun get(url: String): DownloadState? = states[url]

    fun isActive(url: String): Boolean = states[url]?.isActive == true

    fun activeStates(): List<DownloadState> = states.values.filter { it.isActive }

    fun update(state: DownloadState) {
        if (state.isActive) {
            states[state.url] = state
        } else {
            states.remove(state.url)
        }
        mainHandler.post {
            for (l in listeners) l.onDownloadChanged(state)
        }
    }

    fun requestCancel(url: String) {
        cancelRequested.add(url)
    }

    fun isCancelRequested(url: String): Boolean = cancelRequested.contains(url)

    fun clearCancelRequest(url: String) {
        cancelRequested.remove(url)
    }
}

/** সাইজ ও গতি দেখানোর ফরম্যাট (অ্যাপ ও নোটিফিকেশন দুই জায়গাতেই এক)। */
object DownloadFormat {

    fun speed(bytesPerSec: Long): String {
        val mbps = bytesPerSec / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.2f MB/s", mbps)
    }

    fun progressLine(state: DownloadState): String {
        if (state.status == DownloadStatus.QUEUED) return "অপেক্ষায় আছে…"
        if (state.downloadedBytes <= 0L) return "সংযোগ হচ্ছে…"
        val done = GuideRepository.formatFileSize(state.downloadedBytes)
        val speedText = if (state.speedBytesPerSec > 0) " • ${speed(state.speedBytesPerSec)}" else ""
        return if (state.totalBytes > 0) {
            val total = GuideRepository.formatFileSize(state.totalBytes)
            "${state.percent}% • $done / $total$speedText"
        } else {
            "$done ডাউনলোড হয়েছে$speedText"
        }
    }
}
