package com.gloli.dto

import java.math.BigDecimal

/** スクレイパーが商品ページから取得した情報 */
data class ScrapedProductInfo(
    val name: String?,
    val imageUrl: String?,
    /** スクレイプしたブランド名テキスト。brandId が取れた場合はそちらを優先する */
    val brand: String?,
    /** 登録済みブランドURLとのドメインマッチで特定したブランドID。null の場合は brand 名でフロント側がマッチする */
    val brandId: Long?,
    val price: BigDecimal?,
    val description: String?,
    val category: String?
)
