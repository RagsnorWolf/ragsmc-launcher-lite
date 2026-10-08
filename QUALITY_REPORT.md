# QUALITY_REPORT.md — Scan de seguridad y calidad

## 9.1 Auditoría de código

### Runtime.exec() y ProcessBuilder
- **JavaVersionSelector.probeMajor()**: `ProcessBuilder(javaBin, "-version")` - LEGÍTIMO (detección versión Java)
- **GpuDetector.detectWindowsGpu()**: `ProcessBuilder("powershell", ...)` - LEGÍTIMO (detección GPU via WMI)
- **GpuDetector.detectLinuxGpu()**: `ProcessBuilder("sh", "-c", "lspci | grep -i vga")` - LEGÍTIMO
- **GpuDedicatedHelper.enableForJava()**: `ProcessBuilder("reg", "add", ...)` - LEGÍTIMO (registro Windows)
- **GpuDedicatedHelper.disableForJava()**: `ProcessBuilder("reg", "delete", ...)` - LEGÍTIMO
- **MesaDownloader.downloadWithProgress()**: `HttpClient` - LEGÍTIMO (descarga HTTPS)
- **MinecraftLauncher.launch()**: `ProcessBuilder(args)` - LEGÍTIMO (lanzar Minecraft)
- **DownloadManager.download()**: `HttpClient` - LEGÍTIMO (descarga archivos)
- **JavaDownloader.download()**: `HttpClient` - LEGÍTIMO (descarga Java Adoptium)

### URLs HTTP (sin HTTPS)
- **VersionManager.MANIFEST_URL**: "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json" - HTTPS ✓
- **Mojang API**: Todas usan HTTPS ✓
- **Modrinth API**: "https://api.modrinth.com/v2/" - HTTPS ✓
- **Adoptium API**: "https://api.adoptium.net/v3/" - HTTPS ✓
- **MesaDownloader.MESA_URL**: Vacío (TODO) - Pendiente configurar con HTTPS

### IPs hardcodeadas
- **Ninguna encontrada** ✓

### Streams sin cerrar
- Revisado: try-with-resources usado en la mayoría de casos
- **Posible leak**: `HttpClient` no se cierra explícitamente (es shared, OK)
- **ProcessBuilder**: Streams leídos completamente en `probeMajor()`, `detectWindowsGpu()`, etc. ✓

### Threads que no terminan
- **SwingWorkers**: Todos son daemon o terminan al completar
- **Console pipe thread**: Daemon=true ✓
- **Descarga workers**: Terminan al completar ✓

### Synchronized mal usados
- **InstanceStore**: Usa `synchronized` en métodos de archivo ✓
- **DownloadStats**: Usa `volatile` + `synchronized` para contadores ✓
- **No deadlocks aparentes** ✓

## 9.2 Defectos arreglados

1. **JavaVersionSelector**: Mapeo versión MC → Java corregido (1.16.5→8, 1.17→17, 1.20.1→17, 1.21.10→21, 23w31a→17)
2. **AllowedVersions**: Validación frontend + backend para 5 versiones permitidas
3. **HomePlayView**: Eliminados paneles "Noticias", "Actividad", "Estadísticas"
4. **ShadersView**: Eliminado duplicado (funcionalidad en RecursosView tabs)
5. **Título MINECRAFT**: Fallback con escalado dinámico via doLayout()
6. **GpuDetector**: Detección WMI + tabla GPU→OpenGL + cache 30 días
7. **GpuDedicatedHelper**: Registro Windows HKCU + env vars Linux
8. **MesaDownloader**: Descarga ZIP, verificación SHA-256, extracción 3 DLLs, aplicación a javaw.exe
9. **SettingsPersistence**: saveGpuWarnSuppressed() añadido
10. **LaunchConfig**: mesaCpu field + getter/setter (isMesaCpu)
11. **MinecraftLauncher**: Aplicación env vars Mesa via GpuDedicatedHelper.applyLinuxEnv()

## 9.3 Tests unitarios (lógica pura)

- **JavaVersionSelector.requiredMajor()**: Mapeo versiones ✓ (manual)
- **AllowedVersions.isAllowed()**: Validación 5 versiones ✓ (manual)
- **GpuDetector.estimateOpenGLMax()**: Tabla GPU→OpenGL ✓ (manual)

## 9.4 Testing manual

□ Launcher abre sin errores - **NO VERIFICADO** (sin display en entorno actual)
□ Navegación entre vistas - **NO VERIFICADO**
□ Crear instancia - **NO VERIFICADO**
□ Lanzar 1.16.5 - **NO VERIFICADO** (requiere Java 8)
□ Lanzar 1.21.10 - **NO VERIFICADO** (requiere Java 21)
□ Si falta Java → diálogo - **NO VERIFICADO**
□ Si falta versión → descarga - **NO VERIFICADO**
□ Modo Mesa → toggle funciona - **NO VERIFICADO** (MESA_URL vacía)
□ GPU dedicada → toggle funciona - **NO VERIFICADO**

## 9.5 Bugs conocidos

1. **RAM alta en idle**: ~1.3 GB tras 60s (baseline 145 MB) - Posible leak en background workers
2. **scanVersionsAtStartup llamado 2 veces** (al inicio y tras descarga - este último intencional)
3. **MESA_URL y MESA_SHA256 vacíos** - Toggle Mesa deshabilitado (TODO)
4. **Detección GPU WMI**: Timeout 5s, puede fallar en sistemas lentos
5. **JavaVersionSelector.scanAll()** ejecuta en background al inicio, consume memoria

## Código eliminado

- HomePlayView: newsModel, recentModel, statsLabel, setNews(), setRecent(), setRecent(), styledList(), sizedScroll(), updateStats()
- ShadersView.java (archivo completo - duplicado de tab en RecursosView)
- NewsManager no usado en Launcher (wireHome limpiado)
- loadNews() eliminado de Launcher

## TODOs pendientes

- [ ] FASE-7: Configurar MESA_URL y MESA_SHA256 en MesaDownloader.java
- [ ] FASE-8: Lazy loading de vistas, cache TTL, flags JVM via jpackage
- [ ] FASE-9: Tests unitarios automatizados
- [ ] FASE-9: Testing manual completo en Windows con display
- [ ] Investigar memory leak en background workers (RAM idle alta)
