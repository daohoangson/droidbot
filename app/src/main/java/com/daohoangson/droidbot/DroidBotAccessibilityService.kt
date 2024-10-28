package com.daohoangson.droidbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Path
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.asFlow
import com.daohoangson.droidbot.bedrock.Client
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

class DroidBotAccessibilityService : AccessibilityService() {
    companion object {
        private val TAP_TIMEOUT = ViewConfiguration.getTapTimeout().toLong()
    }

    private val prefs: SharedPreferencesLiveData by lazy { SharedPreferencesLiveData(this) }
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
        computerUse()

        vm.screenshots.observeForever {
            takeScreenshotAndResize()
        }

        vm.taps.observeForever { pair ->
            dispatchTap(pair.first, pair.second)
        }
    }

    private fun bitmapCompress(bitmap: Bitmap): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "screenshot.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES
            )
        }

        return contentResolver.insert(
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun computerUse() {
        GlobalScope.async {
            performGlobalAction(GLOBAL_ACTION_BACK)
            delay(100)
            performGlobalAction(GLOBAL_ACTION_BACK)
            delay(100)

            val prefValues = prefs.asFlow().first()
            val bedrock = Client(
                accessKeyId = prefValues.awsAccessKeyId ?: "",
                secretAccessKey = prefValues.awsSecretAccessKey ?: ""
            )

            val screenshot = takeScreenshotAndResize()
            val pair = screenshot.await()
            val size = pair!!.second
            try {
                bedrock.invokeComputerUse(
                    displayHeightPx = size.height,
                    displayWidthPx = size.width,
                ).collect { computerUseEvent ->
                    Log.d("DroidBot", "computerUseEvent: $computerUseEvent")
                }
            } catch (e: Exception) {
                Log.e("DroidBot", "invokeComputerUse", e)
            }
        }
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


    private fun takeScreenshotAndResize(): Deferred<Pair<Uri, Size>?> {
        val deferred = CompletableDeferred<Pair<Uri, Size>?>()

        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                screenshot.hardwareBuffer.use { buffer ->
                    val bitmap = Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace) ?: run {
                        deferred.completeExceptionally(IllegalStateException("Could not wrap hardware buffer"))
                        return
                    }

                    try {
                        val resizedBitmap = bitmapResizeIfNeeded(bitmap)
                        try {
                            val uri = bitmapCompress(resizedBitmap) ?: run {
                                deferred.completeExceptionally(IllegalStateException("Could not compress bitmap"))
                                return
                            }
                            deferred.complete(
                                Pair(
                                    uri, Size(resizedBitmap.width, resizedBitmap.height)
                                )
                            )
                        } catch (e: Exception) {
                            deferred.completeExceptionally(e)
                            if (resizedBitmap != bitmap) resizedBitmap.recycle()
                        }
                    } catch (e: Exception) {
                        deferred.completeExceptionally(e)
                        bitmap.recycle()
                    }
                }
            }

            override fun onFailure(errorCode: Int) {
                // TODO
            }
        })

        return deferred
    }
}
