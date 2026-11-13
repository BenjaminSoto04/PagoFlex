# AGENTS.md

Single-module Android app (`:app`): Kotlin + Jetpack Compose + Room, namespace `com.example.pagoflex`.
All persistence is local; payments are simulated (no backend/Retrofit).

## Build and verify

- Windows: `.\gradlew.bat :app:assembleDebug` (verified; APK at `app/build/outputs/apk/debug/app-debug.apk`).
- Fast compile check: `.\gradlew.bat :app:compileDebugKotlin`.
- There are **no** unit or instrumentation test sources; `test`/`androidTest` are empty. Verify changes by compiling and, when relevant, running the app.
- Toolchain: AGP 9.4.1, Gradle 9.6.0, Kotlin 2.2.10, compileSdk 37, minSdk 24, Java 11 target with desugaring. Keep the existing AGP 9 DSL (`compileSdk { version = release(37) }`) when editing `app/build.gradle.kts`.

## Architecture

- Entry flow: `MainActivity` -> `AppNavegacion` -> screens in `ui/screens` -> `viewmodel/*ViewModel` -> `data/repository` -> Room DAOs/entities.
- No DI framework. `RepositoryProvider` is the service locator; ViewModels are `AndroidViewModel` and obtain repositories via `RepositoryProvider.<repo>(application)`. Follow this pattern instead of constructing databases or DAOs in screens.
- Routes are centralized in `navigation/Rutas.kt`; every destination and its role guard lives in `navigation/AppNavegacion.kt`.
- Shared UI styles live in `ui/styles`: `EstiloCampo` for text fields, `EstilosTarjeta` for cards, and top-level `BotonPrimario`/`BotonSecundario`/`BotonCompacto`/`BotonTexto` in `EstiloBoton.kt`. Use them instead of raw Material components in new screens; form screens also need `imePadding()` + `verticalScroll()`. `ui/styles/Formatos.kt` has the shared money/date formatting helpers (`formatearPesos`, `formatearFecha`, `etiquetaEstado`, `parsearFecha`).
- Session state is split: `SessionManager` (SharedPreferences `pagoflex_session`) stores `userId`/`role`, while `AuthViewModel.currentUser` holds the loaded user. Navigation guards read `SessionManager`. `AuthViewModel` deliberately does not restore the session; the app always starts at login. Keep both stores in sync on login/logout/profile changes.
- `viewmodel/PagoViewModel.kt` actually declares `PagosViewModel`; don't search for a `PagoViewModel` class.

## Room and data gotchas

- Database `pagoflex.db` is version 16 with `exportSchema = false`. Any entity/schema change requires bumping `@Database(version = ...)` and adding a `Migration` in `AppDatabase.getDatabase`; destructive fallback is not enabled, so a missing migration crashes existing installs. `MIGRATION_15_16` (adds `compromisos.motivoAnulacion`) is the latest template.
- `AppDatabase.getDatabase` seeds demo companies, users, commitments, payments, reports, and chat conversations on access. Seeds are idempotent but run on the real database; demo/test IDs and accounts are hardcoded in `AppDatabase.kt`.
- Dates are stored as strings in `dd-MM-uuuu` or `dd-MM-uuuu HH:mm`; money is `Int` CLP. Keep that representation and the entity/model mapping in the repositories unless a migration is included.
- Commitments persist `EstadoCompromiso.name` as text (`PENDIENTE`, `VENCIDO`, `PAGADO`, `EN_REVISION`, `ANULADO`).
- Payment rules live in `PagosViewModel` (one payment per commitment, must pay oldest active commitment for the same company first). There is no payment gateway: paying only inserts a local `PagoEntity` and marks the commitment paid.

## Demo accounts and docs

- Seeded logins (all password `PagoFlex123!`): `usuario.prueba@pagoflex.cl`, `ejecutivo.prueba@pagoflex.cl`, `agente.prueba@pagoflex.cl`.
- `README.md` is stale in places: it mentions `usuario@pagoflex.cl` (use the `*.prueba@pagoflex.cl` accounts above) and claims notifications, but there is no notification implementation in the code.
