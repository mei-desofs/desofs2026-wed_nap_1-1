package com.example.desofs.controllers;

import com.example.desofs.domain.Role;
import com.example.desofs.security.IRoleGuard;
import com.example.desofs.services.IMovieService;
import com.example.desofs.services.IAuditLogService;
import com.example.desofs.shared.dtos.MovieDTO;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.util.List;

/**
 * REST controller exposing movie catalog operations.
 * <p>
 * Supports listing movies, retrieving a single movie by id, and creating new
 * movie records.
 */
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    /** Logger for request tracing. */
    private static final Logger logger = LoggerFactory.getLogger(MovieController.class);

    /** Service responsible for movie persistence and business logic. */
    private final IMovieService movieService;

    /** Service responsible for recording audit log entries. */
    private final IAuditLogService auditLogService;

    /** Guard that enforces role-based access checks. */
    private final IRoleGuard roleGuard;

    /**
     * Constructs the controller with the required service.
     *
     * @param movieService service used to manage movies
     * @param roleGuard service used to guard role-based access
     */

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring-managed dependency injection"
    )
    public MovieController(IMovieService movieService, IAuditLogService auditLogService, IRoleGuard roleGuard) {
        this.movieService = movieService;
        this.auditLogService = auditLogService;
        this.roleGuard = roleGuard;
    }

    /**
     * Returns the list of all movies available in the catalog.
     *
     * @return list of {@link MovieDTO}
     */
    @GetMapping
    public List<MovieDTO> list(@AuthenticationPrincipal Jwt jwt) {
        String auth0Id = jwt.getSubject();
        logger.info("User {} requested movie list", auth0Id);

        roleGuard.requireRole(jwt, Role.ADMIN);

        auditLogService.log(jwt.getSubject(), jwt.getSubject(), Role.ADMIN, "GET_MOVIE_LIST");
        return movieService.listAll();
    }

    /**
     * Retrieves a single movie by its identifier.
     *
     * @param id movie identifier
     * @return {@link ResponseEntity} containing the movie when found, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<MovieDTO> get(@PathVariable Long id) {
        MovieDTO movie = movieService.get(id);
        if (movie == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(movie);
    }

    /**
     * Creates a new movie record.
     *
     * @param movie movie payload
     * @return 201 Created with location header pointing to the new resource
     */
    @PostMapping
    public ResponseEntity<MovieDTO> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MovieDTO movie) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        movie.setId(null);
        MovieDTO created = movieService.create(movie);
        auditLogService.log(jwt.getSubject(), jwt.getSubject(), Role.ADMIN, "CREATE_MOVIE");
        return ResponseEntity.created(URI.create("/api/movies/" + created.getId())).body(created);
    }

    /**
     * Updates an existing movie record by its identifier.
     * Only users with ADMIN role can perform this operation.
     * @param jwt the authenticated user's JWT token, used for role checking and audit logging
     * @param id the identifier of the movie to update
     * @param movie the updated movie data
     * @return the updated movie if successful, 404 Not Found if the movie does not exist, or 403 Forbidden if the user lacks permissions
     */
    @PutMapping("/{id}")
    public ResponseEntity<MovieDTO> update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody MovieDTO movie) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        
        MovieDTO updated = movieService.update(id, movie);

        if (updated == null) return ResponseEntity.notFound().build();

        auditLogService.log(jwt.getSubject(), jwt.getSubject(), Role.ADMIN, "UPDATE_MOVIE");
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a movie by its identifier.
     * Only users with ADMIN role can perform this operation.
     * 
     * @param jwt the authenticated user's JWT token, used for role checking and audit logging
     * @param id the identifier of the movie to delete
     * @return 204 No Content if deletion was successful, or 403 Forbidden if the user lacks permissions
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        roleGuard.requireRole(jwt, Role.ADMIN);

        movieService.delete(id);
        auditLogService.log(jwt.getSubject(), jwt.getSubject(), Role.ADMIN, "DELETE_MOVIE");
        return ResponseEntity.noContent().build();
    }
}
