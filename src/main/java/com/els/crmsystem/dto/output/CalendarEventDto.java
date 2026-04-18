package com.els.crmsystem.dto.output;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarEventDto {
    private String id;
    private String title;
    private String start;
    private String end;
    private String color;
    private String url;
    private String type;
    private Boolean allDay; // ADD THIS to prevent timezone stretching!
}