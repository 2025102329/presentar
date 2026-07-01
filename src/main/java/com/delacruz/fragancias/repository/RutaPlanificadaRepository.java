package com.delacruz.fragancias.repository;

import com.delacruz.fragancias.entity.RutaPlanificada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RutaPlanificadaRepository extends JpaRepository<RutaPlanificada, Long> {
    Optional<RutaPlanificada> findFirstByOrderByFechaDesc();
}
