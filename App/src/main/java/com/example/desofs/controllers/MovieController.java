package com.example.desofs.controllers;

import com.example.desofs.services.MovieService;
import com.example.desofs.domain.Movie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/movies")

/**
 * REST controller exposing movie catalog operations.
 * <p>
 * Supports listing movies, retrieving a single movie by id, and creating new
 * movie records.
 */
public class MovieController {
    
    /** Service responsible for movie persistence and business logic. */
    private final MovieService movieService;

    /**
     * Constructs the controller with the required service.
     *
     * @param movieService service used to manage movies
     */
    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Returns the list of all movies available in the catalog.
     *
     * @return list of {@link Movie}
     */
    @GetMapping
    public List<Movie> list() {
        return movieService.listAll();
    }

    /**
     * Retrieves a single movie by its identifier.
     *
     * @param id movie identifier
     * @return {@link ResponseEntity} containing the movie when found, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Movie> get(@PathVariable Long id) {
        Movie m = movieService.get(id);
        if (m == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(m);
    }

    /**
     * Creates a new movie record.
     *
     * @param m movie payload
     * @return 201 Created with location header pointing to the new resource
     */
    @PostMapping
    public ResponseEntity<Movie> create(@RequestBody Movie m) {
        Movie created = movieService.create(m);
        return ResponseEntity.created(URI.create("/api/movies/" + created.getId())).body(created);
    }
}
