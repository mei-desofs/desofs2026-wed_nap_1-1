package com.example.desofs.shared.mappers;

import com.example.desofs.domain.Movie;
import com.example.desofs.shared.dtos.MovieDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MovieMapper Unit Tests")
class MovieMapperTest {

    @Test
    void toDTO_mapsAllMovieFields() {
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);
        movie.setId(1L);

        MovieDTO dto = new MovieMapper().toDTO(movie);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Inception");
        assertThat(dto.getDescription()).isEqualTo("A mind-bending thriller");
        assertThat(dto.getGenre()).isEqualTo("Sci-Fi");
        assertThat(dto.getPlatform()).isEqualTo("Blu-ray");
        assertThat(dto.getPrice()).isEqualByComparingTo("14.99");
        assertThat(dto.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void toEntity_mapsAllMovieFields() {
        MovieDTO dto = new MovieDTO(2L, "Avatar", "Blue aliens", "Action", "4K", new BigDecimal("19.99"), 15);

        Movie movie = new MovieMapper().toEntity(dto);

        assertThat(movie.getId()).isEqualTo(2L);
        assertThat(movie.getTitle()).isEqualTo("Avatar");
        assertThat(movie.getDescription()).isEqualTo("Blue aliens");
        assertThat(movie.getGenre()).isEqualTo("Action");
        assertThat(movie.getPlatform()).isEqualTo("4K");
        assertThat(movie.getPrice()).isEqualByComparingTo("19.99");
        assertThat(movie.getStockQuantity()).isEqualTo(15);
    }
}
