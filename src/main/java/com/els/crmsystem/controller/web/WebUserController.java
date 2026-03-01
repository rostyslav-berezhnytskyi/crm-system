package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.UserEditDto;
import com.els.crmsystem.enums.Role;
import com.els.crmsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/users/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditUserPage(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        model.addAttribute("roles", Role.values()); // Passes ADMIN, DIRECTOR, etc. to the dropdown
        return "edit-user";
    }

    @PostMapping("/users/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String processEditUser(@PathVariable Long id,
                                  @ModelAttribute UserEditDto dto,
                                  @RequestParam(value = "photo", required = false) MultipartFile photo) {
        userService.adminUpdateUser(id, dto, photo);
        return "redirect:/users";
    }
}
