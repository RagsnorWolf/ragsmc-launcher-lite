# MOTOR_REPORT.md — Auditoría de motor y lenguaje

## RagsMC Lite (C:\ragsmc-launcher-java\)

- **Lenguaje**: Java 11 (compilado con target 11)
- **Framework UI**: Swing + FlatLaf 3.4 (Look & Feel moderno)
- **Motor gráfico**: LWJGL 3.3.3 (OpenGL bindings)
- **Motor de lanzamiento**: Custom Java (MinecraftLauncher, VersionManager, JavaVersionSelector, ClasspathBuilder, AuthlibPatcher)
- **Versiones de dependencias**:
  - LWJGL: 3.3.3 (lwjgl, lwjgl-opengl, lwjgl-glfw)
  - Gson: 2.11.0
  - FlatLaf: 3.4
- **Tamaño del JAR (shaded)**: 3.26 MB (3,420,266 bytes)
- **Líneas de código totales**: ~7,959 líneas en 43 archivos .java
- **Build system**: Maven 3.9.16
- **JRE target**: Java 11+ (compatible con Java 8 para runtime MC)

## RagsMC Modern (C:\ragsmc-launcher\) — SOLO LECTURA

- **Lenguaje**: TypeScript (React 19) + Rust (Tauri 2.6)
- **Framework UI**: React 19 + TailwindCSS + Framer Motion
- **Motor gráfico**: Three.js (via skinview3d) + Tauri WebView2
- **Motor de lanzamiento**: Rust (tauri backend) + TypeScript frontend
- **Tamaño del binario**: 19.5 MB (RagsMC-Launcher.exe)
- **Dependencias clave**:
  - Frontend: React 19, Three.js, skinview3d, Framer Motion, Tailwind
  - Backend: Tauri 2.6, reqwest, serde, zip, winreg, sevenz-rust, rsa, sha2
- **Build system**: Vite + Cargo (Rust)
- **Distribución**: NSIS installer (~11.5 MB)

---
