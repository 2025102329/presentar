package com.delacruz.fragancias.config;

import com.delacruz.fragancias.entity.ConexionRuta;
import com.delacruz.fragancias.entity.Producto;
import com.delacruz.fragancias.repository.ConexionRutaRepository;
import com.delacruz.fragancias.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carga un catálogo y un grafo inicial únicamente la primera vez.
 * Los clientes y pedidos se dejan vacíos para que el usuario registre los suyos.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner cargarDatosBase(
            ProductoRepository productoRepository,
            ConexionRutaRepository conexionRepository
    ) {
        return args -> {
            if (productoRepository.count() == 0) {
                productoRepository.saveAll(List.of(
                        perfume("PER-0001", "Lumière Rose", "Maison Élégance", "Floral", 100, "189.90", 18),
                        perfume("PER-0002", "Noir Intense", "Atelier Nuit", "Amaderada", 100, "219.90", 12),
                        perfume("PER-0003", "Citrus Bloom", "Aura", "Cítrica", 75, "149.90", 24),
                        perfume("PER-0004", "Velvet Oud", "Imperial Scents", "Oriental", 100, "259.90", 9),
                        perfume("PER-0005", "Ocean Muse", "Blue House", "Acuática", 80, "169.90", 15)
                ));
            }

            if (conexionRepository.count() == 0) {
                conexionRepository.saveAll(List.of(
                        conexion("Tienda", "Centro de Lima", 5.5, 18, "MEDIO"),
                        conexion("Tienda", "San Miguel", 8.0, 24, "MEDIO"),
                        conexion("Tienda", "Ate", 10.0, 32, "ALTO"),
                        conexion("Centro de Lima", "Pueblo Libre", 5.0, 17, "MEDIO"),
                        conexion("Centro de Lima", "San Isidro", 7.0, 22, "ALTO"),
                        conexion("Pueblo Libre", "San Miguel", 4.0, 13, "BAJO"),
                        conexion("Pueblo Libre", "Miraflores", 8.0, 25, "MEDIO"),
                        conexion("San Miguel", "Miraflores", 9.0, 27, "MEDIO"),
                        conexion("San Isidro", "Miraflores", 4.0, 14, "ALTO"),
                        conexion("San Isidro", "Surco", 7.0, 21, "MEDIO"),
                        conexion("Miraflores", "Surco", 6.0, 18, "MEDIO"),
                        conexion("Surco", "Ate", 9.0, 26, "MEDIO")
                ));
            }
        };
    }

    private Producto perfume(
            String codigo,
            String nombre,
            String marca,
            String familia,
            int ml,
            String precio,
            int stock
    ) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setMarca(marca);
        p.setFamiliaOlfativa(familia);
        p.setPresentacionMl(ml);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        p.setActivo(true);
        return p;
    }

    private ConexionRuta conexion(
            String origen,
            String destino,
            double km,
            int minutos,
            String trafico
    ) {
        ConexionRuta c = new ConexionRuta();
        c.setOrigen(origen);
        c.setDestino(destino);
        c.setDistanciaKm(km);
        c.setTiempoMinutos(minutos);
        c.setTrafico(trafico);
        return c;
    }
}
