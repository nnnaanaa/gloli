package com.gloli.domain

import jakarta.persistence.*

@Entity
@Table(name = "brands")
class Brand(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    var name: String,

    var url: String? = null
)
