package com.els.crmsystem.exception;

import com.els.crmsystem.service.AuditNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor // <-- Added so Spring can inject your AuditService
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // <-- Injecting your universal Telegram service
    private final AuditNotificationService auditService;

    /**
     * 1. HANDLE 404 ERRORS
     * This intercepts missing URLs and missing static files.
     * We DO NOT send this to Telegram, otherwise internet bots will spam your phone!
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundError() {
        return "error/404";
    }

    /**
     * 2. HANDLE 500 ERRORS
     * This catches ALL other general runtime errors (null pointers, database crashes).
     * We send this directly to Telegram so you can fix it immediately.
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, HttpServletRequest request, Model model) {
        // 1. Log the exact error in your server console (keeps the full stack trace safe)
        logger.error("Unhandled Exception Caught by Global Handler: ", ex);

        // 2. Extract useful info
        String errorUrl = request.getRequestURI(); // Which page crashed?
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();

        // 3. Fire the Telegram Alert!
        auditService.notifyCriticalAlert("Збій системи (500 Error)! \nСторінка: %s \nПричина: %s",
                errorUrl, errorMessage);

        // 4. Pass a technical message to the UI for the user
        model.addAttribute("errorMessage", errorMessage);

        // 5. Redirect to our custom 500 Error HTML page
        return "error/500";
    }
}