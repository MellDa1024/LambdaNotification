package com.mellda

import com.lambda.client.plugin.api.Plugin
import com.mellda.modules.LambdaNotification

internal object NotificationPlugin : Plugin() {

    override fun onLoad() {
        modules.add(LambdaNotification)
    }
}