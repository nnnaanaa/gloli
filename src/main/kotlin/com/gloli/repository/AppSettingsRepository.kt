package com.gloli.repository

import com.gloli.domain.AppSettings
import org.springframework.data.jpa.repository.JpaRepository

interface AppSettingsRepository : JpaRepository<AppSettings, String>
