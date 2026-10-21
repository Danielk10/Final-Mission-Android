# Final Mission Android (NES Adaptation) 🚀👾

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API Level](https://img.shields.io/badge/API-24%20--%2037-orange.svg)](https://developer.android.com/about/versions/15)
[![Language](https://img.shields.io/badge/Language-Java_11%2F21-red.svg)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A.svg)](https://gradle.org)

Port y adaptación nativa a **Android en Java puro** del legendario juego de NES **Final Mission** (*S.C.A.T.: Special Cybernetic Attack Team* / *Action in New York*), desarrollado por Natsume (1990).

Este proyecto combina la ingeniería inversa de la ROM binaria original (`.nes`) con una arquitectura moderna de alto rendimiento en Android, integrando un motor 2D dedicado sobre `SurfaceView` y un sistema de control multitáctil sin botones en pantalla inspirado en **IA Rebellion**.

---

## 🕹️ Características Principales

- **Fidelidad Absoluta al Juego Original NES:**
  - Lógica, sprites, patrones de disparo, colisiones y jefes de fase adaptados 1:1 desde la ROM binaria NES (`final_mission.nes` - Mapper 1 SxROM, 128 KB PRG-ROM + 128 KB CHR-ROM).
  - Sistema de armas auténtico: Disparo Normal, Disparo Amplio (**Wide 'W'**), Rayo Láser (**Laser 'L'**), Acelerador de velocidad (**Speed 'S'**) y Recuperación de Energía (**1-Up / Life**).
  - Satélites orbitales de apoyo con fuego coordinado continuo.
  - Sin mecánicas ajenas (sin bombas ni agregados que rompan la esencia de la ROM original).
  
- **Controles Multitáctiles Innovadores (Basados en IA Rebellion):**
  - **Primer Dedo (Movimiento Relativo 1:1):** Arrastre diferencial (`deltaX = touchX - playerX`, `deltaY = touchY - playerY`). Permite pilotar la nave/soldado con agilidad milimétrica sin que el dedo tape en ningún momento el sprite del personaje.
  - **Traspaso Suave de Dedo:** Si el dedo de movimiento se levanta mientras otro dedo toca la pantalla, el movimiento se transfiere sin saltos bruscos.
  - **Segundo Dedo (Apuntado y Bloqueo 360° de Satélites):** Al mantener y arrastrar con el segundo dedo en cualquier dirección, los satélites giran y quedan bloqueados apuntando hacia ese ángulo (emulando la función táctica del botón A original de la NES).
  - **Giro Rápido (Flick Horizontal):** Deslizamiento rápido hacia la izquierda o derecha (< 0.40s) para cambiar la orientación del soldado.
  - **Disparo Continuo:** Fuego automático fluido mientras se interactúa con la pantalla.

- **Motor Gráfico y Renderizado Retro 2D:**
  - Basado en `SurfaceView` con hilo de renderizado dedicado a **60 FPS estables**.
  - Resolución virtual interna con escalado pixel-perfect y letterboxing automático para conservar la relación de aspecto original de la NES en pantallas modernas de cualquier resolución y aspect ratio (16:9, 19.5:9, 20:9).
  - Fondos parallax multicapa con scroll continuo y sincronizado.

- **Subsistema de Audio de Baja Latencia:**
  - `SoundPool` para efectos de sonido (SFX) simultáneos de respuesta inmediata (disparos, impactos láser, explosiones, alertas).
  - `MediaPlayer` para la música de fondo (BGM) en bucle continuo de cada etapa.

---

## 🏗️ Arquitectura y Configuración de Compilación

Basado en la plantilla oficial de desarrollo Android Java moderna con configuraciones de caché y SDK probadas en el proyecto `PIC-k150-Programing`:

| Propiedad | Valor |
| :--- | :--- |
| **Namespace / Application ID** | `com.diamon.finalmission` |
| **Compile SDK** | `37` (Android 15 / 16 Preview) |
| **Min SDK** | `24` (Android 7.0 Nougat+) |
| **Target SDK** | `37` |
| **Android Gradle Plugin (AGP)** | `9.2.1` / `9.4.1` |
| **Gradle** | `9.5.1` |
| **Compatibilidad Java** | Java 11 / 17 / 21 |
| **Caché y Salida Temporal** | Salida redirigida a `/tmp/finalmission_build` y `--project-cache-dir /tmp/gradle-cache-...` para proteger el almacenamiento del disco de trabajo. |

---

## 📁 Estructura del Proyecto

```text
Final-Mission-Android/
├── app/
│   ├── build.gradle                 # Configuración de compilación, SDK 37 y redirección a /tmp
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml  # Configuración inmersiva horizontal y aceleración por HW
│           ├── assets/
│           │   ├── audio/           # SFX (disparo, laser, explosion) y BGM originales
│           │   ├── data/            # Mapas de colisión, niveles y paletas PPU de la ROM
│           │   └── sprites/         # Sprites extraídos (Soldados, Satélites, Jefes, 8192 tiles CHR)
│           ├── java/com/diamon/finalmission/
│           │   ├── MainActivity.java    # Entrada Android, modo inmersivo sticky
│           │   ├── GameView.java        # Ciclo de juego 60 FPS y renderizado SurfaceView
│           │   ├── TouchController.java # Mecánicas táctiles 1:1, flick y satélites 360°
│           │   ├── AudioManager.java    # Gestión SoundPool y MediaPlayer
│           │   ├── Player.java          # Entidad jugador, jetpack, vida y poderes
│           │   ├── Satellite.java       # Satélites orbitales con fijación angular
│           │   ├── Weapons.java         # Proyectiles Normal, Wide, Laser y enemigos
│           │   ├── Enemies.java         # AntiAéreo, Volador, Saltador, Robot, Boss
│           │   └── World.java           # Scroll de niveles, parallax y colisiones
│           └── res/                     # Iconos y temas de la aplicación
├── gradle/                          # Wrapper y catálogos de dependencias
├── gradlew                          # Script ejecutable con caché en /tmp
├── local.properties                 # Configuración local del SDK
├── setup-sdk.sh                     # Script automatizado de aprovisionamiento del SDK
├── LICENSE                          # Licencia Apache 2.0
└── README.md                        # Documentación del proyecto
```

---

## 🚀 Compilación y Ejecución

### 1. Configurar SDK de Android
Ejecuta el script automatizado para enlazar las herramientas de compilación y el SDK:
```bash
bash setup-sdk.sh
```

### 2. Compilar APK Debug
Para compilar la aplicación utilizando el sistema de caché optimizado:
```bash
./gradlew assembleDebug
```
El APK resultante se generará en `/tmp/finalmission_build/outputs/apk/debug/app-debug.apk`.

### 3. Instalar y Probar vía ADB
Con el dispositivo conectado mediante cable USB o túnel ADB:
```bash
adb install -r /tmp/finalmission_build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.diamon.finalmission/.MainActivity
```

---

## 📄 Licencia

Este proyecto se distribuye bajo los términos de la **Licencia Apache 2.0**. Consulta el archivo [`LICENSE`](LICENSE) para más detalles.
