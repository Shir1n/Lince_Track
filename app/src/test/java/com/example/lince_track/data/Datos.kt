package com.example.lince_track.data

import com.example.lince_track.data.remote.dto.AsignacionDto
import com.example.lince_track.data.remote.dto.PersonaDto
import com.example.lince_track.data.remote.dto.ReservaDto
import com.example.lince_track.data.remote.dto.ServicioDto
import com.example.lince_track.data.servicios.ServicioMapper
import java.time.LocalDate

/** Datos de prueba con la misma forma que trae la API de Southbound. */
object Datos {

    fun dto(
        id: Long,
        fecha: LocalDate = LocalDate.now(),
        tipo: String = "EXCURSION",
        transportistaId: Long? = 37,
        guiaId: Long? = null,
        statusPL: String? = "RQ",
        estadoServicio: String = "CF",
        referencia: String = "REF$id"
    ) = ServicioDto(
        id = id,
        fecha = fecha.toString(),
        horaPickup = "08:30",
        tipo = tipo,
        descripcion = "Servicio $id",
        estadoServicio = estadoServicio,
        reserva = ReservaDto(referencia = referencia, nombre = "Pasajero $id", estado = "CF"),
        pickup = "Hotel Costanera Puerto Varas",
        dropoff = "Hotel Costanera Puerto Varas",
        guia = guiaId?.let { PersonaDto(it, nombre = "Guía $it") },
        transportista = transportistaId?.let { PersonaDto(it, nombre = "Conductor $it") },
        asignacion = AsignacionDto(statusPL = statusPL)
    )

    fun servicio(id: Long, statusPL: String? = "RQ", estadoServicio: String = "CF"): Servicio =
        ServicioMapper.aDominio(dto(id, statusPL = statusPL, estadoServicio = estadoServicio))!!
}
