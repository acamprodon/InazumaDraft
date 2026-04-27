# ⚡ Inazuma Draft

![Android](https://img.shields.io/badge/platform-Android-green)
![Kotlin](https://img.shields.io/badge/language-Kotlin-purple)
![Min SDK](https://img.shields.io/badge/minSdk-21-blue)
![Status](https://img.shields.io/badge/status-Completed-brightgreen)
![License](https://img.shields.io/badge/license-Educational-lightgrey)

Aplicación Android inspirada en *Inazuma Eleven* que implementa un sistema de **draft interactivo de equipos**, combinando selección estratégica, aleatoriedad y gestión avanzada de plantilla.

---

## 📱 Características

### 🎯 Sistema de Draft

* Selección de formación aleatoria
* Elección de capitán
* Picks de jugadores por posición
* 4 opciones aleatorias por turno
* Validación automática por rol

### 👥 Gestión de equipo

* 11 titular en campo
* Posiciones: PT, DF, MC, DL
* Soporte de posiciones secundarias

### 🔁 Banquillo dinámico

* Hasta 5 suplentes
* Intercambio con el campo mediante **drag & drop**
* Sincronización en tiempo real

### 👁️ Interacción avanzada

* Previsualización con pulsación larga
* Render dinámico del campo según formación

### 📊 Vista final

* Alternancia entre:

  * Vista campo
  * Estadísticas de jugadores

---

## 🧠 Arquitectura

```
ui/
 ├── MainActivity
 ├── DraftActivity
 ├── FinalTeamActivity
 ├── adapters/
 │    ├── OptionAdapter
 │    ├── TeamAdapter
 │    ├── FinalTeamAdapter
 │    └── BenchSelectedAdapter

model/
 └── Player

data/
 ├── PlayerRepository
 └── Formation
```

---

## 🔄 Flujo de la aplicación

```
Inicio
  ↓
Selección de temporadas
  ↓
Selección de formación
  ↓
Elección de capitán
  ↓
Draft del equipo
  ↓
Gestión del banquillo
  ↓
Pantalla final
```

---

## 🧪 Tecnologías

* **Kotlin**
* **Android SDK**
* **RecyclerView**
* **XML Layouts**
* **Drag & Drop API**
* **Parcelable**

---

## 📊 Modelo de datos

```kotlin
data class Player(
    val name: String,
    val nickname: String,
    val position: String,
    val element: Int,
    val kick: Int,
    val speed: Int,
    val control: Int,
    val defense: Int,
    val image: Int,
    val secondaryPositions: List<String>
)
```

---

## ⚙️ Instalación

```bash
git clone https://github.com/tuusuario/inazuma-draft.git
```

1. Abrir en **Android Studio**
2. Ejecutar en emulador o dispositivo físico

---

## 📈 Estado del proyecto

| Módulo       | Estado       |
| ------------ | ------------ |
| Draft        | ✅ Completado |
| Banquillo    | ✅ Completado |
| Drag & Drop  | ✅ Completado |
| UI Final     | ✅ Completado |
| Persistencia | ❌ Pendiente  |

---

## 🧠 Aprendizajes clave

* Gestión de estado complejo en interfaces dinámicas
* Implementación de drag & drop en Android
* Renderizado adaptativo de layouts
* Diseño de sistemas tipo draft

---

## 🔮 Mejoras futuras

* 💾 Guardado de equipos
* 🌐 Modo online
* 🤖 IA para picks
* 📊 Estadísticas avanzadas

---

## 👤 Autor

**camproo22**
Proyecto académico inspirado en *Inazuma Eleven*

---

## ⭐ Contribuir

Si te gusta el proyecto:

* Dale ⭐ al repositorio
* Haz fork y mejora la app
* Abre issues con ideas o bugs

---

## 📄 Licencia

Uso educativo y personal.
