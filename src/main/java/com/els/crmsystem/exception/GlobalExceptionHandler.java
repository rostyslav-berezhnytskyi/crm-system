package com.els.crmsystem.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // This catches ALL general runtime errors (like database issues, null pointers, bad logic)
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        // 1. Log the exact error in your server console so you can fix it later
        logger.error("Unhandled Exception Caught by Global Handler: ", ex);

        // 2. Pass a friendly (or technical) message to the UI
        model.addAttribute("errorMessage", ex.getMessage());

        // 3. Redirect to our custom 500 Error HTML page
        return "error/500";
    }
}
