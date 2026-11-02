package com.example.lince_track.data

/**
 * Reemplazo elegido por el administrador, guardado en `asignaciones/{servicioId}`. La API es de
 * solo lectura, así que manda sobre la persona que trae el servicio.
 */
data class AsignacionServicio(
    val servicioId: String,
    val uid: String,
    val nombre: String
)

/** Cuenta de `usuarios` que puede recibir un servicio. [idApi] es su guiaId o transportistaId. */
data class PersonaDisponible(
    val uid: String,
    val nombre: String,
    val rol: Rol,
    val idApi: Long?
)

/** Qué servicios ve cada persona y a quién se le puede reasignar. Sin Android ni Firebase. */
object ReglasAsignacion {

    /** Pone al reemplazo en el servicio, que vuelve a quedar por responder. Lo cancelado no cambia. */
    fun aplicar(servicio: Servicio, asignacion: AsignacionServicio?): Servicio {
        if (asignacion == null || servicio.cancelado) return servicio
        val conReemplazo = servicio.copy(estado = EstadoServicio.PENDIENTE, reasignado = true)
        return if (servicio.esGuiado) {
            conReemplazo.copy(guia = asignacion.nombre)
        } else {
            conReemplazo.copy(conductor = asignacion.nombre)
        }
    }

    /** Solo cuenta la respuesta del reemplazo: la de quien tenía antes el servicio ya no vale. */
    private fun respuestaVigente(respuesta: RespuestaServicio?, asignacion: AsignacionServicio?): RespuestaServicio? =
        if (asignacion == null || respuesta?.uid == asignacion.uid) respuesta else null

    /**
     * Lista final de servicios para las pantallas.
     *
     * @param propios lo que trae la fuente para el usuario (para el administrador, toda la oficina).
     * @param oficina toda la oficina; vacía para el administrador, que ya la tiene en [propios].
     */
    fun armar(
        propios: List<Servicio>,
        oficina: List<Servicio>,
        asignaciones: Map<String, AsignacionServicio>,
        respuestas: Map<String, RespuestaServicio>,
        estados: Map<String, RespuestaServicio>,
        perfil: PerfilUsuario?,
        uid: String?
    ): List<Servicio> {
        val lista = if (perfil?.rol == Rol.ADMINISTRADOR) {
            propios.map { servicio ->
                val asignacion = asignaciones[servicio.id]
                val conRespuesta = ReglasRespuesta.aplicar(
                    aplicar(servicio, asignacion),
                    respuestaVigente(respuestas[servicio.id], asignacion)
                )
                ReglasCompanero.aplicarPublicado(conRespuesta, estados[servicio.id])
            }
        } else {
            visiblesPara(propios, oficina, asignaciones, respuestas, perfil, uid)
        }
        val otros = if (perfil?.rol == Rol.ADMINISTRADOR) {
            lista
        } else {
            oficina.map { ReglasCompanero.aplicarPublicado(aplicar(it, asignaciones[it.id]), estados[it.id]) }
        }
        return lista.map { ReglasCompanero.completar(it, otros) }
    }

    /**
     * Lo propio más lo reasignado a esta persona. Lo que se le reasignó a otra deja de verse,
     * salvo que lo haya rechazado: ese rechazo sigue siendo parte de su historial.
     */
    private fun visiblesPara(
        propios: List<Servicio>,
        oficina: List<Servicio>,
        asignaciones: Map<String, AsignacionServicio>,
        respuestas: Map<String, RespuestaServicio>,
        perfil: PerfilUsuario?,
        uid: String?
    ): List<Servicio> {
        val idsPropios = propios.map { it.id }.toSet()
        val recibidos = oficina.filter { it.id !in idsPropios && asignaciones[it.id]?.uid == uid }
        return (propios + recibidos)
            .filter { servicio ->
                val asignacion = asignaciones[servicio.id]
                asignacion == null || asignacion.uid == uid ||
                    respuestas[servicio.id]?.estado == EstadoServicio.RECHAZADO
            }
            .map { servicio ->
                val asignacion = asignaciones[servicio.id]?.takeIf { it.uid == uid }
                ReglasCompanero.conNombrePropio(
                    ReglasRespuesta.aplicar(aplicar(servicio, asignacion), respuestas[servicio.id]),
                    perfil
                )
            }
            .sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
    }

    /** Solo lo que nadie ha tomado: una vez hecho el check-in, el servicio ya está en curso. */
    fun puedeReasignar(servicio: Servicio, checkin: CheckinServicio?): Boolean =
        !servicio.cancelado && checkin == null && servicio.estado != EstadoServicio.ACEPTADO

    /**
     * Cuentas que pueden recibir el servicio: las del rol que corresponde, vinculadas y con nombre.
     * Se quita a quien lo tiene y a quien ya lo rechazó, porque un rechazo es definitivo.
     */
    fun candidatos(
        servicio: Servicio,
        personal: List<PersonaDisponible>,
        asignacion: AsignacionServicio?,
        rechazaron: Set<String>
    ): List<PersonaDisponible> {
        val rol = if (servicio.esGuiado) Rol.GUIA else Rol.CONDUCTOR
        return personal
            .filter { it.rol == rol && it.idApi != null && it.nombre.isNotBlank() }
            .filter { it.uid !in rechazaron }
            .filter { if (asignacion == null) it.idApi != servicio.responsableId else it.uid != asignacion.uid }
            .sortedBy { it.nombre }
    }

    /** Quiénes rechazaron cada servicio: id del servicio → uids. */
    fun rechazos(respuestas: List<RespuestaServicio>): Map<String, Set<String>> =
        respuestas
            .filter { it.estado == EstadoServicio.RECHAZADO && it.uid != null }
            .groupBy({ it.servicioId }, { it.uid!! })
            .mapValues { it.value.toSet() }
}
