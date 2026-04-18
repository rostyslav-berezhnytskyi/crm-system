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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyStandupService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final AuditNotificationService notificationService;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM");

    // =========================================================================
    // 1. SCHEDULERS
    // =========================================================================

    // Weekdays (Mon-Fri) at 09:00
    @Scheduled(cron = "0 0 9 * * MON-FRI", zone = "Europe/Kiev")
    @Transactional(readOnly = true)
    public void sendWeekdayMorningBriefings() {
        log.info("Running Weekday Morning Briefing...");
        executeMorningBriefing("🌅 *Добрий ранок, %s!*\nОсь ваш план на сьогодні (%s):\n\n");
    }

    // Weekends (Sat-Sun) at 10:30 (Later time, different text)
    @Scheduled(cron = "0 30 10 * * SAT,SUN", zone = "Europe/Kiev")
    @Transactional(readOnly = true)
    public void sendWeekendMorningBriefings() {
        log.info("Running Weekend Morning Briefing...");
        executeMorningBriefing("☕ *Добрий ранок вихідного дня, %s!*\nЯкщо ви сьогодні працюєте, ось ваш план (%s):\n\n");
    }

    // Every Evening (Mon-Fri) at 19:00
    @Scheduled(cron = "0 0 19 * * MON-FRI", zone = "Europe/Kiev")
    @Transactional(readOnly = true)
    public void sendEveningWrapUp() {
        log.info("Running Evening Wrap-Up...");
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        for (User user : getTelegramUsers()) {
            List<Task> completedToday = taskRepository.findByAssigneeIdAndCompletedTrueAndCompletedAtBetween(
                    user.getId(), startOfToday, endOfToday);

            if (!completedToday.isEmpty()) {
                StringBuilder msg = new StringBuilder();
                msg.append("🌙 *Підсумки дня!*\nВи чудово попрацювали сьогодні та закрили *")
                        .append(completedToday.size()).append("* задач(і):\n\n");

                for (Task t : completedToday) {
                    msg.append("✅ ").append(t.getTitle()).append("\n");
                }
                msg.append("\nГарного вечора! 🍷");
                notificationService.sendDirectMessage(user.getTelegramId(), msg.toString());
            }
        }
    }

    // Friday Evening (19:05) - Weekly Summary
    @Scheduled(cron = "0 5 19 * * FRI", zone = "Europe/Kiev")
    @Transactional(readOnly = true)
    public void sendWeeklySummary() {
        log.info("Running Friday Weekly Summary...");
        LocalDateTime startOfWeek = LocalDateTime.of(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), LocalTime.MIN);
        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        for (User user : getTelegramUsers()) {
            List<Task> completedThisWeek = taskRepository.findByAssigneeIdAndCompletedTrueAndCompletedAtBetween(
                    user.getId(), startOfWeek, endOfToday);

            if (!completedThisWeek.isEmpty()) {
                String msg = String.format(
                        "🏆 *Підсумки тижня!*\nЗа цей тиждень ви успішно виконали *%d* задач(і)!\nДякуємо за роботу та бажаємо чудових вихідних! 🎉",
                        completedThisWeek.size()
                );
                notificationService.sendDirectMessage(user.getTelegramId(), msg);
            }
        }
    }

    // =========================================================================
    // 2. CORE LOGIC HELPERS
    // =========================================================================

    private void executeMorningBriefing(String greetingTemplate) {
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime endOfNext7Days = endOfToday.plusDays(7);

        for (User user : getTelegramUsers()) {
            List<Task> tasks = taskRepository.findByAssigneeIdAndCompletedFalseAndDueDateBeforeOrderByDueDateAsc(
                    user.getId(), endOfNext7Days);

            if (tasks.isEmpty()) continue;

            List<Task> overdueTasks = tasks.stream().filter(t -> t.getDueDate().isBefore(startOfToday)).toList();
            List<Task> todayTasks = tasks.stream().filter(t -> !t.getDueDate().isBefore(startOfToday) && !t.getDueDate().isAfter(endOfToday)).toList();
            List<Task> upcomingTasks = tasks.stream().filter(t -> t.getDueDate().isAfter(endOfToday)).toList();

            if (overdueTasks.isEmpty() && todayTasks.isEmpty()) continue;

            StringBuilder message = new StringBuilder();
            message.append(String.format(greetingTemplate, user.getUsername(), LocalDate.now().format(dateFormatter)));

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

            if (!upcomingTasks.isEmpty()) {
                message.append("🗓 *Анонс:* У вас заплановано ще *")
                        .append(upcomingTasks.size())
                        .append("* задач(і) на найближчі 7 днів.\n\n");
            }

            message.append("Вдалого дня! ⚡️");
            notificationService.sendDirectMessage(user.getTelegramId(), message.toString());
        }
    }

    private List<User> getTelegramUsers() {
        return userRepository.findAll().stream()
                .filter(User::isEnabled)
                .filter(u -> u.getTelegramId() != null && !u.getTelegramId().trim().isEmpty())
                .toList();
    }
}