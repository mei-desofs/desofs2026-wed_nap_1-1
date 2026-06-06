package com.example.desofs.services;

import com.example.desofs.shared.dtos.MovieDTO;

import java.util.List;

/**
 * Interface for movie-related operations.
 */
public interface IMovieService {
    List<MovieDTO> listAll();

    MovieDTO get(Long id);

    MovieDTO create(MovieDTO movieDTO);

    void delete(Long id);

    MovieDTO update(Long id, MovieDTO movieDTO);
}
