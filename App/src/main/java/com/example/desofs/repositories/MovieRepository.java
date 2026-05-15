package com.example.desofs.repositories;

import com.example.desofs.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisting and retrieving {@link Movie} entities.
 */
public interface MovieRepository extends JpaRepository<Movie, Long> {
}
