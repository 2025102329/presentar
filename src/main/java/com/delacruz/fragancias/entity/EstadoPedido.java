package com.delacruz.fragancias.entity;

/** Estados sencillos para seguir el ciclo de vida de un pedido. */
public enum EstadoPedido {
    PENDIENTE,
    LISTO_PARA_ENVIO,
    EN_RUTA,
    ENTREGADO,
    CANCELADO
}
