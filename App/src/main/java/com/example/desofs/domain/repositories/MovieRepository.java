package com.example.desofs.domain.repositories;

import com.example.desofs.domain.entities.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
