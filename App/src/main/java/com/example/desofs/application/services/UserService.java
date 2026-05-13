package com.example.desofs.application.services;

import com.example.desofs.domain.entities.User;
import com.example.desofs.domain.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> listAll() {
        return userRepository.findAll();
    }

    public Optional<User> get(Long id) {
        return userRepository.findById(id);
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User create(User u) {
        return userRepository.save(u);
    }

    public User update(Long id, User u) {
        return userRepository.findById(id).map(existing -> {
            if (u.getEmail() != null) existing.setEmail(u.getEmail());
            if (u.getName() != null) existing.setName(u.getName());
            return userRepository.save(existing);
        }).orElse(null);
    }
}
