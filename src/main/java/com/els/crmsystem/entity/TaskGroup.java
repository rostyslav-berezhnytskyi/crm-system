package com.els.crmsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // e.g., "Нові Ліди", "В роботі", "Бухгалтерія"

    @Column(name = "display_order", nullable = false)
    private int displayOrder; // 1, 2, 3 (Determines column position left-to-right)

    @Column(length = 7)
    private String colorHex; // Optional: Gives the column header a cool color

    // This allows you to easily grab all tasks in a column
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC") // Automatically sorts tasks top-to-bottom
    @SQLRestriction("is_completed = false")
    private List<Task> tasks = new ArrayList<>();
}