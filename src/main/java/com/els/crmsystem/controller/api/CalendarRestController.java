package com.els.crmsystem.controller.api;

import com.els.crmsystem.dto.output.CalendarEventDto;
import com.els.crmsystem.entity.CalendarEvent;
import com.els.crmsystem.entity.Task;
import com.els.crmsystem.entity.Transaction;
import com.els.crmsystem.repository.CalendarEventRepository;
import com.els.crmsystem.repository.TaskRepository;
import com.els.crmsystem.repository.TransactionRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarRestController {

    private final TaskRepository taskRepository;
    private final TransactionRepository transactionRepository;
    private final CalendarEventRepository calendarEventRepository;

    /**
     * This endpoint is automatically called by FullCalendar.io every time the user
     * changes the month view (e.g., clicking "Next Month").
     */
    @GetMapping("/events")
    public List<CalendarEventDto> getEvents(
            @RequestParam String start,
            @RequestParam String end) {

        // FullCalendar sends ISO strings with timezones (e.g., 2026-04-01T00:00:00+03:00)
        LocalDateTime startDate = ZonedDateTime.parse(start).toLocalDateTime();
        LocalDateTime endDate = ZonedDateTime.parse(end).toLocalDateTime();

        List<CalendarEventDto> events = new ArrayList<>();

        // 1. MANUAL EVENTS
        List<CalendarEvent> manualEvents = calendarEventRepository.findEventsInDateRange(startDate, endDate);
        for (CalendarEvent e : manualEvents) {
            events.add(CalendarEventDto.builder()
                    .id("EVT_" + e.getId())
                    .title(e.getTitle())
                    .start(e.getStartDate().toLocalDate().toString()) // Strip the time to prevent stretching
                    .end(e.getEndDate() != null ? e.getEndDate().toLocalDate().plusDays(1).toString() : null)
                    .color(e.getColor() != null ? e.getColor() : "#8540f5")
                    .type("EVENT")
                    .allDay(true) // Neat solid block
                    .build());
        }

        // 2. TASKS (Workflows)
        List<Task> tasks = taskRepository.findByDueDateBetween(startDate, endDate);
        for (Task t : tasks) {
            String color;

            if (t.isCompleted()) {
                color = "#6c757d"; // Gray: Completed tasks fade to the background
            } else {
                // Determine color based on your specific Priority Enum
                String prio = t.getPriority() != null ? t.getPriority().name() : "LOW";

                color = switch (prio) {
                    case "URGENT", "CRITICAL" -> "#dc3545"; // RED for Urgent
                    case "HIGH" -> "#ffc107";               // YELLOW/ORANGE for High
                    case "MEDIUM" -> "#0dcaf0";             // LIGHT BLUE for Medium
                    default -> "#0d6efd";                   // STANDARD BLUE for Low/Normal
                };
            }

            events.add(CalendarEventDto.builder()
                    .id("TSK_" + t.getId())
                    .title("📋 " + t.getTitle())
                    .start(t.getDueDate().toString()) // Keeps the exact time
                    .color(color)
                    .type("TASK")
                    .allDay(false)
                    .build());
        }

        // 3. TRANSACTIONS (Cashflow)
        List<Transaction> transactions = transactionRepository.findByDateBetween(startDate, endDate);
        for (Transaction txn : transactions) {
            boolean isIncome = txn.getType().name().equals("INCOME");
            // Changed orange to a solid, professional Red (#dc3545)
            String color = isIncome ? "#198754" : "#dc3545";
            String prefix = isIncome ? "💰 + " : "💸 - ";

            events.add(CalendarEventDto.builder()
                    .id("TXN_" + txn.getId())
                    .title(prefix + txn.getAmount() + " ₴")
                    // Removed .toLocalDate() so it keeps the exact Hours and Minutes!
                    .start(txn.getDate().toString())
                    .color(color)
                    .type("TRANSACTION")
                    .url("/transactions")
                    // Changed to FALSE so it drops into the actual time slot on the grid!
                    .allDay(false)
                    .build());
        }

        return events;
    }

    // --- Endpoints for managing the Manual Notes ---

    @PostMapping("/events")
    public void createEvent(@RequestBody CalendarEvent event) {
        if (event.getColor() == null) event.setColor("#8540f5");
        calendarEventRepository.save(event);
    }

    @DeleteMapping("/events/{id}")
    public void deleteEvent(@PathVariable Long id) {
        calendarEventRepository.deleteById(id);
    }
}
