package com.delacruz.fragancias.repository;

import com.delacruz.fragancias.entity.Cliente;
import com.delacruz.fragancias.entity.LadoArbol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findFirstByLado(LadoArbol lado);
    Optional<Cliente> findByPadreIdAndLado(Long padreId, LadoArbol lado);
    List<Cliente> findAllByOrderByFechaRegistroAsc();
    boolean existsByDniIgnoreCase(String dni);
}
