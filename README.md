# PPU - Weather App (Versión Local - SQLite) 🌦️

Aplicación meteorológica interactiva para Android, desarrollada con un enfoque de arquitectura *Offline-First* y almacenamiento local.

## Características Principales

*   **Autenticación Segura Local:** Sistema de login y registro gestionado localmente, incluyendo validación de modo "Invitado" (con restricciones operativas).
*   **Seguridad:** Implementación de encriptación unidireccional (Hashing SHA-256) para las contraseñas de los usuarios.
*   **Mapa Interactivo (Google Maps SDK):** Geolocalización por GPS, búsqueda por texto (*Geocoder*) y colocación de pines personalizados.
*   **Clima en Tiempo Real:** Integración con la API de Open-Meteo mediante Retrofit para obtener temperatura, viento e índice UV.
*   **Gestión de Favoritos (CRUD Completo):** Base de datos relacional SQLite local con soporte para Crear, Leer, Actualizar (renombrar) y Eliminar ubicaciones favoritas.

##  Tecnologías y Arquitectura

*   **Lenguaje:** Kotlin
*   **Interfaz:** XML (Material Design, DrawerLayout, CardViews)
*   **Almacenamiento Local:** SQLite (Normalizado en 3NF) & SharedPreferences
*   **Consumo de API:** Retrofit + Gson Converter
*   **Servicios de Ubicación:** FusedLocationProviderClient

## Instrucciones de Uso

1.  **Clonar el repositorio:** `git clone [Enlace_del_Repositorio]`
2.  **Abrir en Android Studio:** Asegúrese de sincronizar el proyecto con los archivos Gradle.
3.  **Funcionamiento:**
    *   Ingrese como invitado para visualizar el mapa y el clima.
    *   Regístrese localmente para desbloquear la funcionalidad de guardar pines en el mapa y gestionar su lista de favoritos.