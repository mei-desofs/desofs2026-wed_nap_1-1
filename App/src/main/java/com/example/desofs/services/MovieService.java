package com.example.desofs.services;

import com.example.desofs.domain.Movie;
import com.example.desofs.repositories.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {
    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<Movie> listAll() {
        return movieRepository.findAll();
    }

    public Movie get(Long id) {
        return movieRepository.findById(id).orElse(null);
    }

    public Movie create(Movie m) {
        return movieRepository.save(m);
    }
}
