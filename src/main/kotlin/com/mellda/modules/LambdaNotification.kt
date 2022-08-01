package com.mellda.modules

import com.lambda.client.LambdaMod
import com.lambda.client.event.events.ConnectionEvent
import com.lambda.client.event.events.PacketEvent
import com.lambda.client.event.listener.listener
import com.lambda.client.manager.managers.FriendManager
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.EntityUtils.isFakeOrSelf
import com.lambda.client.util.TickTimer
import com.lambda.client.util.TimeUnit
import com.lambda.client.util.combat.CombatUtils.scaledHealth
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.threads.safeListener
import com.mellda.NotificationPlugin
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.SPacketPlayerListItem
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.opengl.Display
import java.awt.*
import java.util.regex.Pattern

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
    private lateinit var trayIcon : TrayIcon
    private var isTrayOpen = false
    private var playerHealth = 0F

    private val whisper by setting("Whsiper", true)
    private val damage by setting("Damage", true)
    private val spotted by setting("Spotted", true)
    private val friendSpotted by setting("Friend", FriendMode.OnlyFriend, { spotted })
    private val whenName by setting("Name", true)
    private val join by setting("Joined", true)
    private val disconnect by setting("Disconnected", true)
    private val whenJoin by setting("Target Player Join", true)
    private val stalkerName by setting("Player Name", "", { whenJoin })

    private enum class FriendMode {
        Contain, OnlyFriend, Ignore
    }

    private val whisperPattern = Pattern.compile("^([0-9a-zA-Z_]+) whispers: .*")
    private val playerSet = LinkedHashSet<EntityPlayer>()
    private val timer = TickTimer(TimeUnit.SECONDS)

    init{
        onEnable {
            try {
                if (!SystemTray.isSupported()) {
                    MessageSendHelper.sendChatMessage("$chatName Toast System Not supported.")
                    disable()
                } else {
                    tray = SystemTray.getSystemTray()
                    image = Toolkit.getDefaultToolkit().createImage(javaClass.getResourceAsStream("/assets/minecraft/lambda/lambda.png").readBytes())
                    trayIcon = TrayIcon(image, "Lambda Client Notification System")
                    trayIcon.isImageAutoSize = true
                    tray.add(trayIcon)
                    isTrayOpen = true
                }
            } catch (e: Exception) {
                LambdaMod.LOG.error("$e")
                MessageSendHelper.sendChatMessage("$chatName The error occurred, see Minecraft Log for more info.")
                disable()
            }
        }

        onDisable {
            if (isTrayOpen) {
                tray.remove(trayIcon)
                isTrayOpen = false
            }
        }

        safeListener<PacketEvent.Receive> {
            if (it.packet !is SPacketPlayerListItem || !whenJoin || stalkerName.trim() == "") return@safeListener
             val playerPacket = it.packet as SPacketPlayerListItem
             if (playerPacket.action == SPacketPlayerListItem.Action.ADD_PLAYER) {
                 if (playerPacket.entries[0].profile.name == null) return@safeListener
                 if (playerPacket.entries[0].profile.name.lowercase() == stalkerName.trim().lowercase()) {
                     sendNotifications("${playerPacket.entries[0].profile.name} Joined.")
                 }
             }
        }

        listener<ConnectionEvent.Disconnect> {
            if (disconnect) {
                sendNotifications("You've been Disconnected from server.")
            }
        }

        //Code From VisualRange
        safeListener<TickEvent.ClientTickEvent> {
            if (it.phase != TickEvent.Phase.END || !timer.tick(1L) || !spotted) return@safeListener

            val loadedPlayerSet = LinkedHashSet(world.playerEntities)
            for (entityPlayer in loadedPlayerSet) {
                if (entityPlayer.isFakeOrSelf) continue // Self / Freecam / FakePlayer check
                if ((friendSpotted == FriendMode.Ignore) && FriendManager.isFriend(entityPlayer.name)) continue
                if ((friendSpotted == FriendMode.OnlyFriend) && !FriendManager.isFriend(entityPlayer.name)) continue

                if (playerSet.add(entityPlayer) && isEnabled) {
                    sendNotifications("${entityPlayer.name} came into Render Distance.")
                }
            }

            val toRemove = ArrayList<EntityPlayer>()
            for (player in playerSet) {
                if (!loadedPlayerSet.contains(player)) {
                    toRemove.add(player)
                    if (((friendSpotted == FriendMode.OnlyFriend) && FriendManager.isFriend(player.name))
                        || ((friendSpotted == FriendMode.Ignore) && !FriendManager.isFriend(player.name))
                        || (friendSpotted == FriendMode.Contain)) {
                        if (isEnabled) sendNotifications("${player.name} leaved from Render Distance.")
                    }
                }
            }
            playerSet.removeAll(toRemove.toSet())
        }

        safeListener<TickEvent.ClientTickEvent> {
            if (isDisabled || it.phase != TickEvent.Phase.END) return@safeListener
            if (playerHealth > player.scaledHealth && damage) {
                sendNotifications("You've got damaged, HP : ${player.scaledHealth}")
            }
            playerHealth = player.scaledHealth
        }

        listener<ClientChatReceivedEvent> {
            if (it.message.unformattedText == "Connected to the server." && join) {
                sendNotifications("You joined into Main server.")
            }
            if (it.message.unformattedText.contains("whispers:") && whisper) {
                val whispermessage = whisperPattern.matcher(removeColorCode(it.message.unformattedText))
                if (whispermessage.find()) {
                    sendNotifications("Whisper Detected, Sender : ${whispermessage.group(1)}")
                    return@listener
                }
            }
            if (it.message.unformattedText.contains(mc.session.username) && whenName) {
                sendNotifications("Someone called your name.")
            }
        }
    }
    private fun removeColorCode(message: String): String {
        val colorcode = arrayOf("§0","§1","§2","§3","§4","§5","§6","§7","§8","§9","§a","§b","§c","§d","§e","§f","§k","§l","§m","§n","§o","§r")
        var temp = message
        for (i in colorcode) {
            temp = temp.replace(i,"")
        }
        return temp
    }

    private fun sendNotifications(message: String) {
        if (!Display.isActive()) {
            trayIcon.displayMessage("Lambda Client", message, TrayIcon.MessageType.NONE)
        }
    }
}