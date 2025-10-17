package com.example.platforme_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface MilieuStatsRepository extends JpaRepository<MilieuStats, Long> {
    Optional<MilieuStats> findByJoueurId(Long joueurId);
}
