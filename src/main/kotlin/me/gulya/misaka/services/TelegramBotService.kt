package me.gulya.misaka.services

import dev.voroby.springframework.telegram.client.TdApi
import dev.voroby.springframework.telegram.client.TdApi.MessageReplyToMessage
import dev.voroby.springframework.telegram.client.TdApi.UpdateNewMessage
import dev.voroby.springframework.telegram.client.TelegramClient
import dev.voroby.springframework.telegram.client.updates.UpdateNotificationListener
import me.gulya.misaka.TelegramBotConfig
import me.gulya.misaka.db.NewUser
import me.gulya.misaka.db.NewUserRepository
import me.gulya.misaka.db.PendingBanRequest
import me.gulya.misaka.db.PendingBanRequestRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class TelegramBotService(
    @Lazy private val telegramClient: TelegramClient,
    private val newUserRepository: NewUserRepository,
    private val pendingBanRequestRepository: PendingBanRequestRepository,
    private val spamCheckerService: SpamCheckerService,
    private val config: TelegramBotConfig
) : UpdateNotificationListener<UpdateNewMessage> {

    init {
        println()
    }

    private val logger = LoggerFactory.getLogger(TelegramBotService::class.java)

    override fun handleNotification(notification: UpdateNewMessage) {
        val message = notification.message
        if (message.chatId in config.trackingChatIds) {
            handleMessage(message)
        } else if (message.chatId == config.adminId) {
            handleAdminReply(message)
        }
    }

    private fun handleMessage(message: TdApi.Message) {
        val senderId = message.senderId
        if (senderId is TdApi.MessageSenderUser) {
            val newUser = newUserRepository.findByUserId(senderId.userId)
            if (newUser != null) {
                logger.info("Processing first message from new user ${senderId.userId}: ${message.content}")

                if (spamCheckerService.isSpam(getMessageText(message))) {
                    notifyAdmin(senderId.userId, message)
                }
                newUserRepository.delete(newUser)
            }
        }
    }

    private fun getMessageText(message: TdApi.Message): String {
        return when (val content = message.content) {
            is TdApi.MessageText -> content.text.text
            else -> "Non-text message"
        }
    }

    private fun notifyAdmin(senderId: Long, message: TdApi.Message) {
        val adminMessage = """
            User $senderId sent a message:

            ${getMessageText(message)}

            Should I ban this user? Reply 'yes' to ban.
        """.trimIndent()

        telegramClient.sendAsync(
            TdApi.SendMessage(
                config.adminId,
                0,
                null,
                null,
                null,
                TdApi.InputMessageText(TdApi.FormattedText(adminMessage, null), null, false)
            )
        )
            .thenAccept { sentMessage ->
                pendingBanRequestRepository.save(
                    PendingBanRequest(
                        adminMessageId = sentMessage.`object`.id,
                        senderId = senderId,
                        originalChatId = message.chatId,
                        originalMessageId = message.id
                    )
                )
            }
    }

    private fun handleAdminReply(message: TdApi.Message) {
        val replyTo = message.replyTo
        if (replyTo is MessageReplyToMessage) {
            val pendingRequest = pendingBanRequestRepository.findByAdminMessageId(replyTo.messageId)
            if (pendingRequest != null) {
                if (getMessageText(message).trim().lowercase() == "yes") {
                    banUser(pendingRequest)
                } else {
                    noActionTaken(pendingRequest.senderId)
                }
                pendingBanRequestRepository.delete(pendingRequest)
            }
        } else {
            val isSpam = spamCheckerService.isSpam(getMessageText(message))
            sendMessageToAdmin("Is spam: $isSpam")
        }
    }

    private fun banUser(pendingRequest: PendingBanRequest) {
        telegramClient.sendAsync(
            TdApi.SendMessage(
                pendingRequest.originalChatId,
                pendingRequest.originalMessageId,
                null,
                null,
                null,
                TdApi.InputMessageText(TdApi.FormattedText("/sban", null), null, false)
            )
        )
        sendMessageToAdmin("User ${pendingRequest.senderId} has been banned.")
    }

    private fun noActionTaken(senderId: Long) {
        sendMessageToAdmin("No action taken against user $senderId.")
    }

    private fun sendMessageToAdmin(text: String) {
        telegramClient.sendAsync(
            TdApi.SendMessage(
                config.adminId,
                0,
                null,
                null,
                null,
                TdApi.InputMessageText(TdApi.FormattedText(text, null), null, false)
            )
        )
    }

    override fun notificationType(): Class<UpdateNewMessage> {
        return UpdateNewMessage::class.java
    }

    fun handleNewChatMembers(chatId: Long, newMembers: List<TdApi.User>) {
        newMembers.forEach { user ->
            newUserRepository.save(NewUser(userId = user.id))
            logger.info("User ${user.id} joined the chat $chatId.")
        }
    }
}
