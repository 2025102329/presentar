# De la Cruz Fragancias IA

Sistema web académico para administrar perfumes, clientes, pedidos y entregas utilizando:

- Java 17+ y Spring Boot.
- HTML, CSS y JavaScript sin frameworks externos.
- Base de datos local H2.
- Árbol binario de clientes decidido por IA.
- Cola FIFO para preparación de pedidos.
- Pila LIFO para historial.
- Grafo ponderado para rutas de entrega.
- Integración con Google Gemini Interactions API desde el backend.

## Inicio rápido en Windows

1. Abre la carpeta completa en Visual Studio Code.
2. Abre el archivo `.env`.
3. Reemplaza:

```properties
GEMINI_API_KEY=PEGA_AQUI_TU_API_KEY
```

por una clave nueva creada en Google AI Studio.
4. Guarda el archivo.
5. Ejecuta `run-windows.bat` o usa:

```powershell
mvn spring-boot:run
```

6. Abre `http://localhost:8081`.

## Cómo confirmar que la IA funciona

1. Registra un primer cliente para crear la raíz.
2. Registra un segundo cliente para provocar una comparación.
3. Abre el módulo **Decisiones IA**.
4. El origen debe aparecer como `GEMINI`.

Si aparece `LOCAL`, la aplicación utilizó el algoritmo de respaldo porque la API no respondió, la clave no es válida, el modelo no está disponible o existe un problema de cuota/conectividad.

## Árbol mejorado

El árbol se dibuja con SVG y conserva correctamente:

- rama izquierda para valores menores;
- rama derecha para valores mayores;
- enlaces y etiquetas visuales;
- ajuste automático, acercamiento y alejamiento;
- detalles del nodo en una ventana modal separada.

Los detalles ya no reducen el espacio del árbol. Se muestran únicamente al seleccionar **Ver detalles** en un nodo.

## Estructura principal

```text
src/main/java/com/delacruz/fragancias
├── config
├── controller
├── dto
├── entity
├── exception
├── repository
├── service
└── util

src/main/resources
├── static
│   ├── assets/logo-perfume.png
│   ├── css/styles.css
│   ├── js/app.js
│   └── index.html
├── application.properties
├── application-cloud.properties
└── application-mysql.properties
```

## Base de datos local

Los datos se guardan en:

```text
data/fragancias.mv.db
```

La consola de H2 está disponible en:

```text
http://localhost:8081/h2-console
```

Datos de conexión:

```text
JDBC URL: jdbc:h2:file:./data/fragancias
Usuario: sa
Contraseña: vacía
```

## Archivos que normalmente modificarás

- `src/main/resources/static/index.html`: estructura visual.
- `src/main/resources/static/css/styles.css`: colores y estilos.
- `src/main/resources/static/js/app.js`: comportamiento de la interfaz.
- `.env`: API key y puerto local.
- `application-cloud.properties`: base PostgreSQL para publicación.
- `application-mysql.properties`: base MySQL.

## Seguridad

La API key nunca debe colocarse en `index.html`, `app.js` ni en un repositorio público. El archivo `.env` está excluido por `.gitignore`.
