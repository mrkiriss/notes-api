# Notes API

## Назначение

Notes API - headless REST‑сервис для управления заметками и тегами. Основные функции:
- создание, чтение, обновление и удаление заметок;
- архивирование заметок (`is_archived`);
- назначение тегов заметкам (many‑to‑many);
- поиск с фильтрами/сортировкой/пагинацией.

Сервис самостоятельный, без интеграций с другими микросервисами в рамках ТЗ.

## Архитектура и зависимости

Технологии:
- Kotlin (JVM), JDK 17 - язык и платформа
- Ktor (Netty) - веб‑фреймворк и серверный движок
- kotlinx.serialization - сериализация/десериализация JSON
- PostgreSQL - реляционная база данных
- Exposed (SQL DSL) - ORM/SQL‑DSL для работы с БД
- Flyway - управление миграциями БД
- OpenAPI 3.0 + Stoplight/Swagger UI - документация API
- JUnit 5, ktor-server-test - тестирование
- detekt, ktlint - статический анализ и форматирование кода
- Lefthook - git hooks
- Docker, docker-compose - контейнеризация и запуск окружения

Взаимодействия с другими сервисами:
- нет

Внешние сервисы:
- PostgreSQL

Проектировался в соответствии с API Design Guide – https://docs.ensi.tech/guidelines/api

## Способы запуска

### Docker (рекомендуется)

1) Подготовить `.env`:

```powershell
Copy-Item .env.example .env
```

2) Запуск:

```powershell
docker compose up --build
```

### Локально без Docker

1) Поднять PostgreSQL локально.
2) Создать `.env.local` (имеет приоритет над `.env`).
3) Запуск:

```powershell
./gradlew run
```

### Переменные окружения

`.env.example` и `.env.local.example` содержат шаблоны. Основные переменные:

- `APP_PORT` - порт API (например, `8080`)
- `DB_HOST` - хост PostgreSQL
- `DB_PORT` - порт PostgreSQL
- `DB_NAME` - имя базы
- `DB_USER` - пользователь БД
- `DB_PASSWORD` - пароль БД

Правила:
- `.env` используется docker‑compose и обычно содержит `DB_HOST=postgres`.
- `.env.local` опционален и приоритетнее для локального запуска; обычно содержит `DB_HOST=localhost`.
- `.env` и `.env.local` игнорируются git.

## API документация

Доступные страницы:
- Stoplight UI: `http://localhost:8080/docs`
- Swagger UI: `http://localhost:8080/swagger`
- OpenAPI спецификация: `http://localhost:8080/openapi` (редирект на `/apidoc/v1/index.yaml`)

Основные эндпоинты (prefix `/api/v1`):
- `POST /notes` - создать заметку
- `GET /notes/{id}` - получить заметку
- `PUT /notes/{id}` - полная замена
- `PATCH /notes/{id}` - частичное обновление
- `DELETE /notes/{id}` - удалить
- `POST /notes:search` - поиск списка
- `POST /notes:search-one` - поиск одной записи
- `POST /tags` - создать тег
- `GET /tags/{id}` - получить тег
- `POST /tags:search` - поиск тегов

## Как тестировать

### Unit‑тесты (без Docker)

```powershell
./gradlew test
```

### Интеграционные тесты (внешний PostgreSQL)

1) Поднять PostgreSQL (например, через compose):

```powershell
docker compose up -d postgres
```

2) Запустить тесты:

```powershell
$env:RUN_INTEGRATION_TESTS="true"
$env:IT_DB_HOST="localhost"
$env:IT_DB_PORT="5432"
$env:IT_DB_NAME="notes"
$env:IT_DB_USER="notes"
$env:IT_DB_PASSWORD="notes"
./gradlew test
```

## Git hooks

1) Установить Lefthook: https://lefthook.dev/installation/index.html  
2) Включить хуки: `lefthook install`  

Какие хуки настроены:
   - `pre-commit`: `./gradlew ktlintCheck`, `./gradlew detekt`
   - `pre-push`: `./gradlew test`

## Контакты и поддержка

Автор: Воскребенцев Кирилл
Обратная связь по проекту: kvoskrebentsev@mail.ru, https://t.me/mrkiriss
