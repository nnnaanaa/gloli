package com.gloli.domain.enums

enum class Status {
    WANTED,
    ORDERED,
    // OWNED はウィッシュリストから除外され、コレクション一覧にのみ表示される
    OWNED
}
