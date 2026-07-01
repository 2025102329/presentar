package com.delacruz.fragancias.service;

import com.delacruz.fragancias.entity.Historial;
import com.delacruz.fragancias.repository.HistorialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Mantiene el historial con comportamiento LIFO: el último registro aparece primero. */
@Service
public class HistorialService {

    private final HistorialRepository repository;

    public HistorialService(HistorialRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void apilar(String tipo, String descripcion) {
        Historial item = new Historial();
        item.setTipo(tipo);
        item.setDescripcion(descripcion);
        repository.save(item);
    }

    public List<Historial> listarPila() {
        return repository.findAllByOrderByFechaDesc();
    }

    /** Retira solamente la cima de la pila; no borra clientes ni pedidos asociados. */
    @Transactional
    public Optional<Historial> desapilar() {
        Optional<Historial> ultimo = repository.findFirstByOrderByFechaDesc();
        ultimo.ifPresent(repository::delete);
        return ultimo;
    }
}
