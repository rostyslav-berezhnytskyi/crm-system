package com.els.crmsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j // We still use Slf4j here to write to the local file
public class AuditNotificationService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String chatId;

    private final RestTemplate restTemplate = new RestTemplate();

    // --- 1. THE SAFE USER EXTRACTOR ---
    private String getCurrentUserSafe() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        // If there is a real logged-in user, return their name
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return auth.getName();
        }
        // If it's a Cron Job or API, don't crash! Just return this:
        return "Система (Автоматично)";
    }

    // --- 2. THE UPDATED NOTIFY METHOD ---
    public void notifyAndLog(String actionType, String messageTemplate, Object... args) {
        String user = getCurrentUserSafe(); // Grabs user automatically!
        String details = String.format(messageTemplate, args); // Formats text automatically!

        String emoji = getEmojiForAction(actionType);
        String message = String.format("%s *%s*\n👤 Користувач: %s\n📝 Деталі: %s", emoji, actionType, user, details);

        log.info("AUDIT [{}]: User '{}' - {}", actionType, user, details);
        sendToTelegram(message);
    }

    // --- 3. THE UPDATED CRITICAL METHOD ---
    public void notifyCriticalAlert(String messageTemplate, Object... args) {
        String user = getCurrentUserSafe();
        String details = String.format(messageTemplate, args);

        String message = String.format("🚨 *КРИТИЧНА ДІЯ / ПОМИЛКА*\n👤 Користувач: %s\n⚠️ Деталі: %s", user, details);

        log.warn("CRITICAL AUDIT: User '{}' - {}", user, details);
        sendToTelegram(message);
    }

    @Async
    public void sendToTelegram(String text) {
        try {
            // Telegram API URL
            String url = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=Markdown",
                    botToken, chatId, text);
            restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            // If Telegram is blocked or down, just log it locally so the CRM doesn't crash
            log.error("Failed to send Telegram notification: {}", e.getMessage());
        }
    }

    // --- 4. NEW: PERSONAL DIRECT MESSAGES ---
    @Async
    public void sendDirectMessage(String userTelegramId, String text) {
        if (userTelegramId == null || userTelegramId.trim().isEmpty()) {
            return; // Skip if the user hasn't linked their Telegram
        }

        try {
            // Same URL, but we use the USER'S chat ID instead of the group's chat ID
            String url = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=Markdown",
                    botToken, userTelegramId, text);
            restTemplate.getForObject(url, String.class);
            log.info("Sent direct Telegram notification to user ID: {}", userTelegramId);
        } catch (Exception e) {
            log.error("Failed to send direct Telegram notification: {}", e.getMessage());
        }
    }

    // Helper method to make the Telegram feed look nice
    private String getEmojiForAction(String actionType) {
        if (actionType.contains("Транзакція")) return "💰";
        if (actionType.contains("Проєкт")) return "🏗️";
        if (actionType.contains("Компанія") || actionType.contains("Контакт")) return "🤝";
        if (actionType.contains("Експорт")) return "📥";
        if (actionType.contains("Користувач")) return "🔐";
        return "ℹ️";
    }
}