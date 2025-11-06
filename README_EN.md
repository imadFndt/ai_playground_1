# AI Agent Application

This project provides two applications powered by Claude AI:
1. **Ktor Web Server** - HTTP API for AI interactions
2. **Telegram Bot** - Interactive bot for Telegram messenger

## Prerequisites

- Java 17 or higher
- Gradle (included via wrapper)
- Environment variables:
  - `ANTHROPIC_API_KEY` - Your Anthropic API key (required for both applications)
  - `TELEGRAM_BOT_TOKEN` - Your Telegram bot token (required only for Telegram bot)

## Applications

### 1. Ktor Web Server

HTTP server with REST API endpoints for AI interactions.

**Endpoints:**
- `GET /` - Health check endpoint
- `GET /ask?question=<your-question>` - Ask Claude AI a question

**Features:**
- Message length validation (max 1000 characters)
- Error handling
- Integration with Claude Sonnet 4.5

**Running the server:**
```bash
./gradlew run
```

The server will start on `http://localhost:8080`

**Example request:**
```bash
curl "http://localhost:8080/ask?question=what%20is%20kotlin"
```

### 2. Telegram Bot

Interactive Telegram bot that responds to messages using Claude AI.

**Commands:**
- `/start` - Start the bot and get a welcome message
- `/help` - Display help information
- `/findTrack` - Find music tracks with AI-guided conversation (asks for year, genre, and region)

**Features:**
- Structured JSON responses with title and creative commentary
- Message length validation (max 1000 characters)
- Typing indicators
- Markdown formatting support (bold, italic)
- Multi-step conversational workflows with state management
- AI-powered validation of user inputs with configurable temperature
- Error handling with graceful fallbacks
- Integration with Claude Sonnet 4.5

**Running the Telegram bot:**
```bash
# Edit local_run file and add your API keys, then run:
./local_run
```

**Creating a Telegram bot:**
1. Message [@BotFather](https://t.me/botfather) on Telegram
2. Send `/newbot` and follow the instructions
3. Copy the bot token provided by BotFather
4. Set it as the `TELEGRAM_BOT_TOKEN` environment variable

## Building & Running

| Task                                    | Description                                                          |
| -----------------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`                        | Run the tests                                                        |
| `./gradlew build`                       | Build everything                                                     |
| `./gradlew run`                         | Run the Ktor web server                                              |
| `./gradlew runTelegramBot`              | Run the Telegram bot                                                 |
| `./gradlew buildFatJar`                 | Build an executable JAR with all dependencies included               |

## Project Structure

```
src/main/kotlin/
├── Application.kt              # Ktor server entry point
├── TelegramBotApplication.kt   # Telegram bot entry point
├── Routing.kt                  # HTTP routes configuration
└── com/com/
    ├── ai/
    │   ├── AiClient.kt        # AI client interface (JSON & plain text)
    │   ├── AiMessage.kt       # Message model with temperature control
    │   ├── ClaudeClient.kt    # Anthropic Claude implementation
    │   └── ClaudeResponse.kt  # Structured response data class
    ├── bot/
    │   ├── ConversationState.kt      # Conversation state management
    │   ├── ConversationManager.kt    # Multi-step conversation handler
    │   └── FindTrackInteractor.kt    # Music track finder with AI validation
    └── di/
        └── AppModule.kt       # Dependency injection module
```

## Links

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Kotlin Telegram Bot](https://github.com/kotlin-telegram-bot/kotlin-telegram-bot)
- [Anthropic API Documentation](https://docs.anthropic.com/)

