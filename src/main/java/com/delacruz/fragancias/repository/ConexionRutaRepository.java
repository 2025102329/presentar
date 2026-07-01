package com.delacruz.fragancias.repository;

import com.delacruz.fragancias.entity.ConexionRuta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConexionRutaRepository extends JpaRepository<ConexionRuta, Long> {
    List<ConexionRuta> findAllByOrderByOrigenAscDestinoAsc();
}
