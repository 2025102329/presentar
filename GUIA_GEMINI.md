# Configuración completa de Google Gemini

## 1. Archivos principales

```text
src/main/java/com/delacruz/fragancias/
├── config/
│   └── GeminiProperties.java
├── service/
│   ├── GeminiClient.java
│   ├── AiDecisionService.java
│   └── DashboardService.java
├── entity/
│   └── OrigenDecision.java
└── FraganciasApplication.java

src/main/resources/
├── application.properties
└── static/
    ├── css/styles.css
    └── js/app.js
```

## 2. Archivo `.env`

Conserva tus datos de Neon y reemplaza únicamente la clave de Gemini:

```properties
GEMINI_API_KEY=TU_CLAVE_NUEVA_DE_GOOGLE_AI_STUDIO
GEMINI_MODEL=gemini-3.5-flash
GEMINI_ENABLED=true
PORT=8081

spring.profiles.active=cloud
DB_URL=jdbc:postgresql://TU_HOST/neondb?sslmode=require
DB_USER=neondb_owner
DB_PASSWORD=TU_CONTRASENA_DE_NEON
```

No uses comillas, no escribas `Bearer` y no publiques el archivo `.env`.

## 3. Flujo de la integración

1. El frontend solicita una operación al backend.
2. `AiDecisionService` prepara los datos y el JSON Schema.
3. `GeminiClient` llama a Google Gemini con `x-goog-api-key`.
4. Gemini devuelve JSON estructurado.
5. El sistema valida la decisión antes de guardarla.
6. Si Gemini falla, se utiliza el algoritmo local y se registra origen `LOCAL`.
7. Si Gemini responde correctamente, se registra origen `GEMINI`.

## 4. Ejecución

```powershell
cd "C:\Users\German\Downloads\DeLa_Cruz_FraganciasIA\DeLaCruzFraganciasIA"
mvn clean spring-boot:run
```

Abre:

```text
http://localhost:8081
```

## 5. Verificación

Registra dos clientes o genera una ruta. En **Decisiones IA**, el origen debe indicar:

```text
GEMINI
```

Si indica `LOCAL`, revisa en la terminal el mensaje `Gemini respondió con estado ...`.
