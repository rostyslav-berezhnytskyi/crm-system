package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.UserInputDto;
import com.els.crmsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class WebAuthController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // FIX: We must send an empty object to the form!
        // The DTO has 4 fields: username, password, email, phoneNumber
        model.addAttribute("user", new UserInputDto(null, null, null, null));
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") @Valid UserInputDto userDto,
                               BindingResult bindingResult,
                               Model model) {

        // 1. Check for Validation Errors (e.g., empty password)
        if (bindingResult.hasErrors()) {
            return "register"; // Reload page to show errors
        }

        try {
            userService.registerUser(userDto);
            return "redirect:/login?registered"; // Success! Go to login.
        } catch (RuntimeException e) {
            // 2. Handle "User already exists" error
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}