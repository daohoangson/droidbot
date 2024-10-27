package com.daohoangson.droidbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.roundToInt

class DroidBotAccessibilityService : AccessibilityService() {
    companion object {
        private val TAP_TIMEOUT = ViewConfiguration.getTapTimeout().toLong()
    }

    private val vm = DroidBotViewModel

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.also { maybeFocusedNode ->
                Log.d("DroidBot", "maybeFocusedNode.className: ${maybeFocusedNode.className}")
                val focusedNode = findFocusedNodeRecursively(maybeFocusedNode)
                Log.d("DroidBot", "focusedNode.className: ${focusedNode?.className}")
                Log.d("DroidBot", "focusedNode.text: ${focusedNode?.text}")
            }
        }
    }

    override fun onInterrupt() {
        // TODO
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // demo
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_BACK)

        vm.screenshots.observeForever {
            takeScreenshotAndResize()
        }

        vm.taps.observeForever { pair ->
            dispatchTap(pair.first, pair.second)
        }
    }

    private fun bitmapCompress(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "screenshot.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES
            )
        }
        contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )?.also { uri ->
            contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            }
        }
    }

    private fun bitmapResizeIfNeeded(bitmap: Bitmap): Bitmap {
        val preferredWidth = 1280
        val preferredHeight = 800
        val ratio = bitmap.width / bitmap.height.toFloat()

        var resizedWidth = preferredWidth
        var resizedHeight = preferredHeight
        if (ratio > preferredWidth / preferredHeight) {
            resizedHeight = (preferredWidth / ratio).roundToInt()
        } else {
            resizedWidth = (preferredHeight * ratio).roundToInt()
        }

        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true)
    }

    private fun dispatchTap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, TAP_TIMEOUT))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun findFocusedNodeRecursively(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused) {
            return node
        }

        for (i in 0 until node.childCount) {
            val focusedNode = findFocusedNodeRecursively(node.getChild(i))
            if (focusedNode != null) {
                return focusedNode
            }
        }

        return null
    }


    private fun takeScreenshotAndResize() {
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    screenshot.hardwareBuffer.use { buffer ->
                        Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                            ?.also { bitmap ->
                                try {
                                    val resizedBitmap = bitmapResizeIfNeeded(bitmap)
                                    try {
                                        bitmapCompress(resizedBitmap)
                                    } finally {
                                        if (resizedBitmap != bitmap) resizedBitmap.recycle()
                                    }
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    // TODO
                }
            }
        )
    }
}
