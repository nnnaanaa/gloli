package com.mortlace.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mortlace.repository.BrandRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrandControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var brandRepository: BrandRepository

    @BeforeEach
    fun setUp() {
        brandRepository.deleteAll()
    }

    private fun json(vararg pairs: Pair<String, Any?>) =
        objectMapper.writeValueAsString(pairs.toMap())

    @Test
    fun `list brands returns empty list initially`() {
        mockMvc.perform(get("/api/brands"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `create brand returns 201 with body`() {
        mockMvc.perform(
            post("/api/brands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "Nike", "url" to "https://nike.com"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Nike"))
            .andExpect(jsonPath("$.url").value("https://nike.com"))
            .andExpect(jsonPath("$.id").isNumber)
    }

    @Test
    fun `create brand with blank name returns 400`() {
        mockMvc.perform(
            post("/api/brands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to ""))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create duplicate brand name returns 409`() {
        val body = json("name" to "Adidas")
        mockMvc.perform(post("/api/brands").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
        mockMvc.perform(post("/api/brands").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict)
    }

    @Test
    fun `get brand by id returns brand`() {
        val created = mockMvc.perform(
            post("/api/brands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "Puma"))
        )
            .andExpect(status().isCreated)
            .andReturn()
        val id = objectMapper.readTree(created.response.contentAsString)["id"].asLong()

        mockMvc.perform(get("/api/brands/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Puma"))
    }

    @Test
    fun `get brand by nonexistent id returns 404`() {
        mockMvc.perform(get("/api/brands/99999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `update brand changes name and url`() {
        val created = mockMvc.perform(
            post("/api/brands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "Old Name"))
        )
            .andReturn()
        val id = objectMapper.readTree(created.response.contentAsString)["id"].asLong()

        mockMvc.perform(
            put("/api/brands/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "New Name", "url" to "https://newurl.com"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.url").value("https://newurl.com"))
    }

    @Test
    fun `delete brand returns 204 and brand is gone`() {
        val created = mockMvc.perform(
            post("/api/brands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "ToDelete"))
        )
            .andReturn()
        val id = objectMapper.readTree(created.response.contentAsString)["id"].asLong()

        mockMvc.perform(delete("/api/brands/$id"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/brands/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete nonexistent brand returns 404`() {
        mockMvc.perform(delete("/api/brands/99999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `list brands returns all created brands`() {
        val names = listOf("Brand A", "Brand B", "Brand C")
        names.forEach { name ->
            mockMvc.perform(
                post("/api/brands")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json("name" to name))
            ).andExpect(status().isCreated)
        }

        mockMvc.perform(get("/api/brands"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }
}
