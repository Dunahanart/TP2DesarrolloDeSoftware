// Qué acontecimiento representa un Movimiento dentro del recorrido de un envío.
// Usamos un enum (y no texto libre) porque los tipos de acontecimiento son
// fijos por el negocio y así no se pueden registrar movimientos incoherentes.
public enum TipoMovimiento {
    RECIBIDO,
    DESPACHADO,
    ENTREGADO
}
