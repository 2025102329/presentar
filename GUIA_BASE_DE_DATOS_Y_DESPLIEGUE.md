# Guía para guardar datos y usar el sistema desde cualquier lugar

## Idea general

Para abrir el sistema desde celulares o computadoras fuera de tu casa necesitas dos servicios en internet:

```text
Navegador
   ↓
Aplicación Java Spring Boot publicada
   ↓
Base de datos PostgreSQL o MySQL publicada
   ↓
Google Gemini API
```

No basta con subir `index.html`, porque la lógica, la API key y la base de datos viven en el backend Java.

---

## Opción recomendada: PostgreSQL administrado

### Paso 1. Crear la base

Crea una base PostgreSQL en el proveedor que prefieras. El proveedor debe entregarte:

- Host.
- Puerto.
- Nombre de la base.
- Usuario.
- Contraseña.
- Requisito de SSL.

Construye una URL JDBC:

```text
jdbc:postgresql://HOST:5432/NOMBRE_BASE?sslmode=require
```

### Paso 2. Configurar variables del servidor

Registra estas variables en el panel de tu hosting:

```text
SPRING_PROFILES_ACTIVE=cloud
DB_URL=jdbc:postgresql://HOST:5432/NOMBRE_BASE?sslmode=require
DB_USER=USUARIO
DB_PASSWORD=CONTRASEÑA
GEMINI_API_KEY=TU_API_KEY
GEMINI_MODEL=gemini-3.5-flash
```

Spring Boot creará las tablas automáticamente gracias a:

```properties
spring.jpa.hibernate.ddl-auto=update
```

### Paso 3. Publicar el backend

Sube el proyecto a un repositorio privado o entrega el ZIP al servicio de hosting.

Tienes dos caminos:

#### A. Despliegue con Docker

El repositorio ya incluye un `Dockerfile`. El servicio debe construir la imagen y exponer el puerto indicado por la variable `PORT`.

#### B. Despliegue con Maven

Comando de compilación:

```bash
mvn clean package -DskipTests
```

Comando de inicio:

```bash
java -jar target/fragancias-ia-1.0.0.jar
```

### Paso 4. Abrir la URL pública

El hosting entregará una dirección similar a:

```text
https://mi-fragancias.example.com
```

Desde esa misma dirección se carga el frontend y se consumen los endpoints `/api/...`.

---

## Alternativa: MySQL

Crea una base MySQL y registra:

```text
SPRING_PROFILES_ACTIVE=mysql
DB_URL=jdbc:mysql://HOST:3306/fragancias?useSSL=true&serverTimezone=UTC
DB_USER=USUARIO
DB_PASSWORD=CONTRASEÑA
GEMINI_API_KEY=TU_API_KEY
```

El archivo que controla ese perfil es:

```text
src/main/resources/application-mysql.properties
```

---

## Migrar los datos locales

La base local H2 sirve para desarrollo. Para una migración simple de un proyecto académico, normalmente es más limpio:

1. Crear la base nueva vacía.
2. Dejar que Spring Boot cree las tablas.
3. Volver a registrar clientes y pedidos de prueba.

Si ya tienes información real, exporta las tablas desde H2 y adapta los comandos SQL antes de importarlos a PostgreSQL o MySQL. Revisa especialmente los campos `CLOB`, fechas y valores de enums.

---

## Qué no debes hacer

- No conectar MySQL directamente desde JavaScript.
- No colocar la API key en el navegador.
- No subir `.env` a GitHub.
- No publicar la contraseña de la base en `application.properties`.
- No dejar una aplicación comercial sin inicio de sesión.

---

## Copias de seguridad

En una base administrada activa copias automáticas. Como mínimo conserva:

- Una copia diaria de la base.
- Una copia antes de actualizar el proyecto.
- Una exportación manual de clientes y pedidos importantes.

## Recomendación para la exposición

Ejecuta primero la versión local con H2. Cuando todo funcione, cambia al perfil `cloud` y publica la misma aplicación. No necesitas reescribir el frontend ni la lógica del árbol o del grafo.
