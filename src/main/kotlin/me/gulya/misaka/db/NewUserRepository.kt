package me.gulya.misaka.db

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface NewUserRepository : CrudRepository<NewUser, Long> {
    fun findByUserId(userId: Long): NewUser?
}