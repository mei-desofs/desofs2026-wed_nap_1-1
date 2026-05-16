package com.example.desofs.services;

import com.example.desofs.domain.Movie;
import com.example.desofs.repositories.MovieRepository;
import com.example.desofs.shared.mappers.IMovieMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.desofs.shared.dtos.MovieDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MovieService Unit Tests")
class MovieServiceTests {

    @Mock
    private MovieRepository movieRepository;
    @Mock
    private IMovieMapper movieMapper;

    @InjectMocks
    private MovieService movieService;

    private Movie testMovie;
    private MovieDTO testMovieDTO;


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
        testMovieDTO = new MovieDTO(
            1L,
            "Inception",
            "A mind-bending thriller",
            "Sci-Fi",
            "Blu-ray",
            new BigDecimal("14.99"),
            10
        );
    }

    private void setupMovieToDtoMapper() {
        when(movieMapper.toDTO(any(Movie.class))).thenAnswer(inv -> {
            Movie m = inv.getArgument(0);
            return new MovieDTO(m.getId(), m.getTitle(), m.getDescription(), m.getGenre(), m.getPlatform(), m.getPrice(), m.getStockQuantity());
        });
    }

    private void setupMovieToEntityMapper() {
        when(movieMapper.toEntity(any(MovieDTO.class))).thenAnswer(inv -> {
            MovieDTO dto = inv.getArgument(0);
            Movie m = new Movie(dto.getTitle(), dto.getDescription(), dto.getGenre(), dto.getPlatform(), dto.getPrice(), dto.getStockQuantity());
            m.setId(dto.getId());
            return m;
        });
    }

    // OPAQUE-BOX TESTS : Focus on external behavior (public API, outputs)
    @Nested
    @DisplayName("Opaque-box (behavior) tests")
    class OpaqueBoxTests {

        @Test
        @DisplayName("listAll() should return all movies from repository")
        void testListAll_ReturnsAllMovies() {
            setupMovieToDtoMapper();
            List<Movie> movies = List.of(testMovie);
            when(movieRepository.findAll()).thenReturn(movies);

            List<MovieDTO> result = movieService.listAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0))
                .extracting(MovieDTO::getId, MovieDTO::getTitle, MovieDTO::getDescription, MovieDTO::getGenre,
                    MovieDTO::getPlatform, MovieDTO::getPrice, MovieDTO::getStockQuantity)
                .containsExactly(1L, "Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray",
                    new BigDecimal("14.99"), 10);
        }

        @Test
        @DisplayName("listAll() should return empty list when no movies exist")
        void testListAll_WithEmptyDatabase_ReturnsEmptyList() {
            when(movieRepository.findAll()).thenReturn(List.of());

            List<MovieDTO> result = movieService.listAll();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("get(id) should return movie when exists")
        void testGet_WithValidId_ReturnsMovie() {
            setupMovieToDtoMapper();
            when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));

            MovieDTO result = movieService.get(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Inception");
        }

        @Test
        @DisplayName("get(id) should return null when movie not found")
        void testGet_WithInvalidId_ReturnsNull() {
            when(movieRepository.findById(999L)).thenReturn(Optional.empty());

            MovieDTO result = movieService.get(999L);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("create() should persist movie and return with ID")
        void testCreate_WithValidMovie_PersistsAndReturns() {
            setupMovieToEntityMapper();
            setupMovieToDtoMapper();
            MovieDTO newMovie = new MovieDTO(null, "Avatar", "Blue aliens", "Action", "4K",
                new BigDecimal("19.99"), 15);
            Movie savedMovie = new Movie("Avatar", "Blue aliens", "Action", "4K",
                new BigDecimal("19.99"), 15);
            savedMovie.setId(2L);

            when(movieRepository.save(any(Movie.class))).thenReturn(savedMovie);

            MovieDTO result = movieService.create(newMovie);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getTitle()).isEqualTo("Avatar");
        }

        @Test
        @DisplayName("create() should preserve all movie attributes")
        void testCreate_PreservesAllAttributes() {
            setupMovieToEntityMapper();
            setupMovieToDtoMapper();
            MovieDTO movie = new MovieDTO(null, "Dune", "Epic sci-fi", "Sci-Fi", "IMAX",
                new BigDecimal("16.99"), 8);
            Movie savedMovie = new Movie("Dune", "Epic sci-fi", "Sci-Fi", "IMAX",
                new BigDecimal("16.99"), 8);
            savedMovie.setId(3L);

            when(movieRepository.save(any(Movie.class))).thenReturn(savedMovie);

            MovieDTO result = movieService.create(movie);

            assertThat(result)
                    .extracting("title", "description", "genre", "platform", "stockQuantity")
                    .containsExactly("Dune", "Epic sci-fi", "Sci-Fi", "IMAX", 8);
            assertThat(result.getPrice()).isEqualByComparingTo("16.99");
        }
    }

    // TRANSPARENT-BOX TESTS : inspect internal interactions and implementation details
    @Nested
    @DisplayName("Transparent-box (implementation) tests")
    class TransparentBoxTests {

        @Test
        @DisplayName("get(id) should fetch from repository exactly once")
        void testGet_CallsRepositoryOnce() {
            setupMovieToDtoMapper();
            when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));

            movieService.get(1L);

            verify(movieRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("create() should call repository save exactly once")
        void testCreate_CallsRepositoryOnce() {
            setupMovieToEntityMapper();
            setupMovieToDtoMapper();
            MovieDTO movie = new MovieDTO(4L, "Test", "Desc", "Genre", "Format",
                new BigDecimal("9.99"), 1);

            when(movieRepository.save(any(Movie.class))).thenReturn(testMovie);

            movieService.create(movie);

            verify(movieRepository, times(1)).save(any(Movie.class));
        }
    }
}
