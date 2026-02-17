package com.els.crmsystem.controller.web;

import com.els.crmsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller; // Note: Standard @Controller
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller // This means: "I return HTML pages"
@RequiredArgsConstructor
public class WebAuthController {

    private final UserService userService;

    // 1. Show the Page (Browser: GET /register)
    @GetMapping("/register")
    public String showRegistrationForm() {
        // This tells Spring: "Go look for src/main/resources/templates/register.html"
        return "register";
    }

    // 2. Handle the Form Submit (Browser: POST /register)
//    @PostMapping("/register")
//    public String registerUser(@RequestParam String username,
//                               @RequestParam String email,
//                               @RequestParam String password,
//                               @RequestParam String phoneNumber, Model model) {
//        try {
//            // Call your Service (The logic you already wrote)
//            userService.registerUser(username, password, email, phoneNumber);
//
//            // If successful, redirect to same page with a success flag
//            return "redirect:/register?success";
//        } catch (RuntimeException e) {
//            // If error (e.g., duplicate user), reload page with error message
//            model.addAttribute("error", e.getMessage());
//            return "register";
//        }
//    }
}
