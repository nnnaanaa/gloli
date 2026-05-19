package com.gloli.domain

import jakarta.persistence.*

/** カテゴリマスター。ウィッシュリストアイテムに紐づける */
@Entity
@Table(name = "categories")
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** カテゴリ名。一意制約あり */
    @Column(nullable = false, unique = true)
    var name: String
)
