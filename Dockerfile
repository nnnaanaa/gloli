# ビルドステージ
FROM gradle:8.8-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
# プラグイン・依存関係を先にキャッシュ（build.gradle.kts が変わらない限り再ダウンロード不要）
RUN gradle --no-daemon dependencies || true
COPY src/ src/
RUN gradle bootJar --no-daemon

# 実行ステージ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
