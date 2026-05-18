package com.example.desofs.shared.mappers;

import com.example.desofs.domain.Movie;
import com.example.desofs.shared.dtos.MovieDTO;

/**
 * Mapper interface responsible for converting between {@link Movie}
 * domain objects and {@link MovieDTO} transport objects.
 */
public interface IMovieMapper {

	/**
	 * Converts a {@link Movie} domain entity to its DTO representation.
	 *
	 * @param movie domain movie entity
	 * @return corresponding {@link MovieDTO}
	 */
	MovieDTO toDTO(Movie movie);

	/**
	 * Converts a {@link MovieDTO} to a {@link Movie} domain entity.
	 *
	 * @param movieDTO transport DTO
	 * @return domain {@link Movie}
	 */
	Movie toEntity(MovieDTO movieDTO);
}
