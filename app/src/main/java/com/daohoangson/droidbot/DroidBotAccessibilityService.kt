package com.daohoangson.droidbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class DroidBotAccessibilityService : AccessibilityService() {
    companion object {
        private val TAP_TIMEOUT = ViewConfiguration.getTapTimeout().toLong()
    }

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

        DroidBotLiveData.taps.observeForever { pair ->
            dispatchTap(pair.first, pair.second)
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
}
