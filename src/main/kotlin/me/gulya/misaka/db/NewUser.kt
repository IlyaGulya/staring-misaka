package me.gulya.misaka.db

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class NewUser(
    @Id val id: Long? = null,
    val userId: Long,
    val joinTime: LocalDateTime = LocalDateTime.now()
)