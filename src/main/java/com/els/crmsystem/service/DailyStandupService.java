package com.els.crmsystem.service;

import com.els.crmsystem.entity.Task;
import com.els.crmsystem.entity.User;
import com.els.crmsystem.repository.TaskRepository;
import com.els.crmsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyStandupService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final AuditNotificationService notificationService;

    // Runs every day at 09:00 AM Kyiv time (Changed from 8!)
    @Scheduled(cron = "0 0 9 * * ?", zone = "Europe/Kiev")
//    @Scheduled(fixedRate = 60000)
    @Transactional(readOnly = true)
    public void sendMorningBriefings() {
        log.info("Starting Daily Morning Briefing generation...");

        List<User> usersWithTelegram = userRepository.findAll().stream()
                .filter(User::isEnabled)
                .filter(u -> u.getTelegramId() != null && !u.getTelegramId().trim().isEmpty())
                .toList();

        // Define our time horizons
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime endOfNext7Days = endOfToday.plusDays(7); // The Horizon

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM");

        for (User user : usersWithTelegram) {
            // Fetch everything due up to 7 days from now
            List<Task> tasks = taskRepository.findByAssigneeIdAndCompletedFalseAndDueDateBeforeOrderByDueDateAsc(
                    user.getId(), endOfNext7Days);

            if (tasks.isEmpty()) {
                continue; // User has absolutely nothing due in the next week. Lucky them!
            }

            // Bucket the tasks
            List<Task> overdueTasks = tasks.stream()
                    .filter(t -> t.getDueDate().isBefore(startOfToday))
                    .toList();

            List<Task> todayTasks = tasks.stream()
                    .filter(t -> !t.getDueDate().isBefore(startOfToday) && !t.getDueDate().isAfter(endOfToday))
                    .toList();

            List<Task> upcomingTasks = tasks.stream()
                    .filter(t -> t.getDueDate().isAfter(endOfToday))
                    .toList();

            // Skip sending a message if they ONLY have future tasks (no need to bother them today)
            if (overdueTasks.isEmpty() && todayTasks.isEmpty()) {
                continue;
            }

            // Build the message
            StringBuilder message = new StringBuilder();
            message.append("🌅 *Добрий ранок, ").append(user.getUsername()).append("!*\n");
            message.append("Ось ваш план на сьогодні (").append(LocalDate.now().format(dateFormatter)).append("):\n\n");

            if (!overdueTasks.isEmpty()) {
                message.append("🔴 *Прострочено (").append(overdueTasks.size()).append("):*\n");
                for (Task t : overdueTasks) {
                    message.append("— ").append(t.getTitle()).append(" (до ").append(t.getDueDate().format(dateFormatter)).append(")\n");
                }
                message.append("\n");
            }

            if (!todayTasks.isEmpty()) {
                message.append("🟡 *На сьогодні (").append(todayTasks.size()).append("):*\n");
                for (Task t : todayTasks) {
                    message.append("— ").append(t.getTitle()).append(" (о ").append(t.getDueDate().format(timeFormatter)).append(")\n");
                }
                message.append("\n");
            } else {
                message.append("✅ На сьогодні немає термінових задач.\n\n");
            }

            // The "Horizon" Summary
            if (!upcomingTasks.isEmpty()) {
                message.append("🗓 *Анонс:* У вас заплановано ще *")
                        .append(upcomingTasks.size())
                        .append("* задач(і) на найближчі 7 днів.\n\n");
            }

            message.append("Вдалого дня! ⚡️");

            notificationService.sendDirectMessage(user.getTelegramId(), message.toString());
        }

        log.info("Daily Morning Briefings sent successfully.");
    }
}