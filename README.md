# Guía de Instalación y Despliegue

<div align="center">

[![Stars](https://img.shields.io/github/stars/Arktcode/radark-core?label=PLEASE%20STAR%20RADARK!&style=for-the-badge&color=7289DA)](https://github.com/Arktcode/radark-core/stargazers)
[![Latest Version](https://img.shields.io/github/v/release/Arktcode/radark-core?label=LATEST%20VERSION&style=for-the-badge&logo=github&color=43B581)](https://github.com/Arktcode/radark-core/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Arktcode/radark-core/total?label=&style=for-the-badge&color=7289DA)](https://github.com/Arktcode/radark-core/releases)
<img width="1024" height="339" alt="radark" src="https://github.com/user-attachments/assets/474bb41f-aae7-4f47-942f-8e02daf596eb" />
</div>

Esta guía explica cómo configurar y ejecutar el bot en diferentes entornos.

## Prerrequisitos Generales
- **Java JDK 17** (mínimo requerido).
- Archivos compilados del bot (`server.jar`).
- Carpeta `assets` con los recursos del juego.
- Archivo `.env` con el token.

---

## 🖥️ Servidor Dedicado / VPS (Consola)

### 1. Instalación
1. Sube los archivos del bot al servidor.
2. Asegúrate de tener Java 17 instalado: `java -version`.
3. Si usas Linux/Mac, da permisos a Gradle: `chmod +x gradlew`.

### 2. Configuración (.env)
Crea un archivo `.env` junto al jar:
```
DISCORD_TOKEN=tu_token
```

### 3. Descarga de Assets
Ejecuta el script `download_assets.ps1` (Windows) o descarga manualmente `sprites.aatls`, `sprites.png` y `block_colors.png` en `assets/sprites`.

### 4. Ejecución
**Modo Desarrollo:** `./gradlew run` (Linux) o `run.bat` (Windows).
**Modo Producción:**
1. Compila: `./gradlew shadowJar`
2. Ejecuta: `java -jar build/libs/server.jar`

---

## 🦖 Pterodactyl Panel / Hosting Compartido

Muchos hostings de juegos usan Pterodactyl. Sigue estos pasos:

### 1. Crear el Servidor
- **Nest/Egg:** Selecciona "Java" o "Generic Java".
- **Docker Image:** Asegúrate de elegir **Java 17** (o 18+). Si usas Java 8 o 11, **el bot no funcionará**.
- **Startup Command:**
  ```bash
  java -jar server.jar
  ```

### 2. Subir Archivos (File Manager)
1. Compila el bot en tu PC primero (`gradlew shadowJar`) para generar el archivo `server.jar` (se encuentra en `build/libs/`).
2. Sube el archivo `server.jar` a la raíz del servidor en el panel.
3. Sube la carpeta `assets` **completa** (con `sprites` dentro) a la raíz del servidor.
   - *Nota:* Es más fácil subir un ZIP con la carpeta `assets` y descomprimirlo en el panel.

### 3. Configurar Token
En el "File Manager", crea un archivo nuevo llamado `.env` y pega tu token:
```
DISCORD_TOKEN=tu_token_real
```

### 4. Iniciar
Ve a la consola y presiona **Start**.

### prueba

Una vez iniciado el bot intenta ejecutar `/schem` o `@(botname)`. Luego intenta adjunta un archivo. `msch` o un link `base64`, el resultado deberia verse tal que asi:
<details><img width="401" height="299" alt="image" src="https://github.com/user-attachments/assets/32118150-e0a3-407e-b6a9-09cd477a91ed" />
</details>

---

# Installation and Deployment Guide

This guide explains how to configure and run the bot in different environments.

## General Prerequisites
- **Java JDK 17** (minimum required).
- Compiled bot files (`server.jar`).
- `assets` folder with game resources.
- `.env` file with the token.

---

## 🖥️ Dedicated Server / VPS (Console)

### 1. Installation
1. Upload the bot files to the server.
2. Ensure Java 17 is installed: `java -version`.
3. If using Linux/Mac, grant permissions to Gradle: `chmod +x gradlew`.

### 2. Configuration (.env)
Create a `.env` file next to the jar:
```
DISCORD_TOKEN=your_token
```

### 3. Assets Download
Run the `download_assets.ps1` script (Windows) or manually download `sprites.aatls`, `sprites.png`, and `block_colors.png` into `assets/sprites`.

### 4. Execution
**Development Mode:** `./gradlew run` (Linux) or `run.bat` (Windows).
**Production Mode:**
1. Build: `./gradlew shadowJar`
2. Run: `java -jar build/libs/server.jar`

---

## 🦖 Pterodactyl Panel / Shared Hosting

Many game hostings use Pterodactyl. Follow these steps:

### 1. Create Server
- **Nest/Egg:** Select "Java" or "Generic Java".
- **Docker Image:** Make sure to choose **Java 17** (or 18+). If you use Java 8 or 11, **the bot will not work**.
- **Startup Command:**
  ```bash
  java -jar server.jar
  ```

### 2. Upload Files (File Manager)
1. Build the bot on your PC first (`gradlew shadowJar`) to generate the `server.jar` file (found in `build/libs/`).
2. Upload the `server.jar` file to the root of the server in the panel.
3. Upload the **complete** `assets` folder (containing `sprites`) to the root of the server.
   - *Note:* It is easier to upload a ZIP with the `assets` folder and unzip it in the panel.

### 3. Configure Token
In the "File Manager", create a new file named `.env` and paste your token:
```
DISCORD_TOKEN=your_real_token
```

### 4. Start
Go to the console and press **Start**.

###Testing

Once the bot has started, it tries to execute `/schem` or `@(botname)`. Then it tries to attach an `msch` file or a `base64` link; the result should look something like this:

<details><img width="401" height="299" alt="image" src="https://github.com/user-attachments/assets/32118150-e0a3-407e-b6a9-09cd477a91ed" />
</details>




