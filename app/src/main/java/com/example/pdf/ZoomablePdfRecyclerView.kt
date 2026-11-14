package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min

class ZoomablePdfRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    var scaleFactor = 1.0f
        private set
    private val minScale = 1.0f
    private val maxScale = 4.5f

    private var transX = 0f
    private var transY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isScaling = false
    private var lastPointerCount = 0

    var onZoomChanged: ((Float) -> Unit)? = null
    var onSingleTap: (() -> Unit)? = null

    // শুধু ২ আঙুলের পিঞ্চেই জুম হবে। ডাবল-ট্যাপ, ট্যাপ-হোল্ড-ড্র্যাগ (quick scale) ও স্টাইলাস জুম বন্ধ।
    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                if (lastPointerCount < 2) return false
                isScaling = true
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val prevScale = scaleFactor
                val newScale = (scaleFactor * detector.scaleFactor).coerceIn(minScale, maxScale)
                if (newScale != prevScale) {
                    val focusX = detector.focusX
                    val focusY = detector.focusY
                    val ratio = newScale / prevScale
                    transX = focusX - (focusX - transX) * ratio
                    transY = focusY - (focusY - transY) * ratio
                    scaleFactor = newScale
                    clampTranslation()
                    onZoomChanged?.invoke(scaleFactor)
                    invalidate()
                }
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isScaling = false
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onSingleTap?.invoke()
                return false
            }
        }
    ).apply { setIsLongpressEnabled(false) }

    init {
        scaleDetector.isQuickScaleEnabled = false
        scaleDetector.isStylusScaleEnabled = false
    }

    private var lastFedTime = -1L
    private var lastFedAction = -1

    private fun feedDetectors(e: MotionEvent) {
        // onInterceptTouchEvent ও onTouchEvent একই event দুইবার পেতে পারে — একবারই প্রসেস করা হবে
        if (e.eventTime == lastFedTime && e.action == lastFedAction) return
        lastFedTime = e.eventTime
        lastFedAction = e.action
        lastPointerCount = e.pointerCount
        // শুধু ২+ আঙুল থাকলে স্কেল ডিটেক্টর চলবে; এক আঙুলে কখনোই জুম নয়
        if (e.pointerCount >= 2 || isScaling) scaleDetector.onTouchEvent(e)
        gestureDetector.onTouchEvent(e)
    }

    fun resetZoom() {
        scaleFactor = 1.0f
        transX = 0f
        transY = 0f
        onZoomChanged?.invoke(scaleFactor)
        invalidate()
    }

    private fun clampTranslation() {
        if (scaleFactor <= 1.0f) {
            transX = 0f
            transY = 0f
            return
        }
        val maxDx = width * (scaleFactor - 1f)
        val maxDy = height * (scaleFactor - 1f)
        transX = max(-maxDx, min(0f, transX))
        transY = max(-maxDy, min(0f, transY))
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        feedDetectors(e)
        if (e.pointerCount >= 2 || isScaling || scaleFactor > 1.02f) {
            return true
        }
        return super.onInterceptTouchEvent(e)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        feedDetectors(e)

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = e.x
                lastTouchY = e.y
                activePointerId = e.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isScaling && e.pointerCount == 1 && scaleFactor > 1.02f) {
                    val idx = e.findPointerIndex(activePointerId)
                    if (idx != -1) {
                        val x = e.getX(idx)
                        val y = e.getY(idx)
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        transX += dx

                        // Allow vertical pan within zoomed viewport, and pass leftover dy to RecyclerView scroll
                        val maxDy = height * (scaleFactor - 1f)
                        val prevTransY = transY
                        transY = max(-maxDy, min(0f, transY + dy))
                        val consumedY = transY - prevTransY
                        val unconsumedY = dy - consumedY
                        if (kotlin.math.abs(unconsumedY) > 0.5f) {
                            scrollBy(0, (-unconsumedY / scaleFactor).toInt())
                        }

                        clampTranslation()
                        lastTouchX = x
                        lastTouchY = y
                        invalidate()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = e.actionIndex
                val pointerId = e.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = e.getX(newIndex)
                    lastTouchY = e.getY(newIndex)
                    activePointerId = e.getPointerId(newIndex)
                }
            }
        }

        if (e.pointerCount >= 2 || isScaling || scaleFactor > 1.02f) {
            return true
        }
        return super.onTouchEvent(e)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (scaleFactor > 1.0f) {
            canvas.save()
            canvas.translate(transX, transY)
            canvas.scale(scaleFactor, scaleFactor)
            super.dispatchDraw(canvas)
            canvas.restore()
        } else {
            super.dispatchDraw(canvas)
        }
    }
}
