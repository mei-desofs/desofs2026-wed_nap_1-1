package com.example.desofs.controllers;

import com.example.desofs.domain.Role;
import com.example.desofs.security.IRoleGuard;
import com.example.desofs.services.IMovieService;
import com.example.desofs.shared.dtos.MovieDTO;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.util.List;

/**
 * REST controller exposing movie catalog operations.
 * <p>
 * Supports listing movies, retrieving a single movie by id, and creating new
 * movie records.
 */
@RestController
@RequestMapping("/api/movies")
public class MovieController {
    

    /** Service responsible for movie persistence and business logic. */
    private final IMovieService movieService;
    private final IRoleGuard roleGuard;

    /**
     * Constructs the controller with the required service.
     *
     * @param movieService service used to manage movies
     * @param roleGuard service used to guard role-based access
     */
    public MovieController(IMovieService movieService, IRoleGuard roleGuard) {
        this.movieService = movieService;
        this.roleGuard = roleGuard;
    }

    /**
     * Returns the list of all movies available in the catalog.
     *
     * @return list of {@link MovieDTO}
     */
    @GetMapping
    public List<MovieDTO> list() {
        try {
            return movieService.listAll();
        } catch (Throwable ex) {
            return List.of();
        }
    }

    /**
     * Retrieves a single movie by its identifier.
     *
     * @param id movie identifier
     * @return {@link ResponseEntity} containing the movie when found, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<MovieDTO> get(@PathVariable Long id) {
        MovieDTO movie = movieService.get(id);
        if (movie == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(movie);
    }

    /**
     * Creates a new movie record.
     *
     * @param movie movie payload
     * @return 201 Created with location header pointing to the new resource
     */
    @PostMapping
    public ResponseEntity<MovieDTO> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MovieDTO movie) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        movie.setId(null);
        MovieDTO created = movieService.create(movie);
        return ResponseEntity.created(URI.create("/api/movies/" + created.getId())).body(created);
    }
}
