# QUALITY_REPORT.md — Scan de seguridad y calidad

## 9.1 Auditoría de código

### Runtime.exec() y ProcessBuilder
Todos los usos son legítimos:
- **JavaVersionSelector.probeMajor()**: `ProcessBuilder(javaBin, "-version")` - Detección versión Java
- **GpuDetector.detectWindowsGpu()**: `ProcessBuilder("powershell", ...)` - Detección GPU via WMI
- **GpuDetector.detectLinuxGpu()**: `ProcessBuilder("sh", "-c", "lspci | grep -i vga")` - Detección GPU Linux
- **GpuDedicatedHelper**: `ProcessBuilder("reg", "add/delete", ...)` - Registro Windows GPU
- **MesaDownloader**: `HttpClient` - Descarga HTTPS
- **MinecraftLauncher.launch()**: `ProcessBuilder(args)` - Lanzar Minecraft
- **MinecraftLauncher.pipeToConsole()**: `ProcessBuilder` pipe
- **JavaDownloader**: `HttpClient` - Descarga Java Adoptium
- **SystemInfo**: `ProcessBuilder` - Info sistema

### URLs HTTP (sin HTTPS)
**NINGUNA** - Todas las URLs usan HTTPS:
- Mojang: `https://launchermeta.mojang.com/`
- Modrinth: `https://api.modrinth.com/v2/`
- Adoptium: `https://api.adoptium.net/v3/`

### IPs hardcodeadas
**NINGUNA**

### Streams sin cerrar
- **Revisado**: try-with-resources usado en la mayoría de casos
- **HttpClient**: Compartido, no requiere cierre explícito
- **ProcessBuilder**: Streams leídos completamente
- **File I/O**: try-with-resources usado

### Threads que no terminan
- **Verificado**: Todos los threads son daemon (`setDaemon(true)`)
  - `LauncherLifecycle.pipe` thread: `pipe.setDaemon(true)` ✓
  - `MinecraftLauncher.pipeToConsole`: `t.setDaemon(true)` ✓
  - `Launcher.checker` thread: `checker.setDaemon(true)` ✓
- **SwingWorkers**: Terminan al completar ✓

### Synchronized mal usados
- **InstanceStore**: Usa `synchronized` en métodos de archivo ✓
- **DownloadStats**: Usa `volatile` + `synchronized` para contadores ✓
- **No deadlocks aparentes** ✓

## 9.2 Defectos arreglados

1. **JavaVersionSelector**: Mapeo versión MC → Java corregido (1.16.5→8, 1.17→17, 1.20.1→17, 1.21.10→21, 23w31a→17)
2. **AllowedVersions**: Validación frontend + backend para 5 versiones permitidas
3. **HomePlayView**: Eliminados paneles "Noticias", "Actividad", "Estadísticas"
4. **ShadersView**: Eliminado duplicado (funcionalidad en RecursosView tabs)
5. **Título MINECRAFT**: Fallback con escalado dinámico via `doLayout()` override
6. **GpuDetector**: Detección WMI + tabla GPU→OpenGL + cache 30 días
7. **GpuDedicatedHelper**: Registro Windows HKCU + env vars Linux
8. **MesaDownloader**: Descarga ZIP, verificación SHA-256, extracción 3 DLLs, aplicación a javaw.exe
9. **SettingsPersistence**: saveGpuWarnSuppressed() añadido
10. **LaunchConfig**: mesaCpu field + getter/setter (isMesaCpu)
11. **MinecraftLauncher**: Aplicación env vars Mesa via GpuDedicatedHelper.applyLinuxEnv()
12. **Lazy loading**: Java scan, versions scan, GPU detection bajo demanda
13. **View disposal**: cleanup() methods en InstallationsView, SettingsView, RecursosView + inner classes
14. **Cache TTL**: Versiones manifest 24h, Java 1h, GPU 30 días

## 9.3 Tests unitarios (lógica pura)

- **JavaVersionSelector.requiredMajor()**: Mapeo versiones ✓ (manual)
- **AllowedVersions.isAllowed()**: Validación 5 versiones ✓ (manual)
- **GpuDetector.estimateOpenGLMax()**: Tabla GPU→OpenGL ✓ (manual)
- **VersionManager.installedVersions()**: Escaneo versiones ✓ (manual)

## 9.4 Testing manual

□ Launcher abre sin errores - **NO VERIFICADO** (entorno sin display)
□ Navegación entre vistas - **NO VERIFICADO**
□ Crear instancia - **NO VERIFICADO**
□ Lanzar 1.16.5 - **NO VERIFICADO** (requiere Java 8)
□ Lanzar 1.21.10 - **NO VERIFICADO** (requiere Java 21)
□ Si falta Java → diálogo - **NO VERIFICADO**
□ Si falta versión → descarga - **NO VERIFICADO**
□ Modo Mesa → toggle funciona - **NO VERIFICADO** (MESA_URL vacía)
□ GPU dedicada → toggle funciona - **NO VERIFICADO**

## 9.5 Bugs conocidos

1. **MESA_URL y MESA_SHA256 vacíos** - Toggle Mesa deshabilitado (TODO)
2. **Testing manual**: **NO VERIFICADO** (entorno sin display)

## Código eliminado

- HomePlayView: newsModel, recentModel, statsLabel, setNews(), setRecent(), styledList(), sizedScroll(), updateStats()
- ShadersView.java (archivo completo - duplicado de tab en RecursosView)
- NewsManager no usado en Launcher (wireHome limpiado)
- loadNews() eliminado de Launcher
