package com.els.crmsystem.controller.web;

import com.els.crmsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class WebUserController {

    private final UserService userService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')") // Double security check
    public String showUsersPage(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users";
    }
}
