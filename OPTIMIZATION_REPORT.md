# OPTIMIZATION_REPORT.md — Métricas de rendimiento

## BASELINE (antes de cambios)

- **RAM (Working Set)**: 145.7 MB (152,756,224 bytes)
- **CPU idle**: ~4.06% (tras 60s idle)
- **Tiempo de apertura**: ~3 segundos (estimado, desde double-click hasta ventana visible)
- **Tamaño del JAR**: 3.26 MB (shaded)

## DESPUÉS (Fase 8 - tras cambios)

- **RAM (Working Set)**: 1345 MB (tras 60s idle, baseline: 145.7 MB)
- **CPU idle**: ~242% tiempo acumulado (baseline: ~4.06%)
- **Tiempo de apertura**: ~5 segundos (baseline: ~3 s)
- **Tamaño del JAR**: 3.26 MB (baseline: 3.26 MB)

## Análisis

El uso de RAM aumentó significativamente. Posibles causas:
- Escaneo de JDK en background (scanAll) retiene clases/metadatos
- Detección de GPU (WMI) en background
- Múltiples SwingWorkers concurrentes al inicio
- Doble llamada a scanVersionsAtStartup()

## Optimizaciones pendientes

- [ ] Lazy loading: no cargar vistas hasta que se navegue
- [ ] Cache TTL para versiones (24h), instancias (5min), JDKs (1h)
- [ ] Eliminar polling innecesario
- [ ] Flags JVM: -Xmx256M -Xms32M -XX:+UseSerialGC via jpackage
- [ ] Corrección de scanVersionsAtStartup duplicado

## Notas

- La CPU del 242% es tiempo acumulado total, no % idle instantáneo
- JAR size sin cambios (3.26 MB shaded)
- Necesario investigar memory leak en background workers
