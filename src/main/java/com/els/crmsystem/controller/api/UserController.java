package com.els.crmsystem.controller.api;

import com.els.crmsystem.dto.input.UserInputDto;
import com.els.crmsystem.dto.output.UserOutputDto;
import com.els.crmsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // API Endpoint: Receives JSON -> Returns JSON Message
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserInputDto userDto) {
        userService.registerUser(userDto);
        return ResponseEntity.ok("User registered successfully!");
    }

    // API Endpoint: Returns JSON Data
    @GetMapping("/{id}")
    public ResponseEntity<UserOutputDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }
}