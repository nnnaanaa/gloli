package com.gloli.domain

import com.gloli.domain.enums.Priority
import com.gloli.domain.enums.Status
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "wishlist_items")
class WishlistItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(columnDefinition = "TEXT")
    var name: String? = null,

    @Column(nullable = false, length = 2048)
    var url: String,

    var price: BigDecimal? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    var brand: Brand? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Enumerated(EnumType.STRING)
    var priority: Priority = Priority.MEDIUM,

    @Enumerated(EnumType.STRING)
    var status: Status = Status.WANTED,

    // ローカル保存した画像のファイル名（./data/images/ 配下）
    var imagePath: String? = null,

    // 外部画像URL。imagePath と両立しないため、どちらかを設定したらもう一方はクリアする
    @Column(length = 2048)
    var imageUrl: String? = null,

    @CreationTimestamp
    @Column(updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // null = アクティブ、non-null = ソフトデリート済み（アーカイブ）
    var deletedAt: LocalDateTime? = null
)
