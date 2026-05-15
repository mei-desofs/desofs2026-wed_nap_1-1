package com.example.desofs.services;

import com.example.desofs.domain.Movie;
import com.example.desofs.repositories.MovieRepository;
import com.example.desofs.shared.dtos.MovieDTO;
import com.example.desofs.shared.mappers.IMovieMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing movie records.
 *
 * <p>Provides basic operations to list, retrieve, and persist movies through
 * the repository layer.</p>
 */
@Service
public class MovieService implements IMovieService {
    /** Repository used to access movie persistence operations. */
    private final MovieRepository movieRepository;
    private final IMovieMapper movieMapper;

    /**
     * Creates the service with the required repository and mapper dependencies.
     *
     * @param movieRepository repository used to access movie data
     * @param movieMapper mapper used to convert between domain and DTO
     */
    public MovieService(MovieRepository movieRepository, IMovieMapper movieMapper) {
        this.movieRepository = movieRepository;
        this.movieMapper = movieMapper;
    }

    /**
     * Returns all movies stored in the database.
     *
     * @return list of movies
     */
    public List<MovieDTO> listAll() {
        return movieRepository.findAll().stream()
            .map(movieMapper::toDTO)
            .toList();
    }

    /**
     * Retrieves a movie by its identifier.
     *
     * @param id movie identifier
     * @return the movie when found, otherwise {@code null}
     */
    public MovieDTO get(Long id) {
        return movieRepository.findById(id)
            .map(movieMapper::toDTO)
            .orElse(null);
    }

    /**
     * Persists a movie entity.
     *
     * @param movieDTO movie to save
     * @return saved movie entity
     */
    public MovieDTO create(MovieDTO movieDTO) {
        Movie savedMovie = movieRepository.save(movieMapper.toEntity(movieDTO));
        return movieMapper.toDTO(savedMovie);
    }
}
