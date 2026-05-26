# CLAUDE.md

## Project Overview

**gloli** — A wishlist management PWA for Lolita fashion. Users track items they want (WANTED → ORDERED → OWNED), manage their collection, and monitor monthly spending against a budget.

## Common Commands

```bash
# Run with H2 (development)
./gradlew bootRun

# Run tests
./gradlew test

# Build JAR
./gradlew build

# Run with PostgreSQL profile
./gradlew bootRun --args='--spring.profiles.active=postgres'

# Docker (production)
docker compose up --build
```

## Architecture

```
Controller → Service → Repository (Spring Data JPA)
```

- All business logic lives in `service/`. Controllers are thin.
- Filters (priority, brandId, categoryId) are applied in Kotlin after `findAllByDeletedAtIsNull()` — not in DB queries.
- `WishlistItem` is the central entity; `Brand` and `Category` are FK-linked masters.
- `AppSettings` is a generic key/value table. Currently used to persist `month-budgets` (JSON object).

## Key Domain Rules

- **Status flow**: `WANTED` → `ORDERED` → `OWNED`. Moving to `OWNED` or `ORDERED` auto-sets `purchasedAt` (if not already set); reverting to `WANTED` clears it.
- **Soft delete**: `DELETE /api/wishlist/{id}` sets `deletedAt`. Permanent delete is a separate endpoint.
- **Image exclusivity**: `imagePath` (local file) and `imageUrl` (external URL) are mutually exclusive. Setting one clears the other.
- **Image validation**: File type is determined by magic bytes, not extension. Only JPEG, PNG, GIF, WebP are accepted.
- **URL uniqueness**: Enforced at the service layer (not DB constraint) on non-deleted items.
- **Bulk refresh**: Re-scrapes all non-deleted items. `name` is always overwritten when scrape result is non-blank; `imageUrl` is only filled in if neither `imagePath` nor `imageUrl` is set. Returns per-item `FieldChange` details for UI summary.

## Frontend Conventions

- Single-page app in `src/main/resources/static/`. No build step — files are served as-is by Spring Boot.
- All API calls go through the `api(path, opt)` helper which prefixes `/api`.
- Global state: `_items` (wishlist array), `_monthBudgets` (budget map, synced with DB).
- HTML escaping: always use `esc()` before inserting user data into innerHTML.
- Theme CSS variables are applied to `:root` via `applyTheme()`. Never hardcode colors.
- The mascot (`window.mascotSay(msg)`) is called after user actions for personality — keep messages in Japanese.

## Testing

- Tests use `@SpringBootTest` + `MockMvc` + H2 in-memory (`application-test.yml`, `spring.profiles.active=test`).
- `@BeforeEach` deletes all rows — tests are independent and stateless.
- No mocking of the database; tests hit the real H2 instance.
- Test files mirror the controller they test: `WishlistItemControllerTest`, `BrandControllerTest`, `CategoryControllerTest`.

## Adding a New Feature — Checklist

1. **Domain change** → update entity in `domain/`, DTOs in `dto/`
2. **Service logic** → `service/`; keep controllers thin
3. **Controller** → add Swagger `@Operation` annotation
4. **Frontend** → update `app.js` (API call + render); update `index.html` if new HTML needed; update `style.css` for any new classes
5. **README.md** → update Features table and/or API table if user-visible
6. **Tests** → add cases to the relevant `*ControllerTest.kt`

## File Map

```
src/main/kotlin/com/gloli/
  WishlistApplication.kt        — entry point
  DataMigrationRunner.kt        — startup migration (null status → WANTED)
  controller/                   — REST controllers (thin)
  service/                      — business logic
    WishlistItemService.kt      — main CRUD, image, bulk refresh
    ProductScraperService.kt    — Jsoup scraper (OGP → Schema.org → DOM fallback)
    AppSettingsService.kt       — key/value settings
    BrandService.kt / CategoryService.kt
  domain/                       — JPA entities
  dto/                          — request/response data classes
  repository/                   — Spring Data JPA interfaces

src/main/resources/
  application.yml               — H2 dev config (port 8080)
  application-postgres.yml      — PostgreSQL production config
  static/
    index.html                  — SPA shell (6 tabs)
    app.js                      — all frontend logic
    style.css                   — styles (CSS variables for theming)
    sw.js                       — Service Worker
    manifest.json               — PWA manifest

src/test/
  kotlin/.../controller/        — MockMvc integration tests
  resources/application-test.yml — H2 in-memory test config
```

## Scraper Notes

`ProductScraperService` tries sources in priority order:
1. OGP meta tags (`og:title`, `og:image`, `og:price:amount`, ...)
2. Schema.org JSON-LD (`Product`, `BreadcrumbList`)
3. `itemprop` attributes
4. CSS selector heuristics (class/id containing `product-image`, `price`, etc.)

Brand matching prefers URL domain comparison over text matching. Requires `Brand.url` to be set.
