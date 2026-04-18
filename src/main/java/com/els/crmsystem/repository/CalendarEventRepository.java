package com.els.crmsystem.repository;

import com.els.crmsystem.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    // FullCalendar will ask for events between two specific dates (the current month view)
    // This query grabs only the events that overlap with the currently viewed month!
    @Query("SELECT e FROM CalendarEvent e WHERE e.startDate >= :start AND (e.endDate IS NULL OR e.endDate <= :end)")
    List<CalendarEvent> findEventsInDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}