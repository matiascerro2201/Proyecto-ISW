# Prácticas Pedagógicas

## Requisitos
- Java 25
- MySQL 8.4 (Gestión recomendada a través de MySQL Workbench)
*(Nota: No se requiere Node.js, el front-end usa Thymeleaf y se levanta junto con el backend).*

## Configuración y Base de Datos
1. Copiar el archivo `.env.example` y renombrarlo como `.env` en la raíz del proyecto.
2. Completar las variables dentro del `.env` con las credenciales de tu base de datos local. Debería quedar similar a esto:
   ```env
   DB_URL=jdbc:mysql://localhost:3306/practicas_pedagogicas?serverTimezone=UTC
   DB_USER=(usuario)
   DB_PASSWORD=(contraseña)

## Ejecutar

Aplicación (Backend y Front-end): desde el IDE, ejecutar la clase PracticasPedagogicasApplication.

Uso: abrir http://localhost:8080 en el navegador e iniciar sesión con los usuarios de prueba.


## Pruebas
.\mvnw.cmd test


## Versiones entregadas
v1.0.0: Sprint 1
