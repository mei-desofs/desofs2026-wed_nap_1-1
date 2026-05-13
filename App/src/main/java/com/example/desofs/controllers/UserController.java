package com.example.desofs.controllers;

import com.example.desofs.application.dtos.UserDTO;
import com.example.desofs.application.services.UserService;
import com.example.desofs.domain.entities.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDTO> list() {
        return userService.listAll().stream()
            .map(u -> new UserDTO(u.getId(), u.getEmail(), u.getName()))
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> get(@PathVariable Long id) {
        User u = userService.get(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new UserDTO(u.getId(), u.getEmail(), u.getName()));
    }

    @PostMapping
    public ResponseEntity<UserDTO> create(@RequestBody User u) {
        User created = userService.create(u);
        UserDTO dto = new UserDTO(created.getId(), created.getEmail(), created.getName());
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable Long id, @RequestBody User u) {
        User updated = userService.update(id, u);
        if (updated == null) return ResponseEntity.notFound().build();
        UserDTO dto = new UserDTO(updated.getId(), updated.getEmail(), updated.getName());
        return ResponseEntity.ok(dto);
    }
}
