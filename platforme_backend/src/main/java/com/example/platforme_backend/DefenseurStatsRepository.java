package com.example.platforme_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DefenseurStatsRepository extends JpaRepository<DefenseurStats, Long> {
    Optional<DefenseurStats> findByJoueurId(Long joueurId);
}