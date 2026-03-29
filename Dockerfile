# ビルドステージ
FROM gradle:8.8-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
# ダミーソースでコンパイルを走らせ、全依存JARをキャッシュ
# （build.gradle.kts が変わらない限りこのレイヤーは再利用される）
RUN mkdir -p src/main/kotlin && \
    echo 'package com.mortlace' > src/main/kotlin/Dummy.kt && \
    gradle --no-daemon compileKotlin && \
    gradle --no-daemon dependencies --configuration runtimeClasspath; \
    rm -rf src
COPY src/ src/
RUN gradle bootJar --no-daemon

# 実行ステージ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
