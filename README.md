# gloli

A wishlist PWA for the dedicated collector. Track brand items, build your dream coords, and stay within your monthly budget.

## Features

| Feature | Description |
|---|---|
| Items | Record URL, name, brand, category, price, priority, notes, and planned purchase date |
| Collection | Gallery view of items marked as `Owned` |
| Archive | Holds deleted items (restore or permanently delete) |
| Brands / Categories | Tag management for brands and categories |
| Scraper | Automatically fetches product name, price, and image from a URL (Jsoup) |
| Bulk Refresh | Re-scrapes all item URLs to update prices and names; shows a per-item change summary on completion |
| Image Management | Supports both external URLs and file uploads |
| Stats | Monthly spending dashboard with budget tracking, month-over-month comparison, and upcoming purchases |
| Monthly Budget | Set a spending limit per month (default ¥30,000), synced across devices via DB |
| CSV Export | Download all items (Wishlist, Collection, Archive) as CSV |
| Themes | Switch between 5 color themes via swatches at the bottom of the drawer |
| PWA | Installable with offline support via Service Worker |

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Kotlin 2.2 / Spring Boot 3 / Spring Data JPA |
| Database | PostgreSQL 16 (production) / H2 file (development) |
| Scraping | Jsoup |
| Frontend | Vanilla JS / HTML / CSS (no framework) |
| Build | Gradle 9 |
| Infrastructure | Docker / Docker Compose |

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USER` | `wishlist` | PostgreSQL username |
| `DB_PASS` | `changeme` | PostgreSQL password |
| `SERVER_PORT` | `8080` | Listening port |

## Running the App

### Docker Compose (recommended)

```bash
docker compose up --build
```

Access the app at `http://localhost:8080`. PostgreSQL 16 starts alongside the app.

### Local Development (H2)

```bash
./gradlew bootRun
```

Runs with an H2 file database. Data is persisted at `./data/wishlistdb`.

## API

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/wishlist` | List items (filterable by priority / brandId / categoryId) |
| `POST` | `/api/wishlist` | Add item |
| `PUT` | `/api/wishlist/{id}` | Update item |
| `PATCH` | `/api/wishlist/{id}/status` | Change status |
| `PATCH` | `/api/wishlist/{id}/priority` | Change priority |
| `DELETE` | `/api/wishlist/{id}` | Archive (soft delete) |
| `DELETE` | `/api/wishlist/{id}/permanent` | Permanently delete |
| `POST` | `/api/wishlist/{id}/restore` | Restore from archive |
| `POST` | `/api/wishlist/{id}/image` | Upload image |
| `GET` | `/api/wishlist/{id}/image` | Get image |
| `GET` | `/api/wishlist/owned` | List Collection (owned items) |
| `GET` | `/api/wishlist/deleted` | List Archive (deleted items) |
| `POST` | `/api/wishlist/refresh-all` | Re-scrape all items; response includes per-item change details |
| `GET` | `/api/scrape?url=` | Fetch product info from URL |
| `GET/POST` | `/api/brands` | List / add brands |
| `PUT/DELETE` | `/api/brands/{id}` | Update / delete brand |
| `GET/POST` | `/api/categories` | List / add categories |
| `PUT/DELETE` | `/api/categories/{id}` | Update / delete category |
| `GET` | `/api/settings/{key}` | Get setting value |
| `PUT` | `/api/settings/{key}` | Save setting value |
