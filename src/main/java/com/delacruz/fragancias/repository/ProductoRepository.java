package com.delacruz.fragancias.repository;

import com.delacruz.fragancias.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findAllByActivoTrueOrderByNombreAsc();
}
