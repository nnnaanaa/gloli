package com.gloli.repository

import com.gloli.domain.WishlistItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/** ウィッシュリストアイテムの永続化操作 */
@Repository
interface WishlistItemRepository : JpaRepository<WishlistItem, Long> {
    /** アクティブなアイテム一覧（ソフトデリート済みを除く）*/
    fun findAllByDeletedAtIsNull(): List<WishlistItem>

    /** アーカイブ済みアイテム一覧 */
    fun findAllByDeletedAtIsNotNull(): List<WishlistItem>

    /** URL重複チェック（新規登録時） */
    fun existsByUrlAndDeletedAtIsNull(url: String): Boolean

    /** URL重複チェック（更新時。自分自身は除外する）*/
    fun existsByUrlAndDeletedAtIsNullAndIdNot(url: String, id: Long): Boolean
}
