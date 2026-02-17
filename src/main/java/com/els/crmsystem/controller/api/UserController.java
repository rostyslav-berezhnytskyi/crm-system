package com.els.crmsystem.controller.api;

import com.els.crmsystem.dto.input.UserInputDto;
import com.els.crmsystem.dto.output.UserOutputDto;
import com.els.crmsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public String registerUser(@ModelAttribute UserInputDto userDto, Model model) {
        try {
            userService.registerUser(userDto); // Pass the whole object!
            return "redirect:/register?success";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserOutputDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }
}
