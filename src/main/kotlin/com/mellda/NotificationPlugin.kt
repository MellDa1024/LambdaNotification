package com.mellda

import com.lambda.client.plugin.api.Plugin
import com.mellda.modules.LambdaNotification

internal object NotificationPlugin : Plugin() {

    override fun onLoad() {
        // Load any modules, commands, or HUD elements here
        modules.add(LambdaNotification)
    }

    override fun onUnload() {
        // Here you can unregister threads etc...
    }
}