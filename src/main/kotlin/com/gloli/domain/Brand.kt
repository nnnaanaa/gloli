package com.gloli.domain

import jakarta.persistence.*

/** ブランドマスター。ウィッシュリストアイテムに紐づける */
@Entity
@Table(name = "brands")
class Brand(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** ブランド名。一意制約あり */
    @Column(nullable = false, unique = true)
    var name: String,

    /** ブランド公式サイトURL。スクレイパーのドメインマッチングに使用される */
    var url: String? = null
)
