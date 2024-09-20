package me.gulya.misaka.services

import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.spring.AiService

@AiService
interface SpamCheckerService {
    @SystemMessage("You are a spam filter")
    @UserMessage("Determine whether the following message is spam. <message>{{it}}</message>")
    fun isSpam(message: String): Boolean
}