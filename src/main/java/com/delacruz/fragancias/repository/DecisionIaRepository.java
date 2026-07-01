package com.delacruz.fragancias.repository;

import com.delacruz.fragancias.entity.DecisionIa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DecisionIaRepository extends JpaRepository<DecisionIa, Long> {
    List<DecisionIa> findTop50ByOrderByFechaDesc();
    Optional<DecisionIa> findFirstByOrderByFechaDesc();
}
