package com.mellda.modules

import com.lambda.client.event.listener.listener
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.text.MessageSendHelper
import com.mellda.NotificationPlugin
import net.minecraftforge.client.event.ClientChatReceivedEvent
import org.lwjgl.opengl.Display
import java.awt.*

/**
 * This is a module. First set properties then settings then add listener.
 * **/
internal object LambdaNotification : PluginModule(
    name = "Notification",
    category = Category.MISC,
    description = "Notification Module",
    pluginMain = NotificationPlugin
) {
    private lateinit var tray : SystemTray
    private lateinit var image : Image
    private lateinit var trayicon : TrayIcon
    private var istrayopen = false
    init{
        onEnable {
            try {
                if (!SystemTray.isSupported()) {
                    MessageSendHelper.sendChatMessage("$chatName Toast System Not supported.")
                    disable()
                } else {
                    tray = SystemTray.getSystemTray()
                    image = Toolkit.getDefaultToolkit().createImage(javaClass.getResourceAsStream("/assets/minecraft/lambda/lambda.png").readBytes())
                    trayicon = TrayIcon(image, "Lambda Client Notification System")
                    trayicon.isImageAutoSize = true
                    tray.add(trayicon)
                    istrayopen = true
                }
            } catch (e: Exception) {
                MessageSendHelper.sendChatMessage("$chatName Toast System Not supported.")
                disable()
            }
        }
        onDisable {
            if (istrayopen) {
                tray.remove(trayicon)
                istrayopen = false
            }
        }
        listener<ClientChatReceivedEvent> {
            if (it.message.unformattedText.contains("whispers :")) {
                if (!Display.isActive()) {
                    trayicon.displayMessage("Lambda Client", "Whisper Detected.", TrayIcon.MessageType.NONE)
                }
            }
        }
    }
}