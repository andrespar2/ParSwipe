# ParSwipe v1.0.0

Organiza tu galería con gestos. Swipe para conservar o eliminar fotos, detecta
duplicados y recupera lo eliminado desde la papelera de Android. Todo el
procesamiento ocurre **100% en tu dispositivo**, sin internet.

## Características

- **Revisión por gestos**: desliza a la **izquierda** para *conservar* y a la
  **derecha** para *eliminar*. Los botones están alineados con el gesto
  (Conservar a la izquierda, Eliminar a la derecha).
- **Eliminaciones recuperables**: en Android 11+ las fotos se mueven a la
  **papelera del sistema** con una sola confirmación, y puedes restaurarlas desde
  la propia app.
- **Deshacer** los **3 movimientos** más recientes, incluidas las eliminaciones.
- **Detección de duplicados**: agrupa por tamaño y confirma por huella de
  contenido (SHA-256); conserva una copia y propone eliminar el resto.
- **Estadísticas**: fotos revisadas, eliminadas, espacio liberado, duplicados
  encontrados y estado de la papelera.
- **Diseño Modern Black AMOLED** (#000000), minimalista y fluido.
- **Sin límite de cantidad**: revisa tantas fotos como tengas.

## Requisitos

- Android 8.0 (API 26) o superior.
- En Android 11+ el espacio se libera definitivamente cuando el sistema vacía su
  papelera.

## Privacidad

- Todo el análisis y el borrado se ejecutan localmente.
- La app no solicita acceso a internet.

## Instalación

1. Descarga `ParSwipe-v1.0.0.apk`.
2. Ábrelo en el dispositivo y permite la instalación de apps de origen
   desconocido si el sistema lo pide.
3. Concede el permiso de acceso a fotos al abrir la app.

## Correcciones

- Compatibilidad con el acceso parcial a fotos de Android 14+ ("Seleccionar
  fotos").
- Botones de acción alineados con la dirección del gesto de swipe.

## Nota

Este APK está firmado con la clave de release de ParSwipe. Conserva la clave para
futuras actualizaciones.
