package com.example.desofs.services;

import com.example.desofs.domain.Movie;
import com.example.desofs.repositories.MovieRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MovieService.
 * <p>
 * Covers:
 * - listAll(): Retrieves all movies
 * - get(id): Retrieves a single movie
 * - create(movie): Persists a new movie
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MovieService Unit Tests")
class MovieServiceTests {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private Movie testMovie;

    @BeforeEach
    void setUp() {
        testMovie = new Movie(
                "Inception",
                "A mind-bending thriller",
                "Sci-Fi",
                "Blu-ray",
                new BigDecimal("14.99"),
                10
        );
        testMovie.setId(1L);
    }

    // ============ listAll() Tests ============

    @Test
    @DisplayName("listAll() should return all movies from repository")
    void testListAll_ReturnsAllMovies() {
        // Arrange
        List<Movie> movies = List.of(testMovie);
        when(movieRepository.findAll()).thenReturn(movies);

        // Act
        List<Movie> result = movieService.listAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(testMovie);
        verify(movieRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("listAll() should return empty list when no movies exist")
    void testListAll_WithEmptyDatabase_ReturnsEmptyList() {
        // Arrange
        when(movieRepository.findAll()).thenReturn(List.of());

        // Act
        List<Movie> result = movieService.listAll();

        // Assert
        assertThat(result).isEmpty();
        verify(movieRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("listAll() should return multiple movies")
    void testListAll_WithMultipleMovies_ReturnsAll() {
        // Arrange
        Movie movie2 = new Movie("Matrix", "Reality is a construct", "Sci-Fi", "DVD",
            new BigDecimal("12.99"), 5);
        movie2.setId(2L);

        List<Movie> movies = List.of(testMovie, movie2);
        when(movieRepository.findAll()).thenReturn(movies);

        // Act
        List<Movie> result = movieService.listAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Movie::getTitle)
                .containsExactly("Inception", "Matrix");
    }

    // ============ get(id) Tests ============

    @Test
    @DisplayName("get(id) should return movie when exists")
    void testGet_WithValidId_ReturnsMovie() {
        // Arrange
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));

        // Act
        Movie result = movieService.get(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Inception");
        verify(movieRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("get(id) should return null when movie not found")
    void testGet_WithInvalidId_ReturnsNull() {
        // Arrange
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Movie result = movieService.get(999L);

        // Assert
        assertThat(result).isNull();
        verify(movieRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("get(id) should fetch from repository exactly once")
    void testGet_CallsRepositoryOnce() {
        // Arrange
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));

        // Act
        movieService.get(1L);

        // Assert
        verify(movieRepository, times(1)).findById(1L);
    }

    // ============ create(movie) Tests ============

    @Test
    @DisplayName("create() should persist movie and return with ID")
    void testCreate_WithValidMovie_PersistsAndReturns() {
        // Arrange
        Movie newMovie = new Movie("Avatar", "Blue aliens", "Action", "4K",
            new BigDecimal("19.99"), 15);
        Movie savedMovie = new Movie("Avatar", "Blue aliens", "Action", "4K",
            new BigDecimal("19.99"), 15);
        savedMovie.setId(2L);

        when(movieRepository.save(newMovie)).thenReturn(savedMovie);

        // Act
        Movie result = movieService.create(newMovie);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getTitle()).isEqualTo("Avatar");
        verify(movieRepository, times(1)).save(newMovie);
    }

    @Test
    @DisplayName("create() should preserve all movie attributes")
    void testCreate_PreservesAllAttributes() {
        // Arrange
        Movie movie = new Movie("Dune", "Epic sci-fi", "Sci-Fi", "IMAX",
            new BigDecimal("16.99"), 8);
        Movie savedMovie = new Movie("Dune", "Epic sci-fi", "Sci-Fi", "IMAX",
            new BigDecimal("16.99"), 8);
        savedMovie.setId(3L);

        when(movieRepository.save(movie)).thenReturn(savedMovie);

        // Act
        Movie result = movieService.create(movie);

        // Assert
        assertThat(result)
                .extracting("title", "description", "genre", "platform", "stockQuantity")
                .containsExactly("Dune", "Epic sci-fi", "Sci-Fi", "IMAX", 8);
        assertThat(result.getPrice()).isEqualByComparingTo("16.99");
    }

    @Test
    @DisplayName("create() should call repository save exactly once")
    void testCreate_CallsRepositoryOnce() {
        // Arrange
        Movie movie = new Movie("Test", "Desc", "Genre", "Format",
            new BigDecimal("9.99"), 1);
        movie.setId(4L);

        when(movieRepository.save(movie)).thenReturn(movie);

        // Act
        movieService.create(movie);

        // Assert
        verify(movieRepository, times(1)).save(movie);
    }
}

