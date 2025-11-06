# AI Agent приложение

Этот проект предоставляет два приложения на базе Claude AI:
1. **Ktor Web Server** - HTTP API для взаимодействия с AI
2. **Telegram Bot** - Интерактивный бот для мессенджера Telegram

## Требования

- Java 17 или выше
- Gradle (включен через wrapper)
- Переменные окружения:
  - `ANTHROPIC_API_KEY` - Ваш API ключ Anthropic (требуется для обоих приложений)
  - `TELEGRAM_BOT_TOKEN` - Токен вашего Telegram бота (требуется только для Telegram бота)

## Приложения

### 1. Ktor Web Server

HTTP сервер с REST API для взаимодействия с AI.

**Эндпоинты:**
- `GET /` - Эндпоинт проверки работоспособности
- `GET /ask?question=<ваш-вопрос>` - Задать вопрос Claude AI

**Возможности:**
- Валидация длины сообщения (максимум 1000 символов)
- Обработка ошибок
- Интеграция с Claude Sonnet 4.5

**Запуск сервера:**
```bash
./gradlew run
```

Сервер запустится по адресу `http://localhost:8080`

**Пример запроса:**
```bash
curl "http://localhost:8080/ask?question=what%20is%20kotlin"
```

### 2. Telegram Bot

Интерактивный Telegram бот, который отвечает на сообщения используя Claude AI.

**Команды:**
- `/start` - Запустить бота и получить приветственное сообщение
- `/help` - Показать справочную информацию
- `/findTrack` - Найти музыкальные треки с помощью AI (запрашивает год, жанр и регион)

**Возможности:**
- Структурированные JSON-ответы с заголовком и креативными комментариями
- Валидация длины сообщения (максимум 1000 символов)
- Индикаторы печати
- Поддержка форматирования Markdown (жирный, курсив)
- Многошаговые диалоги с управлением состоянием
- AI-валидация пользовательского ввода с настраиваемой температурой
- Обработка ошибок с корректным откатом
- Интеграция с Claude Sonnet 4.5

**Запуск Telegram бота:**
```bash
# Отредактируйте файл local_run и добавьте ваши API ключи, затем запустите:
./local_run
```

**Создание Telegram бота:**
1. Напишите [@BotFather](https://t.me/botfather) в Telegram
2. Отправьте команду `/newbot` и следуйте инструкциям
3. Скопируйте токен бота, предоставленный BotFather
4. Установите его в переменную окружения `TELEGRAM_BOT_TOKEN`

## Сборка и запуск

| Команда                                 | Описание                                                             |
| -----------------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`                        | Запустить тесты                                                      |
| `./gradlew build`                       | Собрать всё                                                          |
| `./gradlew run`                         | Запустить Ktor web сервер                                            |
| `./gradlew runTelegramBot`              | Запустить Telegram бота                                              |
| `./gradlew buildFatJar`                 | Собрать исполняемый JAR со всеми зависимостями                       |

## Структура проекта

```
src/main/kotlin/
├── Application.kt              # Точка входа Ktor сервера
├── TelegramBotApplication.kt   # Точка входа Telegram бота
├── Routing.kt                  # Конфигурация HTTP маршрутов
└── com/com/
    ├── ai/
    │   ├── AiClient.kt        # Интерфейс AI клиента (JSON и обычный текст)
    │   ├── AiMessage.kt       # Модель сообщения с контролем температуры
    │   ├── ClaudeClient.kt    # Реализация Anthropic Claude
    │   └── ClaudeResponse.kt  # Класс структурированного ответа
    ├── bot/
    │   ├── ConversationState.kt      # Управление состоянием диалога
    │   ├── ConversationManager.kt    # Обработчик многошаговых диалогов
    │   └── FindTrackInteractor.kt    # Поиск треков с AI-валидацией
    └── di/
        └── AppModule.kt       # Модуль внедрения зависимостей
```

## Ссылки

- [Документация Ktor](https://ktor.io/docs/home.html)
- [Kotlin Telegram Bot](https://github.com/kotlin-telegram-bot/kotlin-telegram-bot)
- [Документация Anthropic API](https://docs.anthropic.com/)
