package com.daohoangson.droidbot

import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.daohoangson.droidbot.android.AccessibilityLifecycleService
import com.daohoangson.droidbot.bedrock.Client
import com.daohoangson.droidbot.bedrock.computer.ComputerUseEvent
import com.daohoangson.droidbot.bedrock.computer.ComputerUseInput
import com.daohoangson.droidbot.bedrock.message.Image
import com.daohoangson.droidbot.bedrock.message.Message
import com.daohoangson.droidbot.bedrock.message.Role
import com.daohoangson.droidbot.bedrock.message.Text
import com.daohoangson.droidbot.bedrock.message.ToolResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

class DroidBotAccessibilityService : AccessibilityLifecycleService() {
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

    private fun computerUse() {
        lifecycleScope.async {
            performGlobalAction(GLOBAL_ACTION_BACK)
            delay(100)
            performGlobalAction(GLOBAL_ACTION_BACK)
            delay(100)

            val prefValues = prefs.asFlow().first()
            val bedrock = Client(
                accessKeyId = prefValues.awsAccessKeyId ?: "",
                secretAccessKey = prefValues.awsSecretAccessKey ?: ""
            )

            val screenshot = takeScreenshotAndResize(jpeg = false)
            try {
                val displaySize = screenshot.await()!!.second
                var messages = listOf(
                    Message(
                        role = Role.USER,
                        content = listOf(Text("Search for near by pizza place and tell me the top 3 with best reviews."))
                    )
                )
                computerUseLoop(
                    bedrock = bedrock,
                    displaySize = displaySize,
                    messages = messages,
                )
            } catch (e: Exception) {
                Log.e("computerUse", "invokeComputerUse", e)
            }
        }
    }

    private suspend fun computerUseLoop(
        bedrock: Client,
        displaySize: Size,
        messages: List<Message>
    ) {
        var assistantMessages: List<Message> = listOf()
        var newMessages: List<Message> = listOf()

        bedrock.invokeComputerUse(
            displayHeightPx = displaySize.height,
            displayWidthPx = displaySize.width,
            messages = messages,
        ).collect { event ->
            when (event) {
                is ComputerUseEvent.Text -> {
                    Log.v("computerUseLoop", "event.text: ${event.text}")
                }

                is ComputerUseEvent.ComputerUse -> {
                    val input = event.input
                    when (input) {
                        is ComputerUseInput.Screenshot -> {
                            Log.v("computerUseLoop", "Taking screenshot for ${event.id}...")
                            takeScreenshotAndResize(jpeg = true).await()?.let { pair ->
                                val byteArray = pair.first!!.toByteArray()
                                val base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
                                newMessages += Message(
                                    role = Role.USER,
                                    content = listOf(
                                        ToolResult(
                                            toolUseId = event.id,
                                            content = listOf(
                                                Image()
                                            )
                                        )
                                    )
                                )
                            }
                        }

                        else -> {
                            Log.w("computerUseLoop", "event: $event")
                        }
                    }
                }

                is ComputerUseEvent.MessageStopEvent -> {
                    assistantMessages += event.message
                }
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


    private fun takeScreenshotAndResize(jpeg: Boolean): Deferred<Pair<ByteArrayOutputStream?, Size>?> {
        val deferred = CompletableDeferred<Pair<ByteArrayOutputStream?, Size>?>()

        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                try {
                    screenshot.hardwareBuffer.use { buffer ->
                        val bitmap =
                            Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace) ?: run {
                                deferred.completeExceptionally(IllegalStateException("Could not wrap hardware buffer"))
                                return
                            }

                        try {
                            val resizedBitmap = bitmapResizeIfNeeded(bitmap)
                            try {
                                val size = Size(resizedBitmap.width, resizedBitmap.height)

                                if (jpeg) {
                                    val outputStream = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                                    deferred.complete(Pair(outputStream, size))
                                } else {
                                    deferred.complete(Pair(null, size))
                                }
                            } finally {
                                if (resizedBitmap != bitmap) resizedBitmap.recycle()
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    }
                } catch (e: Exception) {
                    deferred.completeExceptionally(e)
                }
            }

            override fun onFailure(errorCode: Int) {
                deferred.completeExceptionally(RuntimeException("errorCode: $errorCode"))
            }
        })

        return deferred
    }
}
