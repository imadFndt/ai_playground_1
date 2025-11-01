#!/bin/bash

# Telegram Bot Local Run Script

# Export environment variables
export ANTHROPIC_API_KEY="your-anthropic-api-key-here"
export TELEGRAM_BOT_TOKEN="your-telegram-bot-token-here"

# Run the Telegram bot
./gradlew runTelegramBot
