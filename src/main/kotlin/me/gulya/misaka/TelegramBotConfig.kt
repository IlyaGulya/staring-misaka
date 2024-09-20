package me.gulya.misaka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
class TelegramBotConfig {
    var adminId: Long = 0
    lateinit var trackingChatIds: List<Long>
}