# OPTIMIZATION_REPORT.md — Métricas de rendimiento

## BASELINE (antes de cambios)

- **RAM (Working Set)**: 145.7 MB (152,756,224 bytes)
- **CPU idle**: ~4.06% (tras 60s idle)
- **Tiempo de apertura**: ~3 segundos (estimado, desde double-click hasta ventana visible)
- **Tamaño del JAR**: 3.26 MB (shaded)

## DESPUÉS (pendiente — Fase 8)

- **RAM (Working Set)**: ___ MB (baseline: 145.7 MB)
- **CPU idle**: ___ % (baseline: ~4.06%)
- **Tiempo de apertura**: ___ s (baseline: ~3 s)
- **Tamaño del JAR**: ___ MB (baseline: 3.26 MB)

## Notas

- La medición se realizó con el custom JRE (jlink) con módulos: java.base, java.desktop, java.net.http, java.logging, java.xml, java.naming, java.security.jgss, jdk.crypto.ec
- Flags JVM usados: -Xmx64M -Xms16M -XX:+UseSerialGC -XX:TieredStopAtLevel=1
- Hardware: Intel Celeron B830 + Intel HD Graphics (Sandy Bridge, OpenGL 3.1) en Windows 10
