// Etapas del envío, con los mismos criterios que EstadoPaquete: máquina de
// estados que avanza de a un paso y protege sus transiciones.
//
//   REGISTRADO       -> dado de alta en el sistema, aún no lo recibió ninguna sucursal
//   EN_PREPARACION   -> una sucursal lo recibió y lo está preparando
//   EN_VIAJE         -> fue despachado, está rodando (puede pasar por sucursales intermedias)
//   ENTREGADO        -> llegó al destino final
public enum EstadoEnvio {
    REGISTRADO,
    EN_PREPARACION,
    EN_VIAJE,
    ENTREGADO
}
