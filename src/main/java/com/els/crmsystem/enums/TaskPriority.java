package com.els.crmsystem.enums;

public enum TaskPriority {
    LOW("Низький"),
    MEDIUM("Середній"),
    HIGH("Високий"),
    URGENT("Терміновий");

    private final String ukrainianName;

    TaskPriority(String ukrainianName) { this.ukrainianName = ukrainianName; }
    public String getUkrainianName() { return ukrainianName; }
}
