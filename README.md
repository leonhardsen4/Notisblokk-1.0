# Notisblokk 1.0

Sistema de gerenciamento de notas com etiquetas, status e notificações de alertas.

## Tecnologias

- **Java 21** - Linguagem de programação
- **Javalin 6.1.3** - Framework web minimalista
- **SQLite** - Banco de dados embarcado
- **BCrypt** - Hash seguro de senhas
- **Jackson** - Serialização JSON com suporte a JavaTime
- **Maven** - Gerenciamento de dependências e build

## Requisitos

- Java 21 ou superior
- Maven 3.6+ (ou usar o Maven wrapper incluído)

## Instalação

Clone o repositório:

```bash
git clone https://github.com/SEU_USUARIO/Notisblokk-1.0.git
cd Notisblokk-1.0
```

## Como Executar

### Desenvolvimento (sem build de JAR)

```bash
mvn clean compile exec:java
```

### Build e execução do JAR

```bash
mvn clean package
java -jar target/notisblokk.jar
```

A aplicação estará disponível em `http://localhost:7070`

## Credenciais Padrão

- Email: `admin@notisblokk.com`
- Senha: `admin123`

## Estrutura do Projeto

```
Notisblokk-1.0/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── notisblokk/
│   │   │           ├── Main.java
│   │   │           ├── config/
│   │   │           ├── controller/
│   │   │           ├── model/
│   │   │           ├── repository/
│   │   │           └── service/
│   │   └── resources/
│   │       └── public/
│   │           ├── css/
│   │           └── js/
│   └── test/
│       └── java/
├── pom.xml
├── README.md
├── CLAUDE.md
├── LICENSE
└── .gitignore
```

## Funcionalidades

- Autenticação de usuários com sessões
- Gerenciamento de notas
- Sistema de etiquetas (tags)
- Status personalizáveis com cores
- Notificações e alertas
- Interface web responsiva

## Comandos Maven Úteis

```bash
# Compilar
mvn clean compile

# Executar testes
mvn test

# Criar JAR executável
mvn clean package

# Limpar build
mvn clean

# Executar sem JAR
mvn clean compile exec:java
```

## Build Avançado

### GraalVM Native Image

Cria um executável nativo (~30 MB, sem JVM):

```bash
mvn package -Pnative
```

### Windows Installer

Cria instalador .exe usando jpackage:

```bash
jpackage --input target --name "Notisblokk" --main-jar notisblokk.jar --main-class com.notisblokk.Main --type exe --dest dist --win-console --win-shortcut
```

## API Endpoints

### Autenticação

- `POST /api/auth/login` - Login de usuário
- `GET /api/auth/verificar` - Verificar sessão
- `POST /api/auth/logout` - Logout

### Usuários

- `GET /api/usuarios` - Listar usuários
- `POST /api/usuarios` - Criar usuário
- `GET /api/usuarios/:id` - Buscar usuário
- `PUT /api/usuarios/:id` - Atualizar usuário
- `DELETE /api/usuarios/:id` - Deletar usuário

### Notas

- `GET /api/notas` - Listar notas
- `POST /api/notas` - Criar nota
- `GET /api/notas/:id` - Buscar nota
- `PUT /api/notas/:id` - Atualizar nota
- `DELETE /api/notas/:id` - Deletar nota

### Etiquetas

- `GET /api/etiquetas` - Listar etiquetas
- `POST /api/etiquetas` - Criar etiqueta
- `GET /api/etiquetas/:id` - Buscar etiqueta
- `PUT /api/etiquetas/:id` - Atualizar etiqueta
- `DELETE /api/etiquetas/:id` - Deletar etiqueta

### Status

- `GET /api/status` - Listar status
- `POST /api/status` - Criar status
- `GET /api/status/:id` - Buscar status
- `PUT /api/status/:id` - Atualizar status
- `DELETE /api/status/:id` - Deletar status

## Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

## Contribuindo

Contribuições são bem-vindas! Sinta-se à vontade para abrir issues e pull requests.

## Suporte

Para questões e suporte, abra uma issue no GitHub.
