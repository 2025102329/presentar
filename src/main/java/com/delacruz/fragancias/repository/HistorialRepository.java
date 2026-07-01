package com.delacruz.fragancias.repository;

import com.delacruz.fragancias.entity.Historial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistorialRepository extends JpaRepository<Historial, Long> {
    List<Historial> findAllByOrderByFechaDesc();
    Optional<Historial> findFirstByOrderByFechaDesc();
}
