package com.example.desofs.controllers;

import com.example.desofs.services.MovieService;
import com.example.desofs.domain.Movie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<Movie> list() {
        return movieService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> get(@PathVariable Long id) {
        Movie m = movieService.get(id);
        if (m == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(m);
    }

    @PostMapping
    public ResponseEntity<Movie> create(@RequestBody Movie m) {
        Movie created = movieService.create(m);
        return ResponseEntity.created(URI.create("/api/movies/" + created.getId())).body(created);
    }
}
