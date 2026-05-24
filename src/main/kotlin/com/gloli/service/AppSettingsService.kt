package com.gloli.service

import com.gloli.domain.AppSettings
import com.gloli.repository.AppSettingsRepository
import org.springframework.stereotype.Service

@Service
class AppSettingsService(private val repo: AppSettingsRepository) {

    fun get(key: String): String? = repo.findById(key).map { it.value }.orElse(null)

    fun set(key: String, value: String) {
        val existing = repo.findById(key)
        if (existing.isPresent) existing.get().value = value
        else repo.save(AppSettings(key, value))
    }
}
