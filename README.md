# mortlace

欲しいものリスト・コレクション管理アプリ。

## 機能

- **Items** — ウィッシュリスト管理。URL・名前・ブランド・カテゴリー・価格・優先度・ノートを記録
- **Collection** — ステータスを `Owned` にしたアイテムを自動移動してコレクションとして管理
- **Archive** — 削除したアイテムのソフトデリート。復元・完全削除に対応
- **Brands / Categories** — ブランド・カテゴリーのマスター管理
- **スクレイパー** — URLから商品名・価格・画像を自動取得 (Jsoup)
- **画像管理** — 外部URLまたはファイルアップロードで画像を登録
- **PWA対応** — オフラインキャッシュ・ホーム画面へのインストールに対応

## 技術スタック

| レイヤー | 技術 |
|---|---|
| バックエンド | Kotlin / Spring Boot 3 / Spring Data JPA |
| DB | PostgreSQL（本番） / H2（開発） |
| フロントエンド | Vanilla JS / HTML / CSS（シングルページ） |
| ビルド | Gradle |
| インフラ | Docker / Docker Compose |

## 起動方法

### Docker Compose（推奨）

```bash
docker compose up --build
```

`http://localhost:8080` でアクセスできます。

### ローカル開発（H2）

```bash
./gradlew bootRun
```

H2インメモリDBで起動します。再起動するとデータはリセットされます。

## API

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

主なエンドポイント:

| メソッド | パス | 説明 |
|---|---|---|
| GET | `/api/wishlist` | Items一覧 |
| POST | `/api/wishlist` | Item追加 |
| PUT | `/api/wishlist/{id}` | Item更新 |
| PATCH | `/api/wishlist/{id}/status` | ステータス変更 |
| DELETE | `/api/wishlist/{id}` | アーカイブ（ソフトデリート） |
| POST | `/api/wishlist/{id}/restore` | アーカイブから復元 |
| GET | `/api/scrape?url=` | URLから商品情報を取得 |
| GET/POST | `/api/brands` | ブランド一覧・追加 |
| GET/POST | `/api/categories` | カテゴリー一覧・追加 |
