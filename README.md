# gloli

ウィッシュリスト管理 PWA。アイテムの記録・コレクション管理・購入統計・予算管理をサポートします。

## 機能

| 機能 | 説明 |
|---|---|
| Items | URL・名前・ブランド・カテゴリ・価格・優先度・ノート・予定購入日を記録 |
| Collection | `Owned` にしたアイテムをギャラリー表示で管理 |
| Archive | 削除したアイテムを保持（復元・完全削除可能） |
| Brands / Categories | ブランド・カテゴリのタグ管理 |
| Scraper | URLから商品名・価格・画像を自動取得 (Jsoup) |
| 一括更新 | 全アイテムのURLを再スクレイプして価格・名前を最新化 |
| 画像管理 | 外部URL・ファイルアップロードに両対応 |
| Stats | 月別支出ダッシュボード・予算管理・先月比・今後の購入予定を可視化 |
| 月別予算 | 月ごとに支出上限を設定（デフォルト ¥30,000）|
| CSV エクスポート | 全アイテム（Wishlist・Collection・Archive）を CSV でダウンロード |
| PWA | インストール可能・Service Worker によるオフライン対応 |

## 技術スタック

| レイヤー | 技術 |
|---|---|
| バックエンド | Kotlin 2.2 / Spring Boot 3 / Spring Data JPA |
| DB | PostgreSQL 16（本番） / H2 file（開発） |
| スクレイピング | Jsoup |
| フロントエンド | Vanilla JS / HTML / CSS（フレームワークなし） |
| ビルド | Gradle 9 |
| インフラ | Docker / Docker Compose |

## 環境変数

| 変数 | デフォルト | 説明 |
|---|---|---|
| `DB_USER` | `wishlist` | PostgreSQL ユーザー名 |
| `DB_PASS` | `changeme` | PostgreSQL パスワード |
| `SERVER_PORT` | `8080` | リッスンポート |

## 起動方法

### Docker Compose（推奨）

```bash
docker compose up --build
```

`http://localhost:8080` でアクセスできます。PostgreSQL 16 が同時に起動します。

### ローカル開発（H2）

```bash
./gradlew bootRun
```

H2 file DB で起動します。`./data/wishlistdb` にデータが永続化されます。

## API

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

| メソッド | パス | 説明 |
|---|---|---|
| `GET` | `/api/wishlist` | Items 一覧（priority / brandId / categoryId でフィルタ可） |
| `POST` | `/api/wishlist` | Item 追加 |
| `PUT` | `/api/wishlist/{id}` | Item 更新 |
| `PATCH` | `/api/wishlist/{id}/status` | ステータス変更 |
| `PATCH` | `/api/wishlist/{id}/priority` | 優先度変更 |
| `DELETE` | `/api/wishlist/{id}` | アーカイブ（ソフトデリート） |
| `DELETE` | `/api/wishlist/{id}/permanent` | 完全削除 |
| `POST` | `/api/wishlist/{id}/restore` | アーカイブから復元 |
| `POST` | `/api/wishlist/{id}/image` | 画像アップロード |
| `GET` | `/api/wishlist/{id}/image` | 画像取得 |
| `GET` | `/api/wishlist/owned` | Collection（Owned アイテム）一覧 |
| `GET` | `/api/wishlist/deleted` | Archive（削除済みアイテム）一覧 |
| `POST` | `/api/wishlist/refresh-all` | 全アイテムを再スクレイプして価格・名前を更新 |
| `GET` | `/api/scrape?url=` | URL から商品情報を取得 |
| `GET/POST` | `/api/brands` | ブランド一覧・追加 |
| `PUT/DELETE` | `/api/brands/{id}` | ブランド更新・削除 |
| `GET/POST` | `/api/categories` | カテゴリー一覧・追加 |
| `PUT/DELETE` | `/api/categories/{id}` | カテゴリー更新・削除 |
