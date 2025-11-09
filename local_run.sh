#!/bin/bash

# Telegram Bot Local Run Script

# Export environment variables
export ANTHROPIC_API_KEY="your-anthropic-api-key-here"
export TELEGRAM_BOT_TOKEN="your-telegram-bot-token-here"

# Yandex GPT Configuration
export YANDEX_API_KEY="your-yandex-api-key-here"
export YANDEX_FOLDER_ID="your-yandex-folder-id-here"

# Run the Telegram bot
./gradlew runTelegramBot
