package com.els.crmsystem.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 1. HANDLE 404 ERRORS
     * This intercepts missing URLs and missing static files.
     * We don't log an error here because users typing bad URLs is normal.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundError() {
        return "error/404";
    }

    /**
     * 2. HANDLE 500 ERRORS
     * This catches ALL other general runtime errors (null pointers, database crashes).
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        // Log the exact error in your server console so you can fix it later
        logger.error("Unhandled Exception Caught by Global Handler: ", ex);

        // Pass a technical message to the UI
        model.addAttribute("errorMessage", ex.getMessage());

        // Redirect to our custom 500 Error HTML page
        return "error/500";
    }
}