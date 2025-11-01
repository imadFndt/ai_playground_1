package com.com

import com.com.ai.AiMessage
import com.com.di.AppModule
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

const val MAX_MESSAGE_LENGTH = 1000

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/ask") {
            val question = call.request.queryParameters["question"]
                ?: return@get call.respondText("Missing 'question' parameter", status = HttpStatusCode.BadRequest)

            if (question.length > MAX_MESSAGE_LENGTH) {
                return@get call.respondText(
                    "Question exceeds maximum length of $MAX_MESSAGE_LENGTH characters",
                    status = HttpStatusCode.BadRequest
                )
            }

            try {
                val aiClient = AppModule.provideAiClient()
                val response = aiClient.sendMessage(AiMessage("user", question))
                call.respondText(response)
            } catch (e: IllegalStateException) {
                call.respondText(
                    "Error: ${e.message ?: "Invalid state while processing request"}",
                    status = HttpStatusCode.InternalServerError
                )
            } catch (e: Exception) {
                call.respondText(
                    "Error communicating with AI service: ${e.message ?: "Unknown error"}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }
}
