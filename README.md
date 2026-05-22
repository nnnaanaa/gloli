# gloli

ウィッシュリスト管理アプリ。アイテムの記録・コレクション管理・アーカイブをサポートします。

## 機能

| 機能 | 説明 |
|---|---|
| Items | URL・名前・ブランド・価格・優先度・ノートを記録 |
| Collection | `Owned` にしたアイテムを管理 |
| Archive | 削除したアイテムを保持（復元可能） |
| Brands / Categories | タグ管理 |
| Scraper | URLから商品名・価格・画像を自動取得 (Jsoup) |
| 一括更新 | 全アイテムのURLを再スクレイプして価格・名前を最新化 |
| 画像管理 | 外部URL・ファイルアップロードに両対応 |
| PWA | インストール可能 |

## 技術スタック

| レイヤー | 技術 |
|---|---|
| バックエンド | Kotlin 2.2 / Spring Boot 3 / Spring Data JPA |
| DB | PostgreSQL 16（本番） / H2（開発） |
| スクレイピング | Jsoup |
| フロントエンド | Vanilla JS / HTML / CSS |
| ビルド | Gradle 9 |
| インフラ | Docker / Docker Compose |

## 環境変数

| 変数 | デフォルト | 説明 |
|---|---|---|
| `SPRING_DATASOURCE_URL` | H2 in-memory | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `sa` | DBユーザー |
| `SPRING_DATASOURCE_PASSWORD` | *(空)* | DBパスワード |
| `SERVER_PORT` | `8080` | リッスンポート |

## 起動方法

### Docker Compose

```bash
docker compose up --build
```

`http://localhost:8080` でアクセスできます。

### ローカル開発（H2）

```bash
./gradlew bootRun
```

H2インメモリDBで起動します。再起動するとデータが消えます。

## API

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

| メソッド | パス | 説明 |
|---|---|---|
| `GET` | `/api/wishlist` | Items一覧 |
| `POST` | `/api/wishlist` | Item追加 |
| `PUT` | `/api/wishlist/{id}` | Item更新 |
| `PATCH` | `/api/wishlist/{id}/status` | ステータス変更 |
| `DELETE` | `/api/wishlist/{id}` | アーカイブ（ソフトデリート） |
| `DELETE` | `/api/wishlist/{id}/permanent` | 完全削除 |
| `POST` | `/api/wishlist/{id}/restore` | アーカイブから復元 |
| `POST` | `/api/wishlist/{id}/image` | 画像アップロード |
| `GET` | `/api/wishlist/{id}/image` | 画像取得 |
| `POST` | `/api/wishlist/refresh-all` | 全アイテムを再スクレイプして価格・名前を更新 |
| `GET` | `/api/scrape?url=` | URLから商品情報を取得 |
| `GET/POST` | `/api/brands` | ブランド一覧・追加 |
| `PUT/DELETE` | `/api/brands/{id}` | ブランド更新・削除 |
| `GET/POST` | `/api/categories` | カテゴリー一覧・追加 |
| `PUT/DELETE` | `/api/categories/{id}` | カテゴリー更新・削除 |
