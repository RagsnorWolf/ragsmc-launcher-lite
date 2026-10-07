# NOTAS.md — Decisiones y TODOs

## FASE 0 - Baseline
- Git inicializado en C:\ragsmc-launcher-java
- Tag pre-lite-v2 creado
- Rama lite-v2 creada
- .gitignore configurado (target/, *.jar, *.log, Mesa DLLs, custom-jre/)
- Proyecto compila OK (mvn clean package -DskipTests)
- JAR shaded: 3.26 MB
- Baseline medido: 145.7 MB RAM, ~4% CPU, ~3s apertura

## TODOs globales
- [ ] FASE 1: Selección automática de Java por versión MC
- [ ] FASE 2: Limitar versiones a 5 (1.16.5, 1.17, 1.20.1, 1.21.10, 23w31a)
- [ ] FASE 3: Limpieza UI (borrar noticias, actividad, sidebar duplicados)
- [ ] FASE 4: Arreglar título MINECRAFT que se corta
- [ ] FASE 5: Detección GPU + OpenGL (WMI Windows, lspci Linux)
- [ ] FASE 6: Forzar GPU dedicada (registro Windows, env vars Linux)
- [ ] FASE 7: Modo Mesa (software rendering) - CRÍTICO
- [ ] FASE 8: Optimización con métricas antes/después
- [ ] FASE 9: Scan seguridad y calidad
- [ ] FASE 10: Reporte final

## Decisiones tomadas
- Java 11 para compilar el launcher, pero runtime MC usa Java 8/17/21 según versión
- Solo 5 versiones permitidas para simplificar testing
- Mesa3D: descargar ZIP propio (3 DLLs) a release GitHub, verificar SHA-256
- GPU detection: WMI en Windows, cacheado en config, no bloqueante
- Forzar GPU: HKCU\Software\Microsoft\DirectX\UserGpuPreferences en Windows
