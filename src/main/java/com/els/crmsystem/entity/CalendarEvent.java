package com.els.crmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "calendar_events")
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    // To color-code events (e.g., "#ff9f89" for holidays, "#00c6ff" for meetings)
    @Column(length = 20)
    private String color;
}
