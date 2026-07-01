package com.delacruz.fragancias.service;

import com.delacruz.fragancias.dto.PedidoRequest;
import com.delacruz.fragancias.dto.PedidoResponse;
import com.delacruz.fragancias.entity.*;
import com.delacruz.fragancias.exception.NoEncontradoException;
import com.delacruz.fragancias.exception.ReglaNegocioException;
import com.delacruz.fragancias.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Maneja pedidos, stock y la cola FIFO. */
@Service
public class PedidoService {

    private final PedidoRepository repository;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final HistorialService historialService;

    public PedidoService(
            PedidoRepository repository,
            ClienteService clienteService,
            ProductoService productoService,
            HistorialService historialService
    ) {
        this.repository = repository;
        this.clienteService = clienteService;
        this.productoService = productoService;
        this.historialService = historialService;
    }

    public List<PedidoResponse> listar() {
        return repository.findAllByOrderByFechaRegistroDesc().stream().map(this::mapear).toList();
    }

    public List<PedidoResponse> colaFifo() {
        return repository.findByEstadoOrderByFechaRegistroAsc(EstadoPedido.PENDIENTE)
                .stream().map(this::mapear).toList();
    }

    @Transactional
    public PedidoResponse registrar(PedidoRequest request) {
        Cliente cliente = clienteService.obtenerEntidad(request.clienteId());
        Producto producto = productoService.obtenerEntidad(request.productoId());

        if (!producto.isActivo()) {
            throw new ReglaNegocioException("El perfume seleccionado ya no está disponible.");
        }
        if (producto.getStock() < request.cantidad()) {
            throw new ReglaNegocioException("Stock insuficiente. Quedan " + producto.getStock() + " unidades.");
        }

        Pedido pedido = new Pedido();
        pedido.setCodigo("TMP-" + UUID.randomUUID().toString().substring(0, 8));
        pedido.setCliente(cliente);
        pedido.setProducto(producto);
        pedido.setCantidad(request.cantidad());
        pedido.setPrecioUnitario(producto.getPrecio());
        pedido.setTotal(producto.getPrecio().multiply(BigDecimal.valueOf(request.cantidad())));
        pedido.setPrioridad(request.prioridad());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setDireccionEntrega(cliente.getDireccion());
        pedido.setZonaEntrega(cliente.getZona());
        repository.save(pedido);
        pedido.setCodigo("PED-%04d".formatted(pedido.getId()));

        producto.setStock(producto.getStock() - request.cantidad());
        repository.save(pedido);

        historialService.apilar("PEDIDO",
                "Se registró " + pedido.getCodigo() + " para " + cliente.getAbreviatura() + ".");
        return mapear(pedido);
    }

    /** El primer pedido que entró es el primero que se prepara: FIFO puro. */
    @Transactional
    public PedidoResponse atenderSiguiente() {
        Pedido pedido = repository.findFirstByEstadoOrderByFechaRegistroAsc(EstadoPedido.PENDIENTE)
                .orElseThrow(() -> new ReglaNegocioException("No hay pedidos pendientes en la cola FIFO."));
        pedido.setEstado(EstadoPedido.LISTO_PARA_ENVIO);
        repository.save(pedido);
        historialService.apilar("FIFO",
                "Se atendió primero " + pedido.getCodigo() + " por ser el más antiguo de la cola.");
        return mapear(pedido);
    }

    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido estado) {
        Pedido pedido = obtenerEntidad(id);
        pedido.setEstado(estado);
        repository.save(pedido);
        historialService.apilar("PEDIDO",
                pedido.getCodigo() + " cambió al estado " + estado.name().replace('_', ' ') + ".");
        return mapear(pedido);
    }

    public Pedido obtenerEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No se encontró el pedido solicitado."));
    }

    public List<Pedido> pedidosParaRuta(List<Long> ids) {
        List<Pedido> pedidos;
        if (ids == null || ids.isEmpty()) {
            pedidos = repository.findByEstadoOrderByFechaRegistroAsc(EstadoPedido.LISTO_PARA_ENVIO);
        } else {
            pedidos = repository.findByIdIn(ids);
        }

        List<Pedido> validos = pedidos.stream()
                .filter(p -> p.getEstado() == EstadoPedido.LISTO_PARA_ENVIO)
                .toList();
        if (validos.isEmpty()) {
            throw new ReglaNegocioException("No hay pedidos listos para generar una ruta.");
        }
        return validos;
    }

    @Transactional
    public void marcarEnRuta(List<Pedido> pedidos) {
        pedidos.forEach(pedido -> pedido.setEstado(EstadoPedido.EN_RUTA));
        repository.saveAll(pedidos);
    }

    private PedidoResponse mapear(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(), pedido.getCodigo(),
                pedido.getCliente().getId(), pedido.getCliente().getCodigo(),
                pedido.getCliente().getNombreCompleto(), pedido.getCliente().getAbreviatura(),
                pedido.getProducto().getId(), pedido.getProducto().getNombre(), pedido.getProducto().getMarca(),
                pedido.getCantidad(), pedido.getPrecioUnitario(), pedido.getTotal(),
                pedido.getPrioridad(), pedido.getEstado(), pedido.getDireccionEntrega(),
                pedido.getZonaEntrega(), pedido.getFechaRegistro()
        );
    }
}
