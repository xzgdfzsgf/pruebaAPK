# ENV UI — Premium preview 0.2.0

Launcher Android con estética Windows 11: escritorio azul noche, gradientes, paneles translúcidos, barra flotante con sombra, reloj, fecha, porcentaje real de batería, cajón de aplicaciones con búsqueda, iconos reales, tema claro/oscuro persistente y adaptación horizontal/vertical.

## Compilar en Windows
1. Instala Android Studio (JDK 17 integrado) y Android SDK 35.
2. Abre la carpeta `ENV-UI` (la que contiene `settings.gradle.kts`).
3. Deja que Android Studio sincronice Gradle. Si solicita configurar Gradle, utiliza una versión 8.10.2 compatible con Android Gradle Plugin 8.7.3 y JDK 17. Este ZIP no incluye Gradle Wrapper; Android Studio puede necesitar una instalación de Gradle o generar el wrapper.
4. Menú **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Si compila, el APK estará en `app/build/outputs/apk/debug/app-debug.apk`.
6. Transfiérelo a la tablet, instala y selecciona ENV UI como launcher predeterminado.

## Estado de validación
- Revisión estática del código y estructura: realizada.
- Compilación APK: **NO ejecutada** (sin Android SDK/Gradle disponibles).
- Prueba visual en tablet: **pendiente**.
- No hay widgets Android de terceros, clima real, fondos configurables, ni ventanas flotantes todavía.
- Los iconos y apps disponibles dependen del firmware de la tablet.

## Precaución
Conserva el launcher original para poder volver a él desde Ajustes > Aplicaciones predeterminadas > Inicio.


## Novedad v0.3: fondo personalizado

Abre el icono de ajustes en la barra de tareas → **Fondo de pantalla** → **Elegir imagen**.
Selecciona una foto desde el selector de documentos de Android. La imagen queda guardada mediante una URI con permiso persistente y vuelve a aparecer al reiniciar el launcher. Usa **Restablecer fondo original** para recuperar el degradado de ENV UI. La foto se recorta para llenar la pantalla en horizontal y vertical; los widgets se muestran con una capa de contraste para facilitar la lectura. No se solicitan permisos de almacenamiento generales.

Nota: el selector debe proporcionar una URI que permita lectura persistente; si el proveedor de imágenes revoca el acceso, será necesario seleccionar la foto de nuevo. Proyecto no compilado ni probado en el dispositivo.
