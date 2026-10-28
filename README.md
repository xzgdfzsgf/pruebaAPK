# ENV UI 0.5

Launcher para tablets Android con estética de escritorio, rediseñado por completo en esta versión para que todo sea personalizable desde el propio launcher y los cambios se vean en vivo.

## Novedades de la versión 0.5

1. Centro de control al tocar los iconos de wifi y batería de la barra, con accesos a Wi‑Fi, Bluetooth, tema, linterna, No molestar y pantalla, control de volumen y barras de batería y almacenamiento.
2. Widget de clima con datos de Open-Meteo, sin cuentas ni claves, configurado para Quito y con buscador de ciudades.
3. Widget de agenda con los eventos de hoy y mañana, que pide permiso al calendario solo cuando se activa.
4. Widget de reloj mundial con once ciudades y la diferencia horaria respecto a la tablet.
5. Menú Inicio con fila de apps más usadas, vista alfabética o por categorías y opción de ocultar apps, que se pueden volver a mostrar desde Personalizar.
6. Siete fondos integrados con versión clara y oscura, saludo con el nombre del usuario, vibración al mantener presionado y reloj que abre las alarmas al tocarlo.
7. Los widgets de cada fila se igualan en altura para un escritorio más ordenado.

## Novedades de la versión 0.4

1. Reloj configurable con cinco estilos (digital, analógico, apilado, fino y neón), cuatro tipografías, cuatro grosores, tamaño ajustable, formato de 12 o 24 horas, segundos, fecha, saludo según la hora, color de acento y alineación a la izquierda o al centro.
2. Tres disposiciones del escritorio (Escritorio, Minimalista y Productivo), con widgets que se pueden mostrar u ocultar de forma individual, entre ellos un calendario del mes, un indicador de batería con estado de carga, notas rápidas que se guardan solas y una cuadrícula de apps fijadas.
3. Apps fijadas y barra de tareas elegidas por el usuario, manteniendo presionada cualquier aplicación en el menú Inicio, que además permite ver la información de la app o desinstalarla.
4. Menú Inicio nuevo con búsqueda que ignora tildes, índice alfabético y apertura directa con la tecla Ir del teclado.
5. Barra de tareas flotante o anclada, con iconos centrados o a la izquierda, sin botones repetidos; la hora solo aparece en la barra cuando el reloj grande no está en pantalla, salvo que el usuario elija otra opción.
6. Apariencia con tema oscuro, claro o según el sistema, siete colores de acento, opacidad del cristal y redondez de esquinas.
7. Fondo de pantalla con control de oscurecimiento y desenfoque (el desenfoque requiere Android 12 o superior).
8. Sección de bloqueo y reposo, que permite aplicar el fondo elegido a la pantalla de bloqueo del sistema y activar «Reposo ENV UI», un protector de pantalla con el mismo estilo de reloj que se muestra mientras la tablet se carga.

## Sobre la pantalla de bloqueo

Android no permite que un launcher reemplace la pantalla de bloqueo del sistema, porque es la que protege el PIN, el patrón o la huella. ENV UI sí puede cambiar su fondo y ofrece el protector de pantalla «Reposo ENV UI», que se activa desde Ajustes del sistema, en la sección de pantalla o protector de pantalla, según el fabricante.

## Compilación

El repositorio incluye el flujo `.github/workflows/build-apk.yml`, que compila el APK de depuración en GitHub Actions cada vez que se sube un cambio a la rama main. El APK queda disponible como artefacto `ENV-UI-debug-apk` en la ejecución correspondiente.

## Precaución

Conserva el launcher original del fabricante para poder volver a él desde Ajustes, Aplicaciones predeterminadas, Aplicación de inicio.
