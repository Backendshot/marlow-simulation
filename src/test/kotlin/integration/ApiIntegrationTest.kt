package com.marlow.integration

// src/test/kotlin/integration/ApiIntegrationTest.kt
import com.marlow.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiIntegrationTest {
    @Test
    fun `POST create adds a todo`() = testApplication {
        application { module() }

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                io.ktor.serialization.kotlinx.json.DefaultJson
            }
        }

        val payload = """{ "userId": 5, "id": 205, "title": "test case", "completed": true }"""
        val response = client.post("/api/v2/create") {
            bearerAuth("UMTC")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        // parse JSON back if needed, or inspect headers/body
    }
}