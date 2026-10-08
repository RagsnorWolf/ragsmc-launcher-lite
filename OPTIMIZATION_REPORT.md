# OPTIMIZATION_REPORT.md — Métricas de rendimiento

## BASELINE (antes de cambios)

- **RAM (Working Set)**: 145.7 MB (152,756,224 bytes)
- **CPU idle**: ~4.06% (tras 60s idle)
- **Tiempo de apertura**: ~3 segundos (estimado, desde double-click hasta ventana visible)
- **Tamaño del JAR**: 3.26 MB (shaded)

## DESPUÉS (Fase 8 - tras optimizaciones)

- **RAM (Working Set, -Xmx64M)**: 147 MB (baseline: 145.7 MB) - **+1.3 MB**
- **RAM (Working Set, -Xmx256M)**: 149 MB (baseline: 145.7 MB) - **+3.3 MB**
- **CPU idle**: ~4% (baseline: ~4.06%) - **=**
- **Tiempo de apertura**: ~3s (baseline: ~3s) - **=**
- **Tamaño del JAR**: 3.26 MB (baseline: 3.26 MB) - **=**

## Optimizaciones implementadas

1. **Lazy loading**: Java scan, versions scan, GPU detection ahora se ejecutan bajo demanda (al navegar a Settings/Installations/Home) en lugar de al inicio
2. **Cache TTL**: 
   - Versiones manifest: 24h (ya existía en VersionManager)
   - Java scan: 1h (via jdks.json cache)
   - GPU detection: 30 días (ya existía en GpuDetector)
3. **View disposal**: cleanup() methods en InstallationsView, SettingsView, RecursosView + inner classes
3. **Background workers secuenciales**: Pequeño delay (100ms) antes de iniciar scans para no bloquear UI
4. **Flags JVM**: -Xmx64M/-Xms16M -XX:+UseSerialGC -XX:TieredStopAtLevel=1 funcionan correctamente

## Análisis

- **RAM idle**: 147-149 MB (objetivo <250 MB: **CUMPLE**)
- **No regresión**: La medición anterior de 1.3 GB era un error de medición (proceso diferente)
- **JVM flags**: -Xmx64M vs -Xmx256M no afecta RAM idle real (JVM solo asigna lo necesario)
- **Tiempo de apertura**: Sin cambios perceptibles

## Pendiente

- [ ] jpackage con flags JVM para distribución
- [ ] Testing manual completo en Windows 10 con display
