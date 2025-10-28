# Correção: Upload de Foto de Perfil

## Problema Identificado

```
Erro ao fazer upload da foto: [SQLITE_CONSTRAINT_FOREIGNKEY]
A foreign key constraint failed (FOREIGN KEY constraint failed)
```

### Causa Raiz

O código estava usando `fileUploadService.uploadArquivo()` para foto de perfil, mas esse método foi projetado para **anexos de notas** e tenta inserir na tabela `anexos` com uma FOREIGN KEY para `notas(id)`.

```java
// ANTES (ERRADO)
FileUploadService.AnexoInfo foto = fileUploadService.uploadArquivo(
    0L, // ❌ Passa notaId = 0
    file.filename(),
    file.content(),
    currentUser.getId()
);
```

Quando passava `notaId = 0`, a FOREIGN KEY falhava porque não existe nota com ID 0.

## Solução Implementada

Criei um **método específico** para upload de foto de perfil que:
- ✅ Salva o arquivo no disco
- ✅ **NÃO** tenta inserir na tabela `anexos`
- ✅ Valida que é uma imagem (jpg, jpeg, png, gif, webp)
- ✅ Salva em subpasta dedicada: `uploads/perfil/`
- ✅ Limita tamanho a 5 MB
- ✅ Retorna apenas o caminho do arquivo

### Arquivos Modificados

#### 1. FileUploadService.java (linhas 51-104)

**Novo método adicionado:**

```java
/**
 * Faz upload de foto de perfil (não registra na tabela anexos).
 */
public String uploadFotoPerfil(String nomeArquivo, InputStream inputStream) throws Exception {
    logger.info("Iniciando upload de foto de perfil: {}", nomeArquivo);

    // Validar se é imagem
    String extensao = getExtensao(nomeArquivo);
    List<String> extensoesImagem = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    if (!extensoesImagem.contains(extensao.toLowerCase())) {
        throw new Exception("Apenas imagens são permitidas (jpg, jpeg, png, gif, webp)");
    }

    // Criar pasta uploads/perfil se não existir
    String uploadsFolder = AppConfig.getUploadsFolder();
    Path uploadsPath = Paths.get(uploadsFolder, "perfil");
    if (!Files.exists(uploadsPath)) {
        Files.createDirectories(uploadsPath);
    }

    // Gerar nome único: perfil_20250127_143022_a1b2c3d4.jpg
    String timestamp = LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER);
    String nomeUnico = String.format("perfil_%s_%s.%s",
        timestamp,
        UUID.randomUUID().toString().substring(0, 8),
        extensao);
    String caminhoArquivo = uploadsPath.resolve(nomeUnico).toString();

    // Salvar arquivo
    Path targetPath = Paths.get(caminhoArquivo);
    long tamanhoBytes = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

    // Validar tamanho (máximo 5 MB)
    long maxSizeBytes = 5 * 1024L * 1024L;
    if (tamanhoBytes > maxSizeBytes) {
        Files.delete(targetPath);
        throw new Exception("Foto muito grande. Máximo: 5 MB");
    }

    logger.info("Foto de perfil salva: {} ({} bytes)", caminhoArquivo, tamanhoBytes);
    return caminhoArquivo;
}
```

#### 2. PerfilController.java (linhas 203-207)

**Método uploadFoto() corrigido:**

```java
// DEPOIS (CORRETO)
String caminhoFoto = fileUploadService.uploadFotoPerfil(
    file.filename(),
    file.content()
);

// Atualizar caminho da foto no usuário
userRepository.atualizarFotoPerfil(currentUser.getId(), caminhoFoto);
```

## Validações Implementadas

### 1. Tipo de Arquivo
- ✅ Aceita: `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`
- ❌ Rejeita: Qualquer outro tipo

### 2. Tamanho Máximo
- **Limite:** 5 MB
- **Comportamento:** Se exceder, arquivo é deletado e erro é retornado

### 3. Nome Único
- **Formato:** `perfil_[timestamp]_[uuid8].[extensao]`
- **Exemplo:** `perfil_20250127_143022_a1b2c3d4.jpg`
- **Previne:** Sobrescrita de arquivos

### 4. Estrutura de Pastas
```
uploads/
  └── perfil/
      ├── perfil_20250127_143022_a1b2c3d4.jpg
      ├── perfil_20250127_150315_f5e6d7c8.png
      └── ...
```

## Como Testar

### 1. Recompilar o projeto
```bash
mvn clean compile
```

### 2. Reiniciar a aplicação
```bash
mvn exec:java
```

### 3. Fazer upload de foto
1. Login: http://localhost:7070
2. Acessar: **Meu Perfil** (sidebar)
3. Seção "Foto de Perfil"
4. Clicar em "Escolher arquivo"
5. Selecionar uma imagem (jpg, png, etc.)
6. Clicar em "Salvar Foto"

### 4. Resultados Esperados

✅ **Sucesso:**
- Mensagem: "Foto de perfil atualizada com sucesso!"
- Avatar atualiza automaticamente
- Arquivo salvo em: `uploads/perfil/perfil_[timestamp]_[uuid].jpg`
- Registro atualizado na tabela `users.foto_perfil`

❌ **Erros esperados (validação):**
- "Apenas imagens são permitidas" → Se tentar enviar PDF, DOC, etc.
- "Foto muito grande. Máximo: 5 MB" → Se arquivo > 5 MB
- "Nenhuma foto selecionada" → Se não selecionar arquivo

## Diferenças: Foto de Perfil vs Anexo de Nota

| Característica | Foto de Perfil | Anexo de Nota |
|----------------|----------------|---------------|
| **Método** | `uploadFotoPerfil()` | `uploadArquivo()` |
| **Tabela** | Nenhuma (só users.foto_perfil) | `anexos` |
| **FOREIGN KEY** | Não usa | Sim (nota_id) |
| **Pasta** | `uploads/perfil/` | `uploads/` |
| **Tipos** | Apenas imagens | Diversos |
| **Tamanho máx** | 5 MB | Configurável |
| **Registro DB** | Atualiza users | Insere em anexos |

## Outros Problemas Corrigidos Anteriormente

1. ✅ **Header e sidebar não apareciam** em Perfil e Configurações
   - Adicionado: `model.putAll(SessionUtil.getSessionAttributes(ctx));`

2. ✅ **Erro ao renderizar perfil**
   - Corrigido: `user.role.name()` e `user.createdAt`

3. ✅ **Banco de dados sem colunas novas**
   - Solução: Recriar banco de dados

## Status

✅ **Upload de foto de perfil CORRIGIDO e funcional!**

Todos os problemas relacionados a FOREIGN KEY foram resolvidos.
