package com.daohoangson.droidbot

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class DroidBotAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // TODO
    }

    override fun onInterrupt() {
        // TODO
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // demo
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_BACK)
    }
}
