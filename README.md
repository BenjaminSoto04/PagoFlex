# PagoFlex

Aplicación Android desarrollada en Kotlin con Jetpack Compose para la gestión de compromisos de pago.

## Objetivo del sistema
Permitir a usuarios visualizar y pagar compromisos, a ejecutivos de empresa crear/asignar/modificar/anular compromisos, y a agentes consultar información, con persistencia local.

## Integrantes del equipo
- (Completar nombres)

## Tecnologías utilizadas
- Kotlin, Jetpack Compose, Navigation Compose
- Room (SQLite) para persistencia local
- ViewModel + StateFlow
- Recursos nativos: notificaciones y cámara

## Funcionalidades implementadas
- Autenticación con roles (Usuario, Ejecutivo, Agente)
- Registro, perfil, cambio y recuperación de contraseña
- Persistencia Room de usuarios, compromisos, pagos, comprobantes y reportes
- Gestión de compromisos por ejecutivo y visualización por usuario
- Validaciones centralizadas en ViewModels
- Notificaciones de vencimiento y captura de fotos de comprobantes
- Animaciones de transición

## Roles
- Usuario: visualiza y paga sus compromisos.
- Ejecutivo de empresa: crea, modifica y anula compromisos.
- Agente: consulta y asistencia.

## Arquitectura
Capas: UI (Compose) -> ViewModel -> Repository -> Room. Repositorios desacoplados para futura integración REST/Retrofit.

## Persistencia con Room
Entidades: UserEntity, CompromisoEntity, PagoEntity, ComprobanteEntity, ReporteEntity. DAOs y AppDatabase. Seed inicial.

## Recursos nativos
- Notificaciones: canal y permiso POST_NOTIFICATIONS (Android 13+).
- Cámara: captura de foto de comprobante con FileProvider.

## Instrucciones
Abrir en Android Studio, sincronizar Gradle, ejecutar en emulador/dispositivo API 24+.
Credenciales demo: usuario@pagoflex.cl / PagoFlex123! (igual para ejecutivo y agente).
