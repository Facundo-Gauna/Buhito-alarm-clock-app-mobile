<div align="center">

# Buhito 🦉

**Una alarma con personalidad: tus propios sonidos, un búho de guardia y un gato dormilón.**

100 % local · sin cuenta · sin anuncios · sin rastreo

[Read in English](README.md)

</div>

## Qué hace

- ⏰ **Alarmas que suenan con el celular bloqueado**: hora exacta, pantalla completa, vibración y posponer 5/10/15 min.
- 🎙️ **Tu propio sonido**: tonos del sistema, cualquier audio del teléfono o tu voz grabada.
- 🔁 Repetir por días, volumen por alarma, sonido en bucle o una sola vez, **secuencias** de timbres y plantillas.
- 🏷️ **Categorías** de fábrica y propias (con emoji y color) y filtros.
- 🗣️ **Lee la etiqueta en voz alta**.
- 🛑 **Formas de detenerla**: botón normal, **mantener presionado** 2 s o **escanear tu código QR de Buhito** (para obligarte a levantarte). El **modo estricto** (Ajustes) quita posponer y cualquier otra salida en las alarmas con QR.
- 🌙 **Sueño**: detección automática según cuándo la pantalla estuvo apagada de noche, ajustable y con notificación; promedios mensuales, anuales y totales.
- ⏱️ **Cronómetros** con un gato curioso.
- 📅 **Importar eventos del calendario** como alarmas (hasta 1 año).
- 📜 **Historial** y 💾 **copia de seguridad** en un `.zip`.
- 🦉 **Recorrido de bienvenida** con el búho y animalitos animados.
- 🌍 **11 idiomas**: English, Español, Português, Français, Deutsch, Italiano, Русский, 中文, 日本語, हिन्दी, العربية.

## Instalar

1. Descargá `buhito-x.y.z.apk` desde [**Releases**](../../releases).
2. Abrilo en el celular y permití instalar desde esa fuente.
3. Para que suene con la pantalla bloqueada, permití **notificaciones**, **alarmas y recordatorios**, **avisos a pantalla completa** y poné la batería en **sin restricciones**. La app muestra tarjetas amarillas si falta algo.

Requiere Android 8.0 o más nuevo (la detección automática de sueño, Android 9+).

## Compilar

Con Android Studio (Ladybug o más nuevo, JDK 17): **Open** → carpeta del proyecto → esperá el sync → **Run**.
Por línea de comandos: `gradle assembleDebug` (con Gradle 8.9, o generá el wrapper una vez con `gradle wrapper --gradle-version 8.9`).

El workflow `.github/workflows/build.yml` compila el APK en cada push; al subir una etiqueta (`git tag v1.0.0 && git push origin v1.0.0`) crea además un Release con el APK adjunto.

## Privacidad

Todo queda en tu teléfono. Sin servidor, sin cuenta, sin analíticas. El micrófono, la cámara, el calendario y el acceso de uso solo se piden cuando activás la función que los necesita.

## Más información

Notas de desarrollo y listas de pruebas: [docs/DEVELOPMENT.es.md](docs/DEVELOPMENT.es.md). Código bajo licencia [MIT](LICENSE); fuente Outfit bajo SIL OFL ([licenses/Outfit-OFL.txt](licenses/Outfit-OFL.txt)).
