package com.delacruz.fragancias.service;

import com.delacruz.fragancias.dto.ProductoRequest;
import com.delacruz.fragancias.dto.ProductoResponse;
import com.delacruz.fragancias.entity.Producto;
import com.delacruz.fragancias.exception.NoEncontradoException;
import com.delacruz.fragancias.repository.ProductoRepository;
import com.delacruz.fragancias.util.NombreUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductoService {

    private final ProductoRepository repository;
    private final HistorialService historialService;

    public ProductoService(ProductoRepository repository, HistorialService historialService) {
        this.repository = repository;
        this.historialService = historialService;
    }

    public List<ProductoResponse> listar() {
        return repository.findAllByActivoTrueOrderByNombreAsc().stream().map(this::mapear).toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = new Producto();
        producto.setCodigo("TMP-" + UUID.randomUUID().toString().substring(0, 8));
        copiarDatos(producto, request);
        repository.save(producto);
        producto.setCodigo("PER-%04d".formatted(producto.getId()));
        repository.save(producto);
        historialService.apilar("PRODUCTO", "Se agregó al catálogo el perfume " + producto.getNombre() + ".");
        return mapear(producto);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = obtenerEntidad(id);
        copiarDatos(producto, request);
        repository.save(producto);
        historialService.apilar("PRODUCTO", "Se actualizó el perfume " + producto.getCodigo() + ".");
        return mapear(producto);
    }

    @Transactional
    public void desactivar(Long id) {
        Producto producto = obtenerEntidad(id);
        producto.setActivo(false);
        repository.save(producto);
        historialService.apilar("PRODUCTO", "Se retiró del catálogo el perfume " + producto.getCodigo() + ".");
    }

    public Producto obtenerEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No se encontró el perfume seleccionado."));
    }

    private void copiarDatos(Producto producto, ProductoRequest request) {
        producto.setNombre(NombreUtils.limpiar(request.nombre()));
        producto.setMarca(NombreUtils.limpiar(request.marca()));
        producto.setFamiliaOlfativa(NombreUtils.limpiar(request.familiaOlfativa()));
        producto.setPresentacionMl(request.presentacionMl());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setActivo(true);
    }

    private ProductoResponse mapear(Producto producto) {
        return new ProductoResponse(
                producto.getId(), producto.getCodigo(), producto.getNombre(), producto.getMarca(),
                producto.getFamiliaOlfativa(), producto.getPresentacionMl(), producto.getPrecio(),
                producto.getStock(), producto.isActivo()
        );
    }
}
