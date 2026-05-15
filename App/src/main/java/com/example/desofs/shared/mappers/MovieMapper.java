package com.example.desofs.shared.mappers;

import com.example.desofs.domain.Movie;
import com.example.desofs.shared.dtos.MovieDTO;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link IMovieMapper} using simple field
 * copying between domain and DTO models.
 */
@Component
public class MovieMapper implements IMovieMapper {
	/**
	 * Default constructor for Spring component wiring.
	 */
	public MovieMapper() {
	}

	/**
	 * Converts a domain {@link Movie} to its DTO representation.
	 *
	 * @param movie domain movie entity
	 * @return corresponding {@link MovieDTO}
	 */
	@Override
	public MovieDTO toDTO(Movie movie) {
		return new MovieDTO(
				movie.getId(),
				movie.getTitle(),
				movie.getDescription(),
				movie.getGenre(),
				movie.getPlatform(),
				movie.getPrice(),
				movie.getStockQuantity()
		);
	}

	/**
	 * Converts a {@link MovieDTO} transport object into a domain {@link Movie}.
	 *
	 * @param movieDTO transport DTO containing movie data
	 * @return domain {@link Movie} populated with values from the DTO
	 */
	@Override
	public Movie toEntity(MovieDTO movieDTO) {
		Movie movie = new Movie(
				movieDTO.getTitle(),
				movieDTO.getDescription(),
				movieDTO.getGenre(),
				movieDTO.getPlatform(),
				movieDTO.getPrice(),
				movieDTO.getStockQuantity()
		);
		movie.setId(movieDTO.getId());
		return movie;
	}
}
