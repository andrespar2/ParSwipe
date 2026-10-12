# ParSwipe

Aplicación Android para organizar, conservar y eliminar fotografías mediante gestos
intuitivos de deslizamiento (estilo *swipe*). Todo el procesamiento es **local**: la app
no usa internet ni envía datos a ningún servidor.

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4)

## Funcionalidades

- **Swipe a la derecha** → eliminar la fotografía.
- **Swipe a la izquierda** → conservarla.
- **Deshacer los últimos 3 movimientos**, incluidas las eliminaciones en cola.
- **Detección de duplicados** por tamaño + huella de contenido (SHA-256 de muestra),
  con borrado selectivo conservando una copia.
- **Revisión sin límites**: recorre toda la galería, con recarga manual.
- **Estadísticas**: fotos revisadas, eliminadas y almacenamiento liberado.
- **Papelera recuperable de Android** (Android 11+) para eliminaciones y restauración.
- **Diseño Modern Black AMOLED**: negro puro `#000000`, minimalista y fluido.

## Cómo se eliminan las fotos

ParSwipe nunca borra nada sin autorización del sistema:

1. Al deslizar a la derecha, la foto entra en una **cola de eliminación** (reversible con
   *Deshacer*).
2. Pulsando **Aplicar**, Android 11+ muestra su diálogo nativo y mueve las fotos a la
   **papelera del sistema** (recuperables desde la galería). Android 10 o inferior no
   dispone de papelera, por lo que el borrado es definitivo y se avisa en la interfaz.
3. Tras aplicar, aparece la opción **Restaurar** para recuperar el último lote.

La app no accede a la red y no declara el permiso `INTERNET`.

## Estructura

```
app/src/main/java/com/parswipe/app/
├── MainActivity.kt                  # Entrada, edge-to-edge y tema AMOLED
├── ParSwipeViewModel.kt             # Estado: mazo, historial (3 undos), estadísticas
├── data/
│   ├── MediaStoreRepository.kt      # Lectura de galería + detección de duplicados
│   ├── MediaActions.kt              # Papelera de Android (API 30+) y borrado legacy
│   ├── StatsStore.kt                # Estadísticas persistidas
│   ├── Permissions.kt               # Permisos según versión de Android
│   └── PhotoItem.kt                 # Modelos
└── ui/
    ├── ParSwipeApp.kt               # Navegación inferior y permisos
    ├── ReviewScreen.kt              # Mazo con gestos, badges y acciones
    ├── DuplicatesScreen.kt          # Grupos de duplicados
    ├── StatsScreen.kt               # Estadísticas
    ├── Common.kt                    # Componentes reutilizables
    └── theme/Theme.kt               # Paleta AMOLED
```

## Compilar

Requisitos: JDK 17 y Android SDK (platform 34, build-tools 34.0.0).

```bash
# APK de depuración
./gradlew :app:assembleDebug

# APK de release firmado (requiere keystore.properties)
./gradlew :app:assembleRelease
```

### Firma

Crea `keystore.properties` en la raíz (no se versiona):

```properties
storeFile=release/parswipe.jks
storePassword=...
keyAlias=parswipe
keyPassword=...
```

Si el archivo no existe, el release se genera sin firmar.

## Instalar

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.parswipe.app/.MainActivity
```

## Privacidad

- Sin permiso de INTERNET.
- Sin librerías de analítica ni publicidad.
- Las imágenes se leen y analizan únicamente en el dispositivo.
