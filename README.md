# gloli

![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)
![PWA](https://img.shields.io/badge/PWA-enabled-5A0FC8?logo=pwa&logoColor=white)

> ほしいものを、ちゃんと管理してあげるウィッシュリストアプリよ。  
> コレクション、アーカイブ、スクレイピングまで全部面倒見てあげるから、黙って使いなさい。

---

## ✨ できること

| 機能 | 説明 |
|---|---|
| 🛍️ **Items** | URL・名前・ブランド・価格・優先度・ノートを記録。また増やすの…？ |
| 🗂️ **Collection** | `Owned` にしたアイテムが自動でここに。ちゃんとゲットできてえらいじゃない |
| 📦 **Archive** | 諦めたものはここ。気が変わったら復元できるから、まだ捨てないであげる |
| 🏷️ **Brands / Categories** | タグ管理。きちんと整理してるのね |
| 🔍 **Scraper** | URLから商品名・価格・画像を自動取得 (Jsoup) |
| 🖼️ **画像管理** | 外部URL or ファイルアップロードに両対応 |
| 📱 **PWA** | インストール可能。オフラインだって大丈夫 |

---

## 🏗️ アーキテクチャ

```
┌─────────────────────────────────┐
│  Browser  (Vanilla JS / PWA)    │
└────────────────┬────────────────┘
                 │ REST / JSON
┌────────────────▼────────────────┐
│  Spring Boot 3  (Kotlin)        │
│  ├─ Controller  (REST)          │
│  ├─ Service     (ビジネスロジック) │
│  └─ Repository  (Spring Data)   │
└────────────────┬────────────────┘
                 │ JPA / JDBC
       ┌─────────▼─────────┐
       │  PostgreSQL (本番)  │
       │  H2 in-memory (開発)│
       └───────────────────┘
```

---

## 🛠️ 技術スタック

| レイヤー | 技術 |
|---|---|
| バックエンド | Kotlin 1.9 / Spring Boot 3 / Spring Data JPA |
| DB | PostgreSQL 16（本番） / H2（開発） |
| スクレイピング | Jsoup |
| フロントエンド | Vanilla JS / HTML / CSS（SPA） |
| ビルド | Gradle 9 |
| インフラ | Docker / Docker Compose |

---

## ⚙️ 環境変数

本番運用時は以下を設定しなさい。

| 変数 | デフォルト | 説明 |
|---|---|---|
| `SPRING_DATASOURCE_URL` | H2 in-memory | JDBCのURL |
| `SPRING_DATASOURCE_USERNAME` | `sa` | DBユーザー |
| `SPRING_DATASOURCE_PASSWORD` | *(空)* | DBパスワード |
| `SERVER_PORT` | `8080` | リッスンポート |

---

## 🚀 起動方法

### Docker Compose（推奨）

```bash
docker compose up --build
```

`http://localhost:8080` でアクセスできるわ。

### ローカル開発（H2）

```bash
./gradlew bootRun
```

H2インメモリDBで起動するの。再起動したらデータが消えるから気をつけなさいよ。

---

## 📡 API リファレンス

Swagger UI → `http://localhost:8080/swagger-ui/index.html`

| メソッド | パス | 説明 |
|---|---|---|
| `GET` | `/api/wishlist` | Items一覧 |
| `POST` | `/api/wishlist` | Item追加 |
| `PUT` | `/api/wishlist/{id}` | Item更新 |
| `PATCH` | `/api/wishlist/{id}/status` | ステータス変更 |
| `DELETE` | `/api/wishlist/{id}` | アーカイブ（ソフトデリート） |
| `POST` | `/api/wishlist/{id}/restore` | アーカイブから復元 |
| `GET` | `/api/scrape?url=` | URLから商品情報を取得 |
| `GET/POST` | `/api/brands` | ブランド一覧・追加 |
| `GET/POST` | `/api/categories` | カテゴリー一覧・追加 |
