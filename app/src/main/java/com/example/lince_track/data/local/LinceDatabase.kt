package com.example.lince_track.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Check-in o check-out que no alcanzó a llegar al backend (sin señal o servidor caído). WorkManager lo envía después
 * con la misma [clave]: si el backend ya lo tenía, responde con el mismo hito en vez de duplicarlo.
 * Guarda también lo necesario para escribir en Firestore y crear la incidencia sin depender de la pantalla.
 */
@Entity(tableName = "hitos_pendientes")
data class HitoPendiente(
    @PrimaryKey val clave: String,
    val uid: String,
    /** Id de [com.example.lince_track.data.Rol]. */
    val rol: String,
    val nombre: String,
    val servicioId: String,
    val titulo: String,
    /** Fecha del servicio, AAAA-MM-DD. */
    val fecha: String,
    /** Pickup (check-in) o destino (check-out), para el detalle de la incidencia. */
    val lugar: String,
    val checkIn: Boolean,
    val lat: Double,
    val lon: Double,
    val precisionMetros: Float,
    /** Distancia a la geocerca que calculó la app (se guarda en Firestore como hoy). */
    val distanciaMetros: Int,
    val creadoEn: Long,
    val intentos: Int = 0,
    val ultimoError: String? = null
)

/** Copia local de las geocercas del backend, para validar el check-in sin conexión. */
@Entity(tableName = "geocercas")
data class GeocercaGuardada(
    @PrimaryKey val id: Long,
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val radioMetros: Int
)

/** Copia local de las reglas del check-in (`/api/parametros`); una sola fila. */
@Entity(tableName = "parametros")
data class ParametrosGuardados(
    @PrimaryKey val id: Int = 1,
    val anticipacionMinutos: Int,
    val precisionMaximaMetros: Int
)

@Dao
abstract class LinceDao {

    // ---------- Cola de hitos ----------

    /** Si ya existe la misma clave (doble toque), no se duplica. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun guardarHito(hito: HitoPendiente)

    @Query("SELECT * FROM hitos_pendientes ORDER BY creadoEn")
    abstract fun observarHitos(): Flow<List<HitoPendiente>>

    @Query("SELECT * FROM hitos_pendientes ORDER BY creadoEn")
    abstract suspend fun hitosPendientes(): List<HitoPendiente>

    @Query("DELETE FROM hitos_pendientes WHERE clave = :clave")
    abstract suspend fun borrarHito(clave: String)

    @Query("UPDATE hitos_pendientes SET intentos = intentos + 1, ultimoError = :error WHERE clave = :clave")
    abstract suspend fun marcarIntento(clave: String, error: String?)

    // ---------- Caché del backend ----------

    @Query("SELECT * FROM geocercas")
    abstract suspend fun geocercas(): List<GeocercaGuardada>

    @Query("DELETE FROM geocercas")
    protected abstract suspend fun borrarGeocercas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertarGeocercas(geocercas: List<GeocercaGuardada>)

    /** Reemplaza toda la caché: una geocerca borrada o desactivada en el backend también desaparece aquí. */
    @Transaction
    open suspend fun reemplazarGeocercas(geocercas: List<GeocercaGuardada>) {
        borrarGeocercas()
        insertarGeocercas(geocercas)
    }

    @Query("SELECT * FROM parametros WHERE id = 1")
    abstract suspend fun parametros(): ParametrosGuardados?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardarParametros(parametros: ParametrosGuardados)
}

@Database(
    entities = [HitoPendiente::class, GeocercaGuardada::class, ParametrosGuardados::class],
    version = 1,
    exportSchema = false
)
abstract class LinceDatabase : RoomDatabase() {
    abstract fun dao(): LinceDao
}

/** La base local de la app. Se inicia en MainActivity y en el Worker (que puede correr con la app cerrada). */
object BaseLocal {

    @Volatile
    private var base: LinceDatabase? = null

    fun iniciar(context: Context): LinceDao {
        val actual = base ?: synchronized(this) {
            base ?: Room.databaseBuilder(context.applicationContext, LinceDatabase::class.java, "lince.db")
                // Solo guarda una cola y una caché: si cambia la estructura, se puede partir de cero.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { base = it }
        }
        return actual.dao()
    }

    /** null si todavía no se inició (por ejemplo, en los tests de JVM). */
    val dao: LinceDao?
        get() = base?.dao()
}
