package me.gulya.misaka.db

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PendingBanRequestRepository : CrudRepository<PendingBanRequest, Long> {
    fun findByAdminMessageId(adminMessageId: Long): PendingBanRequest?
}