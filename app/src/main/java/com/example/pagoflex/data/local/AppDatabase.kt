package com.example.pagoflex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pagoflex.data.local.dao.CompromisoDao
import com.example.pagoflex.data.local.dao.ComprobanteDao
import com.example.pagoflex.data.local.dao.ConversacionDao
import com.example.pagoflex.data.local.dao.MensajeDao
import com.example.pagoflex.data.local.dao.EmpresaDao
import com.example.pagoflex.data.local.dao.PagoDao
import com.example.pagoflex.data.local.dao.ReporteDao
import com.example.pagoflex.data.local.dao.UserDao
import com.example.pagoflex.data.local.entity.CompromisoEntity
import com.example.pagoflex.data.local.entity.ComprobanteEntity
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import com.example.pagoflex.data.local.entity.EmpresaEntity
import com.example.pagoflex.data.local.entity.PagoEntity
import com.example.pagoflex.data.local.entity.ReporteEntity
import com.example.pagoflex.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.firstOrNull

@Database(
    entities = [
        UserEntity::class,
        EmpresaEntity::class,
        CompromisoEntity::class,
        PagoEntity::class,
        ComprobanteEntity::class,
        ReporteEntity::class,
        ConversacionEntity::class,
        MensajeEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun empresaDao(): EmpresaDao
    abstract fun compromisoDao(): CompromisoDao
    abstract fun pagoDao(): PagoDao
    abstract fun comprobanteDao(): ComprobanteDao
    abstract fun reporteDao(): ReporteDao
    abstract fun conversacionDao(): ConversacionDao
    abstract fun mensajeDao(): MensajeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN fotoUri TEXT")
            }
        }

        private val seedLock = Mutex()
        @Volatile private var seeded = false
        @Volatile private var demoSeeded = false

        fun getDatabase(context: Context): AppDatabase {
            val instance = INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pagoflex.db"
                )                    .addMigrations(MIGRATION_14_15).addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                seed(database)
                            }
                        }
                    }).build()
                INSTANCE = newInstance
                newInstance
            }
            CoroutineScope(Dispatchers.IO).launch { seed(instance); seedDemo(instance); cleanupDemoConversations(instance); seedAgentDemo(instance) }
            return instance
        }

        private suspend fun seed(db: AppDatabase) {
            try {
                android.util.Log.d("PagoFlexSeed", "seed start count=${db.userDao().count()}")
                seedLock.withLock {
                    if (seeded) return
                    if (db.userDao().count() > 0) { seeded = true; return }
                    android.util.Log.d("PagoFlexSeed", "seeding...")
                    val u = db.userDao()
                    val e = db.empresaDao()
                    val c = db.compromisoDao()
                    val p = db.pagoDao()
                    val r = db.reporteDao()

                    val hasher = com.example.pagoflex.security.PasswordHasher
                    val defaultPassword = "PagoFlex123!"
                    val salt = hasher.generateSalt()
                    val hash = hasher.hash(defaultPassword, salt)

                    val empresas = listOf(
                        EmpresaEntity(1001, "EC-01", "Inmobiliaria Altos del Maipo SpA", "Altos del Maipo", "76.845.213-K", "Inmobiliaria", "atencion@altosmaipo.ejemplo", "+56 2 0000 0201", 8500),
                        EmpresaEntity(1002, "EC-02", "Corporación Educacional Bosque Nativo", "Colegio Bosque Nativo", "65.132.874-8", "Educación escolar", "pagos@bosquenativo.ejemplo", "+56 2 0000 0202", 5000),
                        EmpresaEntity(1003, "EC-03", "Crédito Andino S.A.", "Crédito Andino", "96.571.438-3", "Crédito de consumo", "clientes@creditoandino.ejemplo", "+56 2 0000 0203", 12000),
                        EmpresaEntity(1004, "EC-04", "Seguros Horizonte Sur S.A.", "Horizonte Sur", "96.814.205-4", "Seguros", "contacto@horizontesur.ejemplo", "+56 2 0000 0204", 3000),
                        EmpresaEntity(1005, "EC-05", "Fundación Manos Abiertas", "Manos Abiertas", "65.247.190-0", "ONG", "socios@manosabiertas.ejemplo", "+56 2 0000 0205", 0),
                        EmpresaEntity(1006, "EC-06", "Club Deportivo Los Halcones", "Club Los Halcones", "65.398.412-K", "Club deportivo", "tesoreria@loshalcones.ejemplo", "+56 2 0000 0206", 2000),
                        EmpresaEntity(1007, "EC-07", "Instituto Técnico Cordillera SpA", "Instituto Cordillera", "77.214.583-7", "Educación superior", "finanzas@itcordillera.ejemplo", "+56 2 0000 0207", 6000),
                        EmpresaEntity(1008, "EC-08", "Arriendos Plaza Oriente Ltda.", "Plaza Oriente", "76.432.871-K", "Administración de arriendos", "cobranza@plazaoriente.ejemplo", "+56 2 0000 0208", 7500)
                    )
                    empresas.forEach { e.insert(it) }

                    val users = listOf(
                        UserEntity(1, "UF-01", "Camila Andrea Rojas Peña", "15.834.207-3", "crojas@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0101", "Maipú", null, null, null, true, "14-03-2023", 3, true),
                        UserEntity(2, "UF-02", "Luis Alberto Muñoz Tapia", "12.476.318-5", "lmunoz@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0102", "Puente Alto", null, null, null, true, "02-11-2023", 1, true),
                        UserEntity(3, "UF-03", "Rosa Elena Contreras Díaz", "6.893.541-5", "rcontreras@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0103", "San Miguel", null, null, null, true, "20-06-2024", 5, true),
                        UserEntity(4, "UF-04", "Matías Ignacio Soto Vergara", "21.045.873-5", "msoto@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0104", "La Florida", null, null, null, true, "07-03-2025", null, false),
                        UserEntity(5, "UF-05", "Fernanda Paz Godoy Lagos", "17.562.904-1", "fgodoy@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0105", "Ñuñoa", null, null, null, true, "15-01-2024", 3, true),
                        UserEntity(6, "UF-06", "Héctor Manuel Pizarro Campos", "10.318.265-4", "hpizarro@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0106", "Quilicura", null, null, null, true, "28-08-2023", 3, true),
                        UserEntity(7, "UF-07", "Javiera Constanza Reyes Molina", "19.287.430-0", "jreyes@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0107", "San Bernardo", null, null, null, true, "03-05-2025", 1, true),
                        UserEntity(8, "UF-08", "Benjamín Esteban Araya Fuentes", "18.704.396-4", "baraya@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0108", "Estación Central", null, null, null, true, "11-09-2026", 3, true),
                        UserEntity(900, "TEST-USUARIO", "Usuario Prueba", null, "usuario.prueba@pagoflex.cl", hash, salt, "USUARIO", null, null, null, null, null, true, null, 3, true),
                        UserEntity(901, "TEST-EJECUTIVO", "Ejecutivo Prueba", null, "ejecutivo.prueba@pagoflex.cl", hash, salt, "EJECUTIVO", null, null, 1001, null, "Ejecutivo de cobranza", true, null, null, true),
                        UserEntity(902, "TEST-AGENTE", "Agente Prueba", null, "agente.prueba@pagoflex.cl", hash, salt, "AGENTE", null, null, null, "Soporte", null, true, null, null, true)
                    )
                    users.forEach { u.insert(it) }

                    val agentes = listOf(
                        UserEntity(101, "AG-01", "Andrés Felipe Lagos Meza", "16.248.317-K", "alagos@pagoflex.ejemplo", hash, salt, "AGENTE", null, null, null, "Soporte", null, true, null, null, true),
                        UserEntity(102, "AG-02", "Daniela Sofía Ortiz Bravo", "18.352.091-1", "dortiz@pagoflex.ejemplo", hash, salt, "AGENTE", null, null, null, "Soporte", null, true, null, null, true),
                        UserEntity(103, "AG-03", "Ricardo José Salinas Cáceres", "13.907.264-2", "rsalinas@pagoflex.ejemplo", hash, salt, "AGENTE", null, null, null, "Soporte", null, false, null, null, true),
                        UserEntity(104, "AG-04", "Valentina Ignacia Paredes Silva", "17.781.435-0", "vparedes@pagoflex.ejemplo", hash, salt, "AGENTE", null, null, null, "Comercial", null, true, null, null, true),
                        UserEntity(105, "AG-05", "Tomás Alejandro Vidal Rojas", "19.564.028-9", "tvidal@pagoflex.ejemplo", hash, salt, "AGENTE", null, null, null, "Comercial", null, true, null, null, true)
                    )
                    agentes.forEach { u.insert(it) }

                    val ejecutivos = listOf(
                        UserEntity(201, "EJ-01", "Paula Andrea Saavedra Núñez", "14.629.853-2", "psaavedra@altosmaipo.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0401", null, 1001, null, "Ejecutiva de cobranza", true, null, null, true),
                        UserEntity(202, "EJ-02", "Marco Antonio Fuentes Riquelme", "11.853.720-3", "mfuentes@bosquenativo.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0402", null, 1002, null, "Encargado de finanzas", true, null, null, true),
                        UserEntity(203, "EJ-03", "Claudia Beatriz Henríquez Soto", "15.370.946-7", "chenriquez@creditoandino.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0403", null, 1003, null, "Jefa de cobranza", true, null, null, true),
                        UserEntity(204, "EJ-04", "Felipe Ignacio Morales Tapia", "17.095.482-3", "fmorales@creditoandino.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0404", null, 1003, null, "Ejecutivo de cobranza", true, null, null, true),
                        UserEntity(205, "EJ-05", "Gloria Inés Carrasco Vega", "9.846.217-1", "gcarrasco@loshalcones.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0405", null, 1006, null, "Tesorera", true, null, null, true),
                        UserEntity(206, "EJ-06", "Sebastián Andrés Núñez Olivares", "18.930.571-0", "snunez@itcordillera.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0406", null, 1007, null, "Analista de finanzas", true, null, null, true),
                        UserEntity(207, "EJ-07", "Mónica Patricia Leiva Castro", "12.584.396-4", "mleiva@plazaoriente.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0407", null, 1008, null, "Administradora de cobranza", true, null, null, true)
                    )
                    ejecutivos.forEach { u.insert(it) }

                    suspend fun insComp(id: Long, folio: String, userId: Long, empId: Long, empresa: String, concepto: String, cuota: Int?, total: Int?, monto: Int, fecha: String, estado: String, recargo: Int) {
                        c.insert(CompromisoEntity(id, folio, userId, empId, empresa, concepto, cuota, total, monto, fecha, estado, recargo))
                    }
                    insComp(101, "CP-2026-0101", 1, 1008, "Plaza Oriente", "Arriendo departamento octubre", 10, 12, 520000, "10-10-2026", "PENDIENTE", 0)
                    insComp(102, "CP-2026-0102", 1, 1006, "Club Los Halcones", "Cuota social octubre", 10, 12, 15000, "05-10-2026", "VENCIDO", 2000)
                    insComp(103, "CP-2026-0103", 1, 1005, "Manos Abiertas", "Aporte mensual octubre", null, null, 10000, "15-10-2026", "PENDIENTE", 0)
                    insComp(104, "CP-2026-0104", 1, 1003, "Crédito Andino", "Crédito de consumo", 7, 24, 86400, "25-09-2026", "PAGADO", 0)
                    insComp(105, "CP-2026-0105", 1, 1003, "Crédito Andino", "Crédito de consumo", 8, 24, 86400, "25-10-2026", "PENDIENTE", 0)
                    insComp(106, "CP-2026-0106", 2, 1001, "Altos del Maipo", "Cuota pie departamento", 14, 36, 310000, "30-09-2026", "VENCIDO", 8500)
                    insComp(107, "CP-2026-0107", 2, 1001, "Altos del Maipo", "Cuota pie departamento", 15, 36, 310000, "30-10-2026", "PENDIENTE", 0)
                    insComp(108, "CP-2026-0108", 3, 1004, "Horizonte Sur", "Seguro de hogar octubre", 10, 12, 18900, "01-10-2026", "PAGADO", 0)
                    insComp(109, "CP-2026-0109", 4, 1007, "Instituto Cordillera", "Arancel octubre", 8, 10, 142000, "05-10-2026", "EN_REVISION", 0)
                    insComp(110, "CP-2026-0110", 5, 1002, "Colegio Bosque Nativo", "Mensualidad octubre", 8, 10, 185000, "10-10-2026", "PENDIENTE", 0)
                    insComp(111, "CP-2026-0111", 6, 1003, "Crédito Andino", "Crédito de consumo", 19, 36, 54200, "20-09-2026", "ANULADO", 0)
                    insComp(112, "CP-2026-0112", 7, 1006, "Club Los Halcones", "Cuota social octubre", 10, 12, 15000, "05-10-2026", "PAGADO", 0)
                    insComp(89, "CP-2026-0089", 1, 1008, "Plaza Oriente", "Arriendo departamento septiembre", 9, 12, 520000, "10-09-2026", "PAGADO", 0)
                    insComp(90, "CP-2026-0090", 1, 1006, "Club Los Halcones", "Cuota social septiembre", 9, 12, 15000, "05-09-2026", "PAGADO", 2000)
                    insComp(91, "CP-2026-0091", 1, 1005, "Manos Abiertas", "Aporte mensual septiembre", null, null, 10000, "15-09-2026", "PAGADO", 0)
                    insComp(98, "CP-2026-0098", 5, 1002, "Colegio Bosque Nativo", "Mensualidad septiembre", null, null, 185000, "10-09-2026", "PAGADO", 0)
                    insComp(72, "CP-2026-0072", 2, 1001, "Altos del Maipo", "Cuota pie departamento", 13, 36, 310000, "02-09-2026", "PAGADO", 8500)

                    p.insert(PagoEntity(398, "CPR-000398", 89, "09-09-2026 13:05", 520000, "Portal de pagos", "Pago simulado"))
                    p.insert(PagoEntity(402, "CPR-000402", 90, "07-09-2026 18:40", 17000, "Portal de pagos", "Pago simulado"))
                    p.insert(PagoEntity(405, "CPR-000405", 91, "15-09-2026 08:22", 10000, "Agente conversacional", "Pago simulado"))
                    p.insert(PagoEntity(451, "CPR-000451", 104, "24-09-2026 10:42", 86400, "Portal de pagos", "Pago simulado"))
                    p.insert(PagoEntity(452, "CPR-000452", 108, "30-09-2026 19:05", 18900, "Agente conversacional", "Pago simulado"))
                    p.insert(PagoEntity(453, "CPR-000453", 112, "04-10-2026 08:15", 15000, "App", "Pago simulado"))
                    p.insert(PagoEntity(447, "CPR-000447", 98, "10-09-2026 22:31", 185000, "Portal de pagos", "Pago simulado"))
                    p.insert(PagoEntity(377, "CPR-000377", 72, "02-09-2026 21:14", 318500, "Agente conversacional", "Pago simulado"))

                    r.insert(ReporteEntity(id = 11, tipo = "Ya pagué por otro medio", compromiso = "CP-2026-0109", descripcion = "Pagué por transferencia directa al instituto el 03-10.", fecha = "03-10-2026", idUsuario = 4))
                    r.insert(ReporteEntity(id = 9, tipo = "El monto no corresponde", compromiso = "CP-2026-0111", descripcion = "Repacté el crédito y me siguen cobrando la cuota antigua.", fecha = "18-09-2026", idUsuario = 6))
                    r.insert(ReporteEntity(id = 7, tipo = "No reconozco este cobro", compromiso = "CP-2026-0081", descripcion = "Me aparece una matrícula que ya pagué en marzo.", fecha = "02-09-2026", idUsuario = 5))
                    r.insert(ReporteEntity(id = 5, tipo = "Otro", compromiso = "CP-2026-0064", descripcion = "El concepto dice \"agosto\" y es la cuota de julio.", fecha = "28-07-2026", idUsuario = 1))
                    r.insert(ReporteEntity(id = 12, tipo = "El monto no corresponde", compromiso = "CP-2026-0106", descripcion = "No sé por qué me cobran $318.500.", fecha = "05-10-2026", idUsuario = 2))
                    r.insert(ReporteEntity(id = 10, tipo = "No reconozco este cobro", compromiso = "CP-2026-0095", descripcion = "Me llegó un cobro de un club al que no pertenezco.", fecha = "30-09-2026", idUsuario = 8))

                    seeded = true
                    android.util.Log.d("PagoFlexSeed", "seed done count=${db.userDao().count()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PagoFlexSeed", "seed failed", e)
            }
        }

        private suspend fun seedDemo(db: AppDatabase) {
            val userId = 900L
            val c = db.compromisoDao()
            val p = db.pagoDao()
            val convDao = db.conversacionDao()
            val msgDao = db.mensajeDao()
            try {
                if (demoSeeded) return
                val existing = c.getByUsuario(userId).firstOrNull()
                android.util.Log.d("PagoFlexSeed", "demo existing=$existing")
                if (existing != null && existing.isNotEmpty()) {
                    demoSeeded = true
                    return
                } else {
                    val fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu")
                    val now = java.time.LocalDate.now()
                    val empresas = listOf(1003L, 1003L, 1008L, 1006L, 1003L)
                    android.util.Log.d("PagoFlexSeed", "inserting compromisos")
                    val compromisos = listOf(
                        Triple("CP-DEMO-001", "Compromiso pr?ximo a vencer 3 d?as", 3),
                        Triple("CP-DEMO-002", "Compromiso pr?ximo a vencer 7 d?as", 7),
                        Triple("CP-DEMO-003", "Compromiso pendiente 15 d?as", 15),
                        Triple("CP-DEMO-004", "Compromiso vencido hace 2 d?as", -2)
                    )
                    compromisos.forEachIndexed { idx, (folio, concepto, dias) ->
                        val fecha = now.plusDays(dias.toLong()).format(fmt)
                        val estado = if (dias < 0) "VENCIDO" else "PENDIENTE"
                        val recargo = if (dias < 0) 12000 else 0
                        c.insert(CompromisoEntity(9000L + idx, folio, userId, 1003L, "Cr?dito Andino", concepto, 1, 12, 50000 + idx * 10000, fecha, estado, recargo))
                    }
                    c.insert(CompromisoEntity(9004L, "CP-DEMO-005", userId, 1003L, "Cr?dito Andino", "Compromiso pagado", 1, 12, 30000, now.minusDays(10).format(fmt), "PAGADO", 0))
                    p.insert(PagoEntity(9001L, "CPR-DEMO-001", 9004L, now.minusDays(10).format(fmt) + " 10:00", 30000, "App", "Pago simulado"))
                }
                android.util.Log.d("PagoFlexSeed", "checking conv")
                // No se crean mensajes automaticos. La conversacion se crea cuando el usuario envia su primer mensaje.
                demoSeeded = true
                android.util.Log.d("PagoFlexSeed", "demo seed done")
            } catch (e: Exception) {
                android.util.Log.e("PagoFlexSeed", "demo seed failed", e)
            }
        }

        private suspend fun seedAgentDemo(db: AppDatabase) {
            val u = db.userDao()
            val c = db.conversacionDao()
            val m = db.mensajeDao()
            try {
                val hasher = com.example.pagoflex.security.PasswordHasher
                val salt = hasher.generateSalt()
                val hash = hasher.hash("PagoFlex123!", salt)
                if (u.getById(950L) == null) {
                    u.insert(UserEntity(950, "DEMO-CAMILA", "Camila Torres", null, "camila.torres@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0201", "Santiago", null, null, null, true, null, 3, true))
                    u.insert(UserEntity(951, "DEMO-MATIAS", "Matias Rojas", null, "matias.rojas@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0202", "Santiago", null, null, null, true, null, 3, true))
                    u.insert(UserEntity(952, "DEMO-VALENTINA", "Valentina Soto", null, "valentina.soto@correo.ejemplo", hash, salt, "USUARIO", "+56 9 0000 0203", "Santiago", null, null, null, true, null, 3, true))
                }
                val fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")
                val now = java.time.LocalDateTime.now()
                val demos = listOf(
                    Triple(950L, "Camila Torres", "Realice un pago ayer y todavia no aparece reflejado en mis compromisos."),
                    Triple(951L, "Matias Rojas", "No puedo visualizar mis compromisos de pago en la aplicacion."),
                    Triple(952L, "Valentina Soto", "Como puedo descargar el comprobante de uno de mis pagos?")
                )
                demos.forEach { (uid, nombre, msg) ->
                    if (c.getByUsuarioOne(uid) == null) {
                        val convId = c.insert(ConversacionEntity(idUsuario = uid, fechaCreacion = now.format(fmt), estado = "PENDIENTE"))
                        m.insert(MensajeEntity(idConversacion = convId, idRemitente = uid, rolRemitente = "USUARIO", contenido = msg, fechaHora = now.format(fmt)))
                    }
                }
                android.util.Log.d("PagoFlexSeed", "agent demo done")
            } catch (e: Exception) {
                android.util.Log.e("PagoFlexSeed", "agent demo failed", e)
            }
        }

        private suspend fun cleanupDemoConversations(db: AppDatabase) {
            try {
                db.mensajeDao().deleteByUsuario(900L)
                db.conversacionDao().deleteByUsuario(900L)
                android.util.Log.d("PagoFlexSeed", "demo conversations cleaned")
            } catch (e: Exception) {
                android.util.Log.e("PagoFlexSeed", "cleanup failed", e)
            }
        }
    }
}
