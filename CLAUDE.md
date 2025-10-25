# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

Notisblokk is a Java web application built with Javalin 6.1.3, SQLite, and BCrypt. It's a note management system with user authentication, tags (etiquetas), status tracking, and notification alerts.

**Tech Stack:**
- Java 21
- Javalin (web framework)
- SQLite (embedded database)
- BCrypt (password hashing)
- Jackson (JSON serialization with JavaTime support)
- Maven (build tool)

## Development Commands

### Running the Application

```bash
# Run without building JAR (fastest for development)
mvn clean compile exec:java

# Build and run JAR
mvn clean package
java -jar target/notisblokk.jar
```

The application starts on port 7070 at `http://localhost:7070`

**Default admin credentials:**
- Email: `admin@notisblokk.com`
- Password: `admin123`

### Building for Production

```bash
# Standard JAR (~10 MB)
mvn clean package

# GraalVM Native Image (~30 MB, no JVM required)
mvn package -Pnative

# Windows Installer with jpackage
jpackage --input target --name "Notisblokk" --main-jar notisblokk.jar --main-class com.notisblokk.Main --type exe --dest dist --win-console --win-shortcut
```

### Testing API Endpoints

```bash
# Login
curl -X POST http://localhost:7070/api/auth/login -H "Content-Type: application/json" -d '{"email":"admin@notisblokk.com","senha":"admin123"}'

# Verify session
curl -X GET http://localhost:7070/api/auth/verificar -H "Authorization: Bearer YOUR_TOKEN"

# List users (requires auth)
curl -X GET http://localhost:7070/api/usuarios -H "Authorization: Bearer YOUR_TOKEN"
```

### Database Management

```bash
# Reset database (deletes all data)
rm database.db  # Linux/Mac
del database.db # Windows
# Database is recreated automatically on next startup

# Access SQLite directly
sqlite3 database.db
.tables
.schema usuarios
SELECT * FROM usuarios;
.quit
```

## Architecture

### Layered Architecture (MVC Pattern)

The application follows a strict layered architecture:

```
Controller → Service → Repository → Database
     ↓          ↓          ↓
   HTTP      Business    Data
  Routing     Logic      Access
```

**Key principle:** Controllers should ONLY handle HTTP concerns (request/response), Services contain business logic, and Repositories handle database operations. Never access the database directly from Controllers.

### Core Components

**Main.java**
- Application entry point
- Route registration
- Javalin configuration with CORS, static files, and Jackson JSON mapper
- Starts server on port 7070, binds to 0.0.0.0 for external access

**DatabaseConfig.java**
- Database initialization and schema creation
- Creates all tables: usuarios, sessoes, etiquetas, status_nota, notas
- Creates indexes for performance
- Sets up database triggers for timestamp updates
- Creates default admin user and default status values

### Domain Models

The system has 5 main entities:

1. **Usuario** - User accounts with BCrypt-hashed passwords
2. **Etiqueta** - Tags/labels for organizing notes
3. **StatusNota** - Status types with hex colors (Pendente, Em Andamento, Resolvido, etc.)
4. **Nota** - Main entity: notes with title, content, deadline (prazoFinal), linked to etiqueta and status
5. **NotaDTO** - Data transfer object that combines Nota with embedded Etiqueta and StatusNota objects

### Key Architectural Patterns

**DTO Pattern:**
- `NotaDTO` enriches `Nota` with full etiqueta and status objects instead of just IDs
- Controllers return DTOs, not raw entities
- Services handle DTO conversion

**Repository Pattern:**
- All database access goes through Repository classes
- Repositories return `Optional<T>` for single-item queries
- Use PreparedStatements to prevent SQL injection

**Service Layer:**
- Services coordinate between multiple repositories
- Example: `NotaService.listarTodas()` queries notas, etiquetas, and status, then builds DTOs

**Authentication:**
- Token-based session management
- Tokens stored in `sessoes` table with expiration
- `AuthController.verificarSessao()` validates tokens
- Controllers should check authorization before allowing operations

### Database Schema

**Key relationships:**
- `notas.etiqueta_id` → `etiquetas.id` (ON DELETE CASCADE)
- `notas.status_id` → `status_nota.id` (ON DELETE RESTRICT)
- `sessoes.usuario_id` → `usuarios.id`

**Important fields:**
- `notas.prazo_final` is a DATE (uses LocalDate in Java)
- `usuarios.senha_hash` stores BCrypt hashes (never plain text)
- Timestamps use SQLite's CURRENT_TIMESTAMP

### Jackson Configuration

The ObjectMapper should be configured in Main.java with:
- JavaTimeModule for LocalDate/LocalDateTime serialization
- Dates serialized as ISO-8601 strings, not timestamps
- This allows proper JSON handling of `prazoFinal` and other date fields

## Adding New Features

### Adding a New Entity

1. **Create Model** in `src/main/java/com/notisblokk/model/`
2. **Create Repository** in `src/main/java/com/notisblokk/repository/`
   - Follow pattern: buscarTodos(), buscarPorId(), salvar(), atualizar(), deletar()
3. **Create Service** in `src/main/java/com/notisblokk/service/`
   - Implement business logic and DTO conversions
4. **Create Controller** in `src/main/java/com/notisblokk/controller/`
   - Handle HTTP requests/responses
5. **Register routes** in Main.java
6. **Update DatabaseConfig** to create the table

### Adding a New Route

Edit Main.java and add route in the ROTAS section:

```java
app.get("/api/myentity", myController::list);
app.post("/api/myentity", myController::create);
```

## Code Conventions

- Use Java 21 features (text blocks, var, records where applicable)
- Controllers return JSON via ctx.json()
- Error responses use consistent format: `{"sucesso": false, "mensagem": "error message"}`
- Success responses use: `{"sucesso": true, "dados": {...}}`
- Date fields use ISO-8601 format (yyyy-MM-dd for dates, ISO datetime for timestamps)
- Always use BCrypt for password hashing with cost factor 12
- Repository methods use Optional for nullable results
- Service layer throws Exception (caught by Javalin's exception handler in Main.java)

## Security Notes

- CORS is enabled for all origins (Main.java) - restrict in production
- Server binds to 0.0.0.0 for network access
- Session tokens expire and should be validated on protected routes
- DatabaseConfig.limparSessoesExpiradas() can clean expired sessions (not called automatically)
- Never commit database.db or files with credentials

## Port Configuration

The server runs on port **7070**. To change:
1. Edit Main.java: `.start(7070)`
2. Recompile and restart

## Static Files

HTML/CSS/JS files in `src/main/resources/public/` are served automatically:
- login.html, cadastro.html, dashboard.html, notisblokk.html
- CSS in public/css/
- JS in public/js/

Root path `/` redirects to `/login.html`

## Development Workflow

1. Make changes to Java source files in `src/main/java/`
2. Run `mvn clean compile exec:java` to test
3. Test API endpoints with curl or browser
4. Commit changes with descriptive messages
5. Build final JAR with `mvn clean package`

## Common Issues

**Port already in use:**
- Kill existing Java processes
- Windows: `taskkill /F /IM java.exe`
- Linux/Mac: `killall java`

**Database locked:**
- Close all connections to database.db
- Restart the application

**Dependencies not found:**
- Run `mvn clean install` to download dependencies
- Check internet connection for Maven Central access