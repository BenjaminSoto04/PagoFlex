package com.example.pagoflex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pagoflex.data.local.dao.CompromisoDao
import com.example.pagoflex.data.local.dao.ComprobanteDao
import com.example.pagoflex.data.local.dao.ConversacionDao
import com.example.pagoflex.data.local.dao.EmpresaDao
import com.example.pagoflex.data.local.dao.MensajeDao
import com.example.pagoflex.data.local.dao.PagoDao
import com.example.pagoflex.data.local.dao.ReporteDao
import com.example.pagoflex.data.local.dao.UserDao
import com.example.pagoflex.data.local.entity.CompromisoEntity
import com.example.pagoflex.data.local.entity.ComprobanteEntity
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.EmpresaEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import com.example.pagoflex.data.local.entity.PagoEntity
import com.example.pagoflex.data.local.entity.PreguntasSeguridad
import com.example.pagoflex.data.local.entity.ReporteEntity
import com.example.pagoflex.data.local.entity.ReporteMensajeEntity
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.ui.styles.Validaciones
import com.example.pagoflex.util.FolioGenerator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        UserEntity::class,
        EmpresaEntity::class,
        CompromisoEntity::class,
        PagoEntity::class,
        ComprobanteEntity::class,
        ReporteEntity::class,
        ReporteMensajeEntity::class,
        ConversacionEntity::class,
        MensajeEntity::class
    ],
    version = 21,
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

        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE compromisos ADD COLUMN motivoAnulacion TEXT")
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE usuarios ADD COLUMN preguntaSeguridad TEXT NOT NULL DEFAULT '${PreguntasSeguridad.PREGUNTA_DEFAULT}'"
                )
                db.execSQL(
                    "ALTER TABLE usuarios ADD COLUMN respuestaSeguridad TEXT NOT NULL DEFAULT '${PreguntasSeguridad.RESPUESTA_DEFAULT}'"
                )
            }
        }

        val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN agenteAsignadoId INTEGER")
            }
        }

        val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE compromisos ADD COLUMN folioUnico TEXT")
                db.execSQL("ALTER TABLE pagos ADD COLUMN folioUnico TEXT")
                db.execSQL("ALTER TABLE pagos ADD COLUMN cuotaPagada INTEGER")
            }
        }

        val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pagos ADD COLUMN fechaVencimientoCuota TEXT")
            }
        }

        val MIGRATION_20_21 = object : androidx.room.migration.Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reportes ADD COLUMN estado TEXT NOT NULL DEFAULT 'PENDIENTE'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reporte_mensajes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `idReporte` INTEGER NOT NULL,
                        `idRemitente` INTEGER NOT NULL,
                        `rolRemitente` TEXT NOT NULL,
                        `contenido` TEXT NOT NULL,
                        `fechaHora` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                // La descripcion original de cada reporte pasa a ser el primer
                // mensaje del hilo para conservar el historial al migrar.
                db.execSQL(
                    """
                    INSERT INTO reporte_mensajes (idReporte, idRemitente, rolRemitente, contenido, fechaHora)
                    SELECT id, idUsuario, 'USUARIO', descripcion, fecha FROM reportes
                    """.trimIndent()
                )
            }
        }

        private val seedLock = Mutex()
        private val datosExtendidosLock = Mutex()
        @Volatile private var seeded = false
        @Volatile private var demoSeeded = false
        @Volatile private var datosExtendidosListos = false
        private val seedDeferred = CompletableDeferred<Unit>()

        suspend fun awaitSeed() {
            if (INSTANCE == null) return
            seedDeferred.await()
        }

        fun getDatabase(context: Context): AppDatabase {
            val instance = INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pagoflex.db"
                )
                    .addMigrations(MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21)
                    .addCallback(object : Callback() {
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
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    seed(instance)
                    limpiarUsuariosMock(instance)
                    seedDemo(instance)
                    seedConversacionesAtencion(instance)
                    seedMensajesAgentes(instance)
                    completarDatosUsuarios(instance)
                    seedDatosExtendidos(instance)
                } finally {
                    seedDeferred.complete(Unit)
                }
            }
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
                    val pregDefault = PreguntasSeguridad.PREGUNTA_DEFAULT
                    val respDefault = PreguntasSeguridad.RESPUESTA_DEFAULT

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
                        UserEntity(1, "UF-01", "Camila Andrea Rojas Peña", "15.834.207-3", "crojas@correo.ejemplo", hash, salt, "USUARIO", "+56 9 5555 0101", "Maipú", null, null, null, true, "14-03-2023", 3, true, null, 101, null, pregDefault, respDefault),
                        UserEntity(2, "UF-02", "Luis Alberto Muñoz Tapia", "12.476.318-5", "lmunoz@correo.ejemplo", hash, salt, "USUARIO", "+56 9 5555 0102", "Puente Alto", null, null, null, true, "02-11-2023", 1, true, null, 101, null, pregDefault, respDefault),
                        UserEntity(3, "UF-03", "Rosa Elena Contreras Díaz", "6.893.541-5", "rcontreras@correo.ejemplo", hash, salt, "USUARIO", "+56 9 5555 0103", "San Miguel", null, null, null, true, "20-06-2024", 5, true, null, 102, null, pregDefault, respDefault),
                        UserEntity(4, "UF-04", "Matías Ignacio Soto Vergara", "21.045.873-5", "msoto@correo.ejemplo", hash, salt, "USUARIO", "+56 9 5555 0104", "La Florida", null, null, null, true, "07-03-2025", 3, false, null, 102, null, pregDefault, respDefault),
                        UserEntity(5, "UF-05", "Fernanda Paz Godoy Lagos", "17.562.904-1", "fgodoy@correo.ejemplo", hash, salt, "USUARIO", "+56 9 5555 0105", "Ñuñoa", null, null, null, true, "15-01-2024", 3, true, null, 103, null, pregDefault, respDefault),
                        UserEntity(6, "UF-06", "Héctor Manuel Pizarro Campos", "10.318.265-4", "hpizarro@correo.ejemplo", hash, salt, "USUARIO", "+56 9 5555 0106", "Quilicura", null, null, null, true, "28-08-2023", 3, true, null, 103, null, pregDefault, respDefault),
                        UserEntity(7, "UF-07", "Javiera Constanza Reyes Molina", "19.287.430-0", "jreyes@correo.ejemplo", hash, salt, "USUARIO", "+56 9 5555 0107", "San Bernardo", null, null, null, true, "03-05-2025", 1, true, null, 104, null, pregDefault, respDefault),
                        UserEntity(8, "UF-08", "Benjamín Esteban Araya Fuentes", "18.704.396-4", "baraya@correo.ejemplo", hash, salt, "USUARIO", "+56 9 5555 0108", "Estación Central", null, null, null, true, "11-09-2026", 3, true, null, 104, null, pregDefault, respDefault),
                        UserEntity(9, "UF-09", "Valentina Isabel Herrera Soto", "20.123.456-7", "valentina.herrera@correo.ejemplo", hash, salt, "USUARIO", "+56 9 5555 0190", "Santiago", null, null, null, true, "10-01-2026", 3, true, null, 105, null, pregDefault, respDefault)
                    )

                    users.forEach { u.insert(it) }

                    val agentes = listOf(
                        UserEntity(101, "AG-01", "Andrés Felipe Lagos Meza", "16.248.317-K", "alagos@pagoflex.ejemplo", hash, salt, "AGENTE", "+56 9 5555 0201", "Santiago", null, "Soporte", "Agente de soporte", true, "12-01-2023", 3, true, null, null, null, pregDefault, respDefault),
                        UserEntity(102, "AG-02", "Daniela Sofía Ortiz Bravo", "18.352.091-1", "dortiz@pagoflex.ejemplo", hash, salt, "AGENTE", "+56 9 5555 0202", "Maipú", null, "Soporte", "Agente de soporte", true, "05-04-2023", 3, true, null, null, null, pregDefault, respDefault),
                        UserEntity(103, "AG-03", "Ricardo José Salinas Cáceres", "13.907.264-2", "rsalinas@pagoflex.ejemplo", hash, salt, "AGENTE", "+56 9 5555 0203", "Puente Alto", null, "Soporte", "Agente de soporte", false, "18-08-2023", 3, true, null, null, null, pregDefault, respDefault),
                        UserEntity(104, "AG-04", "Valentina Ignacia Paredes Silva", "17.781.435-0", "vparedes@pagoflex.ejemplo", hash, salt, "AGENTE", "+56 9 5555 0204", "Ñuñoa", null, "Comercial", "Agente comercial", true, "22-01-2024", 3, true, null, null, null, pregDefault, respDefault),
                        UserEntity(105, "AG-05", "Tomás Alejandro Vidal Rojas", "19.564.028-9", "tvidal@pagoflex.ejemplo", hash, salt, "AGENTE", "+56 9 5555 0205", "La Florida", null, "Comercial", "Agente comercial", true, "09-06-2024", 3, true, null, null, null, pregDefault, respDefault)
                    )

                    agentes.forEach { u.insert(it) }

                    val ejecutivos = listOf(
                        UserEntity(201, "EJ-01", "Paula Andrea Saavedra Núñez", "14.629.853-2", "psaavedra@altosmaipo.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0401", "San José de Maipo", 1001, "Cobranza", "Ejecutiva de cobranza", true, "10-03-2023", 3, true, "Altos del Maipo", null, null, pregDefault, respDefault),
                        UserEntity(202, "EJ-02", "Marco Antonio Fuentes Riquelme", "11.853.720-3", "mfuentes@bosquenativo.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0402", "Providencia", 1002, "Finanzas", "Encargado de finanzas", true, "15-05-2023", 3, true, "Bosque Nativo", null, null, pregDefault, respDefault),
                        UserEntity(203, "EJ-03", "Claudia Beatriz Henríquez Soto", "15.370.946-7", "chenriquez@creditoandino.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0403", "Las Condes", 1003, "Cobranza", "Jefa de cobranza", true, "20-07-2023", 3, true, "Crédito Andino", null, null, pregDefault, respDefault),
                        UserEntity(204, "EJ-04", "Felipe Ignacio Morales Tapia", "17.095.482-3", "fmorales@creditoandino.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0404", "Vitacura", 1003, "Cobranza", "Ejecutivo de cobranza", true, "12-09-2023", 3, true, "Crédito Andino", null, null, pregDefault, respDefault),
                        UserEntity(205, "EJ-05", "Gloria Inés Carrasco Vega", "9.846.217-1", "gcarrasco@loshalcones.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0405", "La Reina", 1006, "Finanzas", "Tesorera", true, "03-02-2024", 3, true, "Los Halcones", null, null, pregDefault, respDefault),
                        UserEntity(206, "EJ-06", "Sebastián Andrés Núñez Olivares", "18.930.571-0", "snunez@itcordillera.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0406", "Peñalolén", 1007, "Finanzas", "Analista de finanzas", true, "18-04-2024", 3, true, "IT Cordillera", null, null, pregDefault, respDefault),
                        UserEntity(207, "EJ-07", "Mónica Patricia Leiva Castro", "12.584.396-4", "mleiva@plazaoriente.ejemplo", hash, salt, "EJECUTIVO", "+56 9 0000 0407", "La Florida", 1008, "Cobranza", "Administradora de cobranza", true, "25-06-2024", 3, true, "Plaza Oriente", null, null, pregDefault, respDefault)
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
                    r.insertMensaje(ReporteMensajeEntity(idReporte = 11, idRemitente = 4, rolRemitente = "USUARIO", contenido = "Pagué por transferencia directa al instituto el 03-10.", fechaHora = "03-10-2026"))
                    r.insert(ReporteEntity(id = 9, tipo = "El monto no corresponde", compromiso = "CP-2026-0111", descripcion = "Repacté el crédito y me siguen cobrando la cuota antigua.", fecha = "18-09-2026", idUsuario = 6))
                    r.insertMensaje(ReporteMensajeEntity(idReporte = 9, idRemitente = 6, rolRemitente = "USUARIO", contenido = "Repacté el crédito y me siguen cobrando la cuota antigua.", fechaHora = "18-09-2026"))
                    r.insert(ReporteEntity(id = 7, tipo = "No reconozco este cobro", compromiso = "CP-2026-0081", descripcion = "Me aparece una matrícula que ya pagué en marzo.", fecha = "02-09-2026", idUsuario = 5))
                    r.insertMensaje(ReporteMensajeEntity(idReporte = 7, idRemitente = 5, rolRemitente = "USUARIO", contenido = "Me aparece una matrícula que ya pagué en marzo.", fechaHora = "02-09-2026"))
                    r.insert(ReporteEntity(id = 5, tipo = "Otro", compromiso = "CP-2026-0064", descripcion = "El concepto dice \"agosto\" y es la cuota de julio.", fecha = "28-07-2026", idUsuario = 1))
                    r.insertMensaje(ReporteMensajeEntity(idReporte = 5, idRemitente = 1, rolRemitente = "USUARIO", contenido = "El concepto dice \"agosto\" y es la cuota de julio.", fechaHora = "28-07-2026"))
                    r.insert(ReporteEntity(id = 12, tipo = "El monto no corresponde", compromiso = "CP-2026-0106", descripcion = "No sé por qué me cobran $318.500.", fecha = "05-10-2026", idUsuario = 2))
                    r.insertMensaje(ReporteMensajeEntity(idReporte = 12, idRemitente = 2, rolRemitente = "USUARIO", contenido = "No sé por qué me cobran $318.500.", fechaHora = "05-10-2026"))
                    r.insert(ReporteEntity(id = 10, tipo = "No reconozco este cobro", compromiso = "CP-2026-0095", descripcion = "Me llegó un cobro de un club al que no pertenezco.", fecha = "30-09-2026", idUsuario = 8))
                    r.insertMensaje(ReporteMensajeEntity(idReporte = 10, idRemitente = 8, rolRemitente = "USUARIO", contenido = "Me llegó un cobro de un club al que no pertenezco.", fechaHora = "30-09-2026"))

                    seeded = true
                    android.util.Log.d("PagoFlexSeed", "seed done count=${db.userDao().count()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PagoFlexSeed", "seed failed", e)
            }
        }

        private suspend fun seedDemo(db: AppDatabase) {
            val userId = 9L
            val c = db.compromisoDao()
            val p = db.pagoDao()
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
                    android.util.Log.d("PagoFlexSeed", "inserting compromisos")
                    val compromisos = listOf(
                        Triple("CP-DEMO-001", "Compromiso próximo a vencer en 3 días", 3),
                        Triple("CP-DEMO-002", "Compromiso próximo a vencer en 7 días", 7),
                        Triple("CP-DEMO-003", "Compromiso pendiente a 15 días", 15),
                        Triple("CP-DEMO-004", "Compromiso vencido hace 2 días", -2)
                    )
                    compromisos.forEachIndexed { idx, (folio, concepto, dias) ->
                        val fecha = now.plusDays(dias.toLong()).format(fmt)
                        val estado = if (dias < 0) "VENCIDO" else "PENDIENTE"
                        val recargo = if (dias < 0) 12000 else 0
                        c.insert(CompromisoEntity(9000L + idx, folio, userId, 1003L, "Crédito Andino", concepto, 1, 12, 50000 + idx * 10000, fecha, estado, recargo))
                    }
                    c.insert(CompromisoEntity(9004L, "CP-DEMO-005", userId, 1003L, "Crédito Andino", "Compromiso pagado", 1, 12, 30000, now.minusDays(10).format(fmt), "PAGADO", 0))
                    p.insert(PagoEntity(9001L, "CPR-DEMO-001", 9004L, now.minusDays(10).format(fmt) + " 10:00", 30000, "App", "Pago simulado"))
                }
                demoSeeded = true
                android.util.Log.d("PagoFlexSeed", "demo seed done")
            } catch (e: Exception) {
                android.util.Log.e("PagoFlexSeed", "demo seed failed", e)
            }
        }

        private suspend fun limpiarUsuariosMock(db: AppDatabase) {
            try {
                db.userDao().getIdsUsuariosMock().forEach { uid ->
                    db.mensajeDao().deleteByUsuario(uid)
                    db.conversacionDao().deleteByUsuario(uid)
                }
                val eliminados = db.userDao().deleteUsuariosMock()
                if (eliminados > 0) {
                    android.util.Log.d("PagoFlexSeed", "usuarios mock eliminados=$eliminados")
                }
            } catch (e: Exception) {
                android.util.Log.e("PagoFlexSeed", "limpieza de usuarios mock failed", e)
            }
        }

        private suspend fun seedConversacionesAtencion(db: AppDatabase) {
            val c = db.conversacionDao()
            val m = db.mensajeDao()
            try {
                val fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")
                val now = java.time.LocalDateTime.now()
                val solicitudes = listOf(
                    1L to "Realicé un pago ayer y todavía no aparece reflejado en mis compromisos.",
                    2L to "No puedo visualizar mis compromisos de pago en la aplicación.",
                    5L to "¿Cómo puedo descargar el comprobante de uno de mis pagos?"
                )
                solicitudes.forEach { (uid, msg) ->
                    if (c.getByUsuarioOne(uid) == null) {
                        val convId = c.insert(ConversacionEntity(idUsuario = uid, fechaCreacion = now.format(fmt), estado = "PENDIENTE"))
                        m.insert(MensajeEntity(idConversacion = convId, idRemitente = uid, rolRemitente = "USUARIO", contenido = msg, fechaHora = now.format(fmt)))
                    }
                }
                android.util.Log.d("PagoFlexSeed", "conversaciones de atencion listas")
            } catch (e: Exception) {
                android.util.Log.e("PagoFlexSeed", "conversaciones de atencion failed", e)
            }
        }

        private suspend fun seedMensajesAgentes(db: AppDatabase) {
            val cdao = db.conversacionDao()
            val mdao = db.mensajeDao()
            val fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")
            val now = java.time.LocalDateTime.now()
            val mensajes = mapOf(
                1L to "Hola, ¿podrían confirmar el estado de mi compromiso pendiente?",
                2L to "Tengo una consulta sobre la fecha de vencimiento de mi cuota.",
                3L to "Quisiera saber si mi pago fue registrado correctamente.",
                4L to "Necesito información sobre mi cuota pendiente.",
                5L to "Confirmo que recibí el comprobante de mi pago.",
                6L to "Tengo dudas sobre el monto pendiente.",
                7L to "¿Podrían revisar mi compromiso vencido?",
                8L to "Quisiera consultar el estado de mi cuenta.",
                9L to "Hola, necesito ayuda con mi compromiso pendiente."
            )
            for ((userId, texto) in mensajes) {
                if (cdao.getByUsuarioOne(userId) == null) {
                    val convId = cdao.insert(ConversacionEntity(idUsuario = userId, fechaCreacion = now.format(fmt), estado = "PENDIENTE"))
                    mdao.insert(MensajeEntity(idConversacion = convId, idRemitente = userId, rolRemitente = "USUARIO", contenido = texto, fechaHora = now.format(fmt)))
                }
            }
            android.util.Log.d("PagoFlexSeed", "seedMensajesAgentes done")
        }

        private suspend fun completarDatosUsuarios(db: AppDatabase) {
            try {
                val udao = db.userDao()
                val usuarios = udao.getAll().firstOrNull() ?: return
                // Migrar usuario 900 -> 9 si no existe ya
                val usuario900 = udao.getById(900L)
                if (usuario900 != null && udao.getById(9L) == null) {
                    udao.deleteById(900L)
                        udao.insert(usuario900.copy(id = 9L, codigo = "UF-09", nombre = "Valentina Isabel Herrera Soto", email = "valentina.herrera@correo.ejemplo", agenteAsignadoId = 105L))
                    db.compromisoDao().getAllSync().filter { it.idUsuario == 900L }.forEach { c ->
                        db.compromisoDao().update(c.copy(idUsuario = 9L))
                    }
                } else {
                    usuarios.find { it.id == 9L }?.let { u ->
                        // Conserva los datos que el usuario haya editado en su
                        // perfil; solo completa lo que falte o sea invalido.
                        val nombre = u.nombre.takeIf { it.isNotBlank() }
                            ?: "Valentina Isabel Herrera Soto"
                        val codigo = u.codigo?.takeIf { it.isNotBlank() } ?: "UF-09"
                        val email = u.email.takeIf {
                            it.isNotBlank() && Validaciones.correo(it) == null
                        } ?: "valentina.herrera@correo.ejemplo"
                        if (
                            u.nombre != nombre ||
                            u.codigo != codigo ||
                            u.email != email ||
                            u.agenteAsignadoId == null
                        ) {
                            udao.update(
                                u.copy(
                                    nombre = nombre,
                                    codigo = codigo,
                                    email = email,
                                    agenteAsignadoId = u.agenteAsignadoId ?: 105L
                                )
                            )
                        }
                    }
                }
                val agentes = usuarios.filter { it.rol == "AGENTE" && it.id != 902L }
                val clientes = usuarios.filter { it.rol == "USUARIO" && it.id != 901L }

                val asignaciones = mapOf(
                    1L to 101L,
                    2L to 101L,
                    3L to 102L,
                    4L to 102L,
                    5L to 103L,
                    6L to 103L,
                    7L to 104L,
                    8L to 104L,
                    9L to 105L
                )
                asignaciones.forEach { (userId, agenteId) ->
                    val user = clientes.find { it.id == userId } ?: udao.getById(userId)
                    if (user != null && user.agenteAsignadoId != agenteId) {
                        udao.update(user.copy(agenteAsignadoId = agenteId))
                    }
                }

                usuarios.forEach { user ->
                    var updated = user
                    var changed = false
                    if (updated.rut == null) {
                        updated = updated.copy(rut = "RUT-${updated.id}")
                        changed = true
                    }
                    if (updated.telefono == null) {
                        updated = updated.copy(telefono = "+56 9 ${10000000 + (updated.id.toInt() * 1234)}")
                        changed = true
                    }
                    if (updated.comuna == null) {
                        updated = updated.copy(comuna = listOf("Santiago", "Maipú", "Puente Alto", "La Florida", "Ñuñoa").getOrElse((updated.id % 5).toInt()) { "Santiago" })
                        changed = true
                    }
                    if (updated.fechaIncorporacion == null) {
                        updated = updated.copy(fechaIncorporacion = "01-01-2024")
                        changed = true
                    }
                    if (updated.empresa == null && updated.empresaId != null) {
                        val empresa = db.empresaDao().getById(updated.empresaId)
                        if (empresa != null) {
                            updated = updated.copy(empresa = empresa.nombreFantasia)
                            changed = true
                        }
                    }
                    if (changed) {
                        udao.update(updated)
                    }
                }
                android.util.Log.d("PagoFlexSeed", "completarDatosUsuarios done")
            } catch (e: Exception) {
                android.util.Log.e("PagoFlexSeed", "completarDatosUsuarios failed", e)
            }
        }

        /**
         * Agrega las cuentas demo, nuevas empresas y compromisos de ejemplo.
         * Se ejecuta en cada apertura, pero solo inserta lo que aun no existe.
         */
        private suspend fun seedDatosExtendidos(db: AppDatabase) {
            if (datosExtendidosListos) return
            datosExtendidosLock.withLock {
            if (datosExtendidosListos) return@withLock
            try {
                val empresaDao = db.empresaDao()
                val nuevasEmpresas = listOf(
                    EmpresaEntity(1009, "EC-09", "Banco del Sur S.A.", "Banco del Sur", "97.123.456-7", "Banca", "clientes@bancodelsur.ejemplo", "+56 2 0000 0209", 9000),
                    EmpresaEntity(1010, "EC-10", "Financiera Sol SpA", "Financiera Sol", "77.654.321-K", "Crédito de consumo", "contacto@financierasol.ejemplo", "+56 2 0000 0210", 6000),
                    EmpresaEntity(1011, "EC-11", "Retail Norte S.A.", "Retail Norte", "96.876.543-2", "Retail", "pagos@retailnorte.ejemplo", "+56 2 0000 0211", 4000)
                )
                nuevasEmpresas.forEach { empresa ->
                    if (empresaDao.getById(empresa.id) == null) empresaDao.insert(empresa)
                }

                val hasher = com.example.pagoflex.security.PasswordHasher
                val salt = hasher.generateSalt()
                val hash = hasher.hash("PagoFlex123!", salt)
                val pregDefault = PreguntasSeguridad.PREGUNTA_DEFAULT
                val respDefault = PreguntasSeguridad.RESPUESTA_DEFAULT

                val ejecutivosNuevos = listOf(
                    UserEntity(208, "EJ-08", "Javiera Paz Fuentealba Ruiz", "16.904.285-3", "jfuentealba@bancodelsur.ejemplo", hash, salt, "EJECUTIVO", "+56 9 6000 0208", "Las Condes", 1009, "Cobranza", "Ejecutiva de cobranza", true, "08-01-2024", 3, true, "Banco del Sur", null, null, pregDefault, respDefault),
                    UserEntity(209, "EJ-09", "Rodrigo Andrés Salas Mena", "14.287.650-1", "rsalas@financierasol.ejemplo", hash, salt, "EJECUTIVO", "+56 9 6000 0209", "Santiago", 1010, "Finanzas", "Analista de crédito", true, "19-03-2024", 3, true, "Financiera Sol", null, null, pregDefault, respDefault),
                    UserEntity(210, "EJ-10", "Carolina Andrea Poblete Rivas", "18.650.294-6", "cpoblete@retailnorte.ejemplo", hash, salt, "EJECUTIVO", "+56 9 6000 0210", "Maipú", 1011, "Cobranza", "Encargada de cobranza", true, "02-06-2024", 3, true, "Retail Norte", null, null, pregDefault, respDefault)
                )
                ejecutivosNuevos.forEach { asegurarUsuarioPrueba(db, it, hash, salt, pregDefault, respDefault) }

                val usuariosPrueba = listOf(
                    UserEntity(901, "TEST-USUARIO", "Usuario Prueba", "11.111.111-1", "usuario.prueba@pagoflex.cl", hash, salt, "USUARIO", "+56 9 6000 0001", "Santiago", null, null, null, true, "01-01-2024", 3, true, null, null, null, pregDefault, respDefault),
                    UserEntity(902, "TEST-EJECUTIVO", "Ejecutivo Prueba", "12.222.222-2", "ejecutivo.prueba@pagoflex.cl", hash, salt, "EJECUTIVO", "+56 9 6000 0002", "Las Condes", 1009, "Cobranza", "Ejecutivo de cobranza", true, "01-01-2024", 3, true, "Banco del Sur", null, null, pregDefault, respDefault),
                    UserEntity(903, "TEST-AGENTE", "Agente Prueba", "13.333.333-3", "agente.prueba@pagoflex.cl", hash, salt, "AGENTE", "+56 9 6000 0003", "Providencia", null, "Soporte", "Agente de soporte", true, "01-01-2024", 3, true, null, null, null, pregDefault, respDefault)
                )
                usuariosPrueba.forEach { asegurarUsuarioPrueba(db, it, hash, salt, pregDefault, respDefault) }

                val usuarioPrueba = db.userDao().getByEmail("usuario.prueba@pagoflex.cl")
                val agentePrueba = db.userDao().getByEmail("agente.prueba@pagoflex.cl")
                if (usuarioPrueba != null && agentePrueba != null && usuarioPrueba.agenteAsignadoId != agentePrueba.id) {
                    db.userDao().update(usuarioPrueba.copy(agenteAsignadoId = agentePrueba.id))
                }

                val fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu")
                val fmtHora = java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")
                val hoy = java.time.LocalDate.now()

                val compromisos = mutableListOf(
                    CompromisoEntity(120, "CP-2026-0120", 1, 1009, "Banco del Sur", "Crédito de consumo", 6, 18, 98000, "28-11-2026", "PENDIENTE", 0),
                    CompromisoEntity(121, "CP-2026-0121", 2, 1009, "Banco del Sur", "Crédito de consumo", 9, 12, 64000, "05-12-2026", "PENDIENTE", 0),
                    CompromisoEntity(122, "CP-2026-0122", 3, 1010, "Financiera Sol", "Crédito personal", 2, 10, 125000, "30-11-2026", "PENDIENTE", 0),
                    CompromisoEntity(123, "CP-2026-0123", 4, 1010, "Financiera Sol", "Crédito personal", 4, 8, 78000, "20-10-2026", "VENCIDO", 6000),
                    CompromisoEntity(124, "CP-2026-0124", 7, 1011, "Retail Norte", "Compra en cuotas", 3, 6, 45990, "10-12-2026", "PENDIENTE", 0),
                    CompromisoEntity(125, "CP-2026-0125", 8, 1011, "Retail Norte", "Compra en cuotas", 5, 6, 45990, "15-10-2026", "EN_REVISION", 0),
                    CompromisoEntity(126, "CP-2026-0126", 5, 1009, "Banco del Sur", "Crédito de consumo", 2, 12, 55000, "01-10-2026", "PAGADO", 0)
                )

                val usuarioPruebaId = usuarioPrueba?.id
                if (usuarioPruebaId != null) {
                    compromisos += listOf(
                        CompromisoEntity(9201, "CP-2026-0201", usuarioPruebaId, 1009, "Banco del Sur", "Crédito de consumo", 4, 18, 98000, hoy.plusDays(5).format(fmt), "PENDIENTE", 0),
                        CompromisoEntity(9202, "CP-2026-0202", usuarioPruebaId, 1010, "Financiera Sol", "Crédito personal", 5, 12, 64000, hoy.minusDays(4).format(fmt), "VENCIDO", 6000),
                        CompromisoEntity(9203, "CP-2026-0203", usuarioPruebaId, 1011, "Retail Norte", "Compra en cuotas", 2, 6, 45990, hoy.plusDays(12).format(fmt), "PENDIENTE", 0),
                        CompromisoEntity(9204, "CP-2026-0204", usuarioPruebaId, 1009, "Banco del Sur", "Crédito de consumo", 3, 18, 98000, hoy.minusDays(35).format(fmt), "PAGADO", 0)
                    )
                }

                val foliosExistentes = db.compromisoDao().getAllSync().map { it.folio }.toMutableSet()
                compromisos.forEach { compromiso ->
                    if (foliosExistentes.add(compromiso.folio)) {
                        db.compromisoDao().insert(compromiso)
                    }
                }

                if (db.pagoDao().getByCompromiso(126) == null) {
                    db.pagoDao().insert(
                        PagoEntity(
                            id = 9202L,
                            numeroComprobante = "CPR-009202",
                            idCompromiso = 126L,
                            fechaHora = "28-09-2026 12:00",
                            montoPagado = 55000,
                            canal = "Portal de pagos",
                            medioPago = "Pago simulado",
                            folioUnico = FolioGenerator.generar(),
                            cuotaPagada = 2,
                            fechaVencimientoCuota = "01-10-2026"
                        )
                    )
                }

                if (db.pagoDao().getByCompromiso(9204) == null) {
                    db.pagoDao().insert(
                        PagoEntity(
                            id = 9201L,
                            numeroComprobante = "CPR-009201",
                            idCompromiso = 9204L,
                            fechaHora = hoy.minusDays(35).format(fmt) + " 10:30",
                            montoPagado = 98000,
                            canal = "App",
                            medioPago = "Pago simulado",
                            folioUnico = FolioGenerator.generar(),
                            cuotaPagada = 3,
                            fechaVencimientoCuota = hoy.minusDays(35).format(fmt)
                        )
                    )
                }

                if (usuarioPruebaId != null && db.reporteDao().countByUsuario(usuarioPruebaId) == 0) {
                    val descripcionReporte = "La cuota de Financiera Sol muestra un recargo que no reconozco."
                    val fechaReporte = hoy.minusDays(1).format(fmt)
                    val reporteId = db.reporteDao().insert(
                        ReporteEntity(
                            tipo = "El monto no corresponde",
                            compromiso = "CP-2026-0202",
                            descripcion = descripcionReporte,
                            fecha = fechaReporte,
                            idUsuario = usuarioPruebaId
                        )
                    )
                    db.reporteDao().insertMensaje(
                        ReporteMensajeEntity(
                            idReporte = reporteId,
                            idRemitente = usuarioPruebaId,
                            rolRemitente = "USUARIO",
                            contenido = descripcionReporte,
                            fechaHora = fechaReporte
                        )
                    )
                }

                if (usuarioPruebaId != null && db.conversacionDao().getByUsuarioOne(usuarioPruebaId) == null) {
                    val convId = db.conversacionDao().insert(
                        ConversacionEntity(
                            idUsuario = usuarioPruebaId,
                            fechaCreacion = java.time.LocalDateTime.now().format(fmtHora),
                            estado = "PENDIENTE"
                        )
                    )
                    db.mensajeDao().insert(
                        MensajeEntity(
                            idConversacion = convId,
                            idRemitente = usuarioPruebaId,
                            rolRemitente = "USUARIO",
                            contenido = "Hola, necesito ayuda con mi cuota de Banco del Sur.",
                            fechaHora = java.time.LocalDateTime.now().format(fmtHora)
                        )
                    )
                }

                datosExtendidosListos = true
                android.util.Log.d("PagoFlexSeed", "seedDatosExtendidos done")
            } catch (e: Exception) {
                android.util.Log.e("PagoFlexSeed", "seedDatosExtendidos failed", e)
            }
            }
        }

        private suspend fun asegurarUsuarioPrueba(
            db: AppDatabase,
            modelo: UserEntity,
            hash: String,
            salt: String,
            pregDefault: String,
            respDefault: String
        ) {
            val dao = db.userDao()
            val actual = dao.getByEmail(modelo.email) ?: dao.getById(modelo.id)
            if (actual == null) {
                dao.insert(modelo)
                return
            }
            // Solo completa datos faltantes o invalidos: nunca pisa ediciones
            // validas que el usuario haya hecho desde su perfil.
            val emailActual = actual.email.takeIf {
                it.isNotBlank() && Validaciones.correo(it) == null
            } ?: modelo.email
            val telefonoActual = actual.telefono?.takeIf {
                it.isNotBlank() && Validaciones.telefonoChileno(it) == null
            } ?: modelo.telefono
            dao.update(
                actual.copy(
                    codigo = actual.codigo ?: modelo.codigo,
                    nombre = actual.nombre.takeIf { it.isNotBlank() } ?: modelo.nombre,
                    email = emailActual,
                    rol = actual.rol.takeIf { it.isNotBlank() } ?: modelo.rol,
                    telefono = telefonoActual,
                    comuna = actual.comuna?.takeIf { it.isNotBlank() } ?: modelo.comuna,
                    rut = actual.rut?.takeIf { it.isNotBlank() } ?: modelo.rut,
                    empresaId = actual.empresaId ?: modelo.empresaId,
                    area = actual.area?.takeIf { it.isNotBlank() } ?: modelo.area,
                    cargo = actual.cargo?.takeIf { it.isNotBlank() } ?: modelo.cargo,
                    // Conserva el estado habilitado/deshabilitado que el agente
                    // haya definido desde la administración de usuarios.
                    activo = actual.activo,
                    fechaIncorporacion = actual.fechaIncorporacion ?: modelo.fechaIncorporacion,
                    empresa = actual.empresa?.takeIf { it.isNotBlank() } ?: modelo.empresa,
                    agenteAsignadoId = actual.agenteAsignadoId ?: modelo.agenteAsignadoId,
                    passwordHash = actual.passwordHash.ifBlank { hash },
                    salt = actual.salt.ifBlank { salt },
                    preguntaSeguridad = actual.preguntaSeguridad.ifBlank { pregDefault },
                    respuestaSeguridad = actual.respuestaSeguridad.ifBlank { respDefault }
                )
            )
        }
    }
}