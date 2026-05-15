package com.example.desofs.controllers;

import com.example.desofs.services.IMovieService;
import com.example.desofs.shared.dtos.MovieDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Constructs the controller with the required service.
     *
     * @param movieService service used to manage movies
     */
    public MovieController(IMovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Returns the list of all movies available in the catalog.
     *
     * @return list of {@link MovieDTO}
     */
    @GetMapping
    public List<MovieDTO> list() {
        return movieService.listAll();
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
    public ResponseEntity<MovieDTO> create(@RequestBody MovieDTO movie) {
        MovieDTO created = movieService.create(movie);
        return ResponseEntity.created(URI.create("/api/movies/" + created.getId())).body(created);
    }
}
