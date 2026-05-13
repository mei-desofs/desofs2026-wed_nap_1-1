package com.example.desofs.services;

import com.example.desofs.domain.Movie;
import com.example.desofs.repositories.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing movie records.
 *
 * <p>Provides basic operations to list, retrieve, and persist movies through
 * the repository layer.</p>
 */
@Service
public class MovieService {
    /** Repository used to access movie persistence operations. */
    private final MovieRepository movieRepository;

    /**
     * Creates the service with the required repository dependency.
     *
     * @param movieRepository repository used to access movie data
     */
    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Returns all movies stored in the database.
     *
     * @return list of movies
     */
    public List<Movie> listAll() {
        return movieRepository.findAll();
    }

    /**
     * Retrieves a movie by its identifier.
     *
     * @param id movie identifier
     * @return the movie when found, otherwise {@code null}
     */
    public Movie get(Long id) {
        return movieRepository.findById(id).orElse(null);
    }

    /**
     * Persists a movie entity.
     *
     * @param m movie to save
     * @return saved movie entity
     */
    public Movie create(Movie m) {
        return movieRepository.save(m);
    }
}
