package me.gulya.misaka.db

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class PendingBanRequest(
    @Id val id: Long? = null,
    val adminMessageId: Long,
    val senderId: Long,
    val originalChatId: Long,
    val originalMessageId: Long,
    val createdAt: LocalDateTime = LocalDateTime.now()
)