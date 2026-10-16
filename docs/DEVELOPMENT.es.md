# Buhito 🦉 — notas de desarrollo (español)

App Android nativa en Kotlin + Jetpack Compose. 100 % local, sin cuenta ni servidor.

## Cómo abrirla
1. Android Studio (Ladybug o más nuevo) → **Open** → carpeta `Buhito`.
2. Esperá el *Gradle sync* (descarga Gradle 8.9 y las dependencias; necesita internet).
3. Conectá el celular con depuración USB y tocá **Run**.

Compatibilidad: `minSdk 26` (Android 8.0) hasta `targetSdk 35`.

## Fase 1 (MVP)
- Crear, editar, activar/desactivar y borrar alarmas (deslizar a la izquierda también borra).
- Sonido: tonos del sistema o un audio del teléfono copiado a la carpeta interna de la app, con previsualización.
- Suena con el celular bloqueado: audio en bucle, vibración, pantalla de alarma sobre el bloqueo, Detener y Posponer 5/10/15 min.
- Duración configurable (por defecto 2 min). Se reprograman al reiniciar el teléfono.

## Fase 2
- **Repetir por días**: una vez, lunes a viernes, todos los días o los días que elijas (L M X J V S D).
- **Categorías**: General, Estudio, Gimnasio, Trabajo, Salud y Personal, cada una con emoji y color. Aparecen en la tarjeta y en la pantalla de alarma.
- **Filtros** en la pantalla principal: Todas, Activas, Pausadas y por categoría.
- **Grabar tu voz** desde la app (pide el permiso del micrófono al tocar el botón). Se guarda en la carpeta interna.
- **Volumen** por alarma (10–100 %) y **repetición del sonido**: encendido = bucle durante toda la duración; apagado = el sonido suena una vez, pero la pantalla, la vibración y la notificación siguen todo el tiempo de duración (ideal para mensajes de voz).
- **Plantillas**: en el editor, "Guardar como plantilla"; al crear una alarma nueva aparecen arriba para usarlas con un toque (mantené presionado para borrar).
- **Animalitos con personalidad**
  - Búho: saluda según la hora, dice frases al tocarlo (salta y se pone contento), se duerme si no hay alarmas activas, se agita cuando suena la alarma y *escucha* (inclina la cabeza, emite ondas y sus pupilas siguen tu voz) mientras grabás.
  - Gato: dormido con "z" flotando; al tocarlo se despierta, mueve la oreja, ronronea con corazoncitos, dice algo y vuelve a dormirse.
  - Cielo con estrellas que titilan y una estrella fugaz cada ~11 s.
  - Al guardar una alarma, festejo con el búho y confeti.

## Versión 1.0
- **Detener con QR**: el código personal se genera en Ajustes (guardá la imagen y/o imprimila). La alarma con "Escanear QR" pide escanearlo con la cámara (CameraX + ZXing); si no hay permiso de cámara o cámara, vuelve a "mantener para detener" para no quedarte sin poder apagarla. Base de datos v7 (`qrStop`) con migración.
- **Idiomas**: inglés, español, portugués, francés, alemán, italiano, ruso, chino, japonés, hindi y árabe. Selector en Ajustes y en el recorrido inicial; se aplica con `Locales.wrap` en cada Activity/Service.
- **Recorrido inicial** del búho (una vez; se repite desde Ajustes).
- **Diseño**: tipografía Outfit (OFL), texto claro sobre fondos oscuros, paneles con iconos.

## Fase 5
- **Sueño** (pestaña nueva 🌙) con **detección automática**: nadie avisa cuándo se duerme, así que la app lo deduce mirando cuándo la pantalla del teléfono estuvo apagada varias horas seguidas de noche. Usa el "Acceso de uso" de Android (se da una vez desde Ajustes, con el botón "Abrir ajustes"); no hay ningún servicio corriendo todo el día. Requiere Android 9 o más nuevo.
  - **Se revisa** al abrir la app y cada mañana a la hora que elijas, y **te avisa con una notificación** ("Registré tu sueño: 23:40 → 07:15 · 7h 35min"). Las noches detectadas llevan 🤖; tocalas para corregir horarios o valorar la noche (al guardar dejan de ser automáticas).
  - **Ajustable** (botón "Ajustar"): sueño mínimo para contarlo (3/4/5 h), cuánto tardás en dormirte después de apagar la pantalla (0/10/20/30 min, se suma a la hora de dormir), desde qué hora puede empezar el sueño (18/20/22 h) y a qué hora de la mañana se revisa. También hay "Detectar ahora" y un interruptor para apagarla.
  - Se tolera mirar el teléfono unos minutos a la madrugada (encendidos de hasta 15 min no cortan la noche). La primera vez mira la última semana. Lo que borres no vuelve a aparecer.
  - Podés seguir **cargando una noche a mano** o editando/borrando cualquier registro.
