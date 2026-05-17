# gloli

あなたのほしいものを、ちゃんと管理してあげるアプリよ。
ウィッシュリストにコレクション、アーカイブまで全部面倒見てあげるから安心しなさい。

## ✨ できること

- 🛍️ **Items** — ほしいものリストね。URL・名前・ブランド・カテゴリー・価格・優先度・ノートを全部記録できるわ。また増やすの…？
- 🗂️ **Collection** — `Owned` にしたアイテムは自動でここに来るの。ちゃんとゲットできてえらいじゃない
- 📦 **Archive** — 諦めたものはここよ。また気が変わったら復元できるから、まだ捨てないであげる
- 🏷️ **Brands / Categories** — ブランドとカテゴリーの管理ね。きちんと整理してるのね
- 🔍 **スクレイパー** — URLから商品名・価格・画像を自動取得してあげるわ (Jsoup)
- 🖼️ **画像管理** — 外部URLかファイルアップロードで画像を登録できるの
- 📱 **PWA対応** — ホーム画面にも追加できるわよ。オフラインだって大丈夫

## 🛠️ 技術スタック

| レイヤー | 技術 |
|---|---|
| バックエンド | Kotlin / Spring Boot 3 / Spring Data JPA |
| DB | PostgreSQL（本番） / H2（開発） |
| フロントエンド | Vanilla JS / HTML / CSS（シングルページ） |
| ビルド | Gradle |
| インフラ | Docker / Docker Compose |

## 🚀 起動方法

じゃあ起動してあげるわ。ちゃんと動かしなさいよ。

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

## 📡 API

Swagger UIは `http://localhost:8080/swagger-ui/index.html` で確認できるわ。

主なエンドポイントはこれよ:

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
