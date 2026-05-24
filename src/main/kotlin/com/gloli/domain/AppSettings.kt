package com.gloli.domain

import jakarta.persistence.*

/** アプリ設定を key/value 形式で永続化する */
@Entity
@Table(name = "app_settings")
class AppSettings(
    @Id
    @Column(nullable = false)
    val key: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var value: String
)
