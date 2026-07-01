package com.delacruz.fragancias.repository;

import com.delacruz.fragancias.entity.EstadoPedido;
import com.delacruz.fragancias.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findAllByOrderByFechaRegistroDesc();
    List<Pedido> findByEstadoOrderByFechaRegistroAsc(EstadoPedido estado);
    List<Pedido> findByIdIn(Collection<Long> ids);
    Optional<Pedido> findFirstByEstadoOrderByFechaRegistroAsc(EstadoPedido estado);
    long countByEstado(EstadoPedido estado);
}