- **Métricas** guardadas en la base de datos: promedio de horas, hora promedio de dormir y de despertar, noches registradas y valoración promedio, por **mes** (con gráfico de barras por día y línea de meta de 8 h), por **año** (promedio de cada mes) y **totales** (sueño total, noche más larga y más corta, promedio por año).
- Una noche se cuenta en el día en que te despertaste; las siestas cuentan como un registro más (suman al promedio).
- **Importar desde el calendario** (Ajustes): elegís el rango (7 días, 14, 30, 60, 90, 180 o 1 año; se recuerda) y los eventos que quieras, y se crean alarmas de una vez, "al empezar" o 10/15/30/60 min antes, con el título como etiqueta y la categoría que elijas. Pide el permiso de calendario recién al tocar el botón; solo lee.
- La copia de seguridad ahora incluye también el sueño. Base de datos v6 con migración.

## Fase 4
- **Categorías propias**: creá las tuyas (nombre, emoji y color) desde el editor ("＋ Nueva categoría") o en **Ajustes**. Se usan igual que las de fábrica (tarjetas, filtros, plantillas). Al borrar una, sus alarmas vuelven a General.
- **Historial** (pestaña nueva): cada vez que sonó una alarma queda registrada como Detenida, Pospuesta o Sin atender, con resumen de la semana. Se guardan 90 días.
- **Mantener para detener**: opción por alarma; el botón Detener hay que mantenerlo presionado ~2 s (en la notificación, el botón pasa a "Abrir").
- **Secuencias**: opción por alarma con 1, 2, 3 o 5 timbres extra cada 5/10/15 min. En la pantalla de alarma ves cuántos faltan y podés "Detener toda la secuencia". Posponer cancela lo que quedaba de la secuencia.
- **Leer la etiqueta en voz alta** (TTS del teléfono): dice la etiqueta al sonar y la repite cada 20 s.
- **Copia de seguridad** (Ajustes): exporta alarmas, plantillas, categorías, cronómetros y tus audios a un .zip; restaurar **reemplaza** lo que hay en la app (pide confirmación).
- Base de datos v4 con migración (se conservan tus datos). 

## Fase 3
- **Duración**: con "Repetir el sonido" apagado el audio suena una vez y la pantalla de alarma sigue el tiempo configurado (ej. 10 min).
- **Gato más tierno**: ojos redondos y brillantes que se abren despacito, cachetes, cabecita inclinada y ojitos felices (^ ^) mientras ronronea.
- **Notificación con personalidad**: un búho animado (aletea, mira de lado y parpadea) con frases, ícono de búho en la barra de estado y botones Posponer/Detener. Si no la atendés, aparece un búho dormido.
- **Cronómetros**: nueva pestaña con varios cronómetros guardados, vueltas, y un gatito que se asoma sobre un cronómetro: sigue la aguja con los ojos cuando algo corre y se duerme si todo está en pausa. Siguen contando aunque cierres la app.
- La base de datos pasó a la versión 3 con **migración real** (tus alarmas y plantillas se conservan).

## Cómo funciona (para que lo puedas seguir)
```
AlarmScheduler  ──setAlarmClock──▶ Android
      │ (hora exacta; permiso USE_EXACT_ALARM)
      ▼
AlarmReceiver ──▶ AlarmService (primer plano: audio + vibración + timeout)
                       │ notificación con fullScreenIntent
                       ▼
                  AlarmActivity (showWhenLocked + turnScreenOn)
```
- `data/` Room (`Alarm`, `Template`, `Category`, `CustomCategory`, `HistoryEntry`, `Stopwatch`, `SleepRecord`) y `AudioStorage` (copias de audios).
- `engine/` copia de seguridad (`Backup`), programación, servicio, receivers, notificaciones, sonidos y `VoiceRecorder`.
- `ui/` pantallas Compose; `ui/mascots/` búho, gato, gato cronómetro y cielo dibujados con Canvas (sin imágenes). `MascotBitmaps.kt` dibuja el búho en un Bitmap para las notificaciones.
- Sin Hilt: `BuhitoApp` crea un único `AlarmRepository` y lo reparte a mano.
- `Alarm.hour/minute` es la hora "de reloj" (no cambia al posponer); `Alarm.triggerAt` es la próxima vez que suena.

## Importante al actualizar
Desde la fase 2 la actualización conserva todo (migración 2→3). Si venís de la fase 1 la base se recrea. Si al abrir la app falla por la base de datos, desinstalala y volvé a instalar.

## Para que suene con el celular bloqueado
La app muestra tarjetas amarillas en la pantalla principal si falta algo:
1. **Notificaciones** permitidas.
2. **Avisos a pantalla completa** permitidos (Android 14+).
3. **Batería sin restricciones** (recomendado; algunos fabricantes tienen además su propio "ahorro" o "inicio automático").
4. En Android 12/12L, **Alarmas y recordatorios**.

## Checklist de pruebas de la fase 5
- [ ] Sueño: dar el Acceso de uso, dejar el teléfono con la pantalla apagada una noche y a la mañana tocar "Detectar ahora" (o esperar el aviso): aparece la noche con 🤖 y llega la notificación.
- [ ] Cambiar "Tardo en dormirme" y "Sueño mínimo" en Ajustar y verificar que se respetan en la próxima detección.
- [ ] Cargar a mano 3 o 4 noches distintas (incluida una pasando la medianoche, ej. 23:30 → 07:00) y mirar Mes, Año y Total.
- [ ] Editar y borrar una noche tocándola.
- [ ] Editar una noche 🤖: al guardar pierde la marca.
- [ ] Ajustes → Elegir eventos: pide permiso, lista eventos, crea las alarmas elegidas (aparecen en la pestaña Alarmas).
- [ ] Exportar y restaurar una copia: vuelven las noches registradas.
- [ ] Actualizar desde la fase 4 sin perder nada.

## Checklist de pruebas de la fase 4
- [ ] Crear una categoría propia desde el editor y usarla; verla en la tarjeta, el filtro y la pantalla de alarma.
- [ ] Borrar esa categoría en Ajustes: la alarma vuelve a General.
- [ ] Dejar sonar, detener y posponer alarmas: aparecen en Historial con el resultado correcto.
- [ ] Alarma con "Mantener para detener": un toque no la apaga, mantener 2 s sí.
- [ ] Secuencia +2 cada 5 min: suena 3 veces; "Detener toda la secuencia" corta las que faltan.
- [ ] "Leer la etiqueta": se escucha la voz con la alarma.
- [ ] Exportar, desinstalar/borrar datos, restaurar: vuelven alarmas, categorías y audios propios.
- [ ] Actualizar desde la fase 3 sin perder alarmas.

## Checklist de pruebas de la fase 3
- [ ] Alarma con "Repetir el sonido" apagado y duración 2 min: suena una vez y la pantalla sigue ~2 min.
- [ ] Con el celular en uso: la notificación muestra el búho moviéndose y una frase.
- [ ] Dejar pasar una alarma: llega la notificación con el búho dormido.
- [ ] Tocar al gato: abre los ojos despacio, sonríe (^ ^) y se duerme.
- [ ] Cronómetros: crear dos, iniciar uno, vuelta, pausar, cerrar la app y volver (sigue contando).
- [ ] El gato del cronómetro sigue la aguja y se duerme cuando todo está en pausa.

## Checklist de pruebas de la fase 2
- [ ] Alarma de lunes a viernes: suena y queda programada para el próximo día hábil.
- [ ] Alarma con días sueltos: el resumen de la tarjeta muestra los días elegidos.
- [ ] Grabar un mensaje de 5 s, previsualizarlo, "Usar este", guardar y esperar la alarma.
- [ ] Con "Repetir el sonido" apagado: suena el mensaje una vez y la pantalla sigue hasta cumplirse la duración.
- [ ] Volumen al 30 %: la previsualización y la alarma suenan más bajo.
- [ ] Guardar una plantilla y usarla en una alarma nueva (con audio grabado: borrar la alarma y verificar que la plantilla conserva su audio).
- [ ] Filtros: pausar una alarma y ver "Pausadas"; filtrar por categoría.
- [ ] Tocar al búho y al gato; mirar al búho dormido sin alarmas activas.
- [ ] Rechazar el permiso del micrófono: aparece el aviso y la app no se cierra.

## Límites conocidos
- Los cronómetros son progresivos (cuentan hacia arriba) y no muestran notificación propia todavía.
- Si el teléfono está apagado a la hora de una alarma de una sola vez, queda desactivada al encenderlo (las repetidas pasan al próximo día).
- Posponer una alarma repetida la reprograma dentro de N min; después vuelve a su horario normal.
- No hay `gradlew` ejecutable; Android Studio usa `gradle/wrapper/gradle-wrapper.properties`.
