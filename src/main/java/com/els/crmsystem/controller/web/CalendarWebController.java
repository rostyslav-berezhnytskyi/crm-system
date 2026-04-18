package com.els.crmsystem.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class CalendarWebController {

    @GetMapping("/calendar")
    public String showCalendar() {
        return "calendar/calendar"; // Points to templates/calendar/calendar.html
    }
}