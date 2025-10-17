package com.example.platforme_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GardienStatsRepository extends JpaRepository<GardienStats, Long> {
    Optional<GardienStats> findByJoueurId(Long joueurId);
}