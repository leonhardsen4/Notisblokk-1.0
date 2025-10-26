# 📦 Como Gerar o Instalador Windows (.exe)

Este guia explica como criar o instalador Windows do Notisblokk usando **jpackage**.

---

## Pré-requisitos

✅ **Java JDK 14 ou superior** instalado
✅ **Maven** instalado
✅ **WiX Toolset 3.14+** instalado (para gerar .exe/.msi)
✅ Projeto já compilado (`mvn clean package`)

### Verificar versão do Java:
```bash
java -version
```
Deve mostrar algo como: `java version "21.0.x"` ou superior a 14

### Instalar WiX Toolset (obrigatório para .exe):

1. **Download:** https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm
   - Baixe `wix314.exe`

2. **Instalar:** Execute o instalador

3. **Adicionar ao PATH:**
   - Variáveis de Ambiente → Path → Adicionar: `C:\Program Files (x86)\WiX Toolset v3.14\bin`

4. **Verificar:**
   ```cmd
   candle.exe -?
   ```

**Alternativa sem WiX:** Use o script `build-portable.bat` para gerar versão portátil (sem instalador)

---

## Opção A: Instalador .exe (Requer WiX) 📦

### Método 1: Script Automático (Recomendado) 🚀

Execute o script fornecido:

```bash
build-exe.bat
```

**O script faz automaticamente:**
1. Verifica se o JAR existe
2. Cria diretório `dist/`
3. Executa o jpackage
4. Gera o instalador em `dist/Notisblokk-1.0.exe`

### Método 2: Comando Manual

Se preferir executar manualmente:

```bash
# 1. Compilar o projeto (se ainda não fez)
mvn clean package

# 2. Executar jpackage
jpackage --input target --name "Notisblokk" --main-jar notisblokk.jar --main-class com.notisblokk.Main --type exe --dest dist --app-version 1.0 --description "Notisblokk - Sistema de Gerenciamento" --vendor "Notisblokk Team" --win-console --win-shortcut --win-menu --win-dir-chooser
```

---

## Resultado

Após executar, você terá:

📁 **Arquivo gerado:** `dist/Notisblokk-1.0.exe`
📏 **Tamanho:** ~60-80 MB (inclui JRE embutida)
⏱️ **Tempo de build:** 2-5 minutos

---

## O que o instalador inclui:

✅ Aplicação completa (JAR + recursos)
✅ **JRE embutida** (Java Runtime) - usuários **não precisam** ter Java instalado
✅ Atalho no Menu Iniciar do Windows
✅ Opção de criar atalho na área de trabalho
✅ Instalador/Desinstalador nativos do Windows
✅ Escolha do diretório de instalação

---

## Como distribuir:

Basta enviar o arquivo `dist/Notisblokk-1.0.exe` para os usuários.

**Eles precisarão:**
1. Executar o instalador
2. Seguir o wizard de instalação
3. Após instalado, executar "Notisblokk" no Menu Iniciar

**A aplicação vai:**
- Iniciar automaticamente na porta 8080
- Estar acessível em `http://localhost:8080` ou `http://0.0.0.0:8080`
- Criar o banco de dados `notisblokk.db` no diretório de instalação

---

## Opção B: Versão Portátil (SEM WiX) 📁

**Use esta opção se não quiser instalar o WiX Toolset.**

### Gerar Versão Portátil:

Execute o script:
```bash
build-portable.bat
```

### O que é gerado:

📁 **Pasta:** `dist-portable/Notisblokk/`
⚡ **Executável:** `dist-portable/Notisblokk/Notisblokk.exe`
📏 **Tamanho:** ~60-80 MB

### Vantagens:

✅ **Não precisa WiX** - funciona sem instalação adicional
✅ **Totalmente portátil** - copie a pasta para qualquer lugar
✅ **JRE embutida** - não precisa Java instalado
✅ **Executável direto** - clique duplo e funciona
✅ **Zero instalação** - não modifica registro do Windows

### Como usar:

1. Copie a pasta `dist-portable/Notisblokk` para onde quiser
2. Execute `Notisblokk.exe`
3. Pronto! Sistema funcionando

### Como distribuir:

1. Comprima a pasta `dist-portable/Notisblokk` em ZIP
2. Envie o ZIP para os usuários
3. Eles descompactam e executam o .exe

**Ideal para:**
- Pen drive (rodar de qualquer computador)
- Testes
- Ambientes sem permissão de instalação
- Deploy em servidores Windows

---

## Adicionar Ícone Personalizado (Opcional)

Para usar um ícone personalizado no instalador:

1. **Converter icon.png para icon.ico:**
   - Online: https://convertio.co/pt/png-ico/
   - Ou use ImageMagick: `magick convert icon.png -define icon:auto-resize=256,128,64,48,32,16 icon.ico`

2. **Salvar em:** `src/main/resources/public/img/icon.ico`

3. **Adicionar no comando jpackage:**
   ```bash
   --icon src/main/resources/public/img/icon.ico
   ```

---

## Solução de Problemas

### ❌ Erro: "jpackage não é reconhecido"
**Solução:** Você precisa do JDK 14+. Verifique com `java -version`

### ❌ Erro: "JAR não encontrado"
**Solução:** Execute primeiro `mvn clean package`

### ❌ Instalador não inicia a aplicação
**Solução:** Verifique se a porta 8080 não está em uso

### ❌ Firewall bloqueia a aplicação
**Solução:** Permita o acesso ao executável nas configurações do Firewall do Windows

---

## Build em Outros Sistemas Operacionais

O jpackage também funciona em Linux e macOS:

**Linux (.deb ou .rpm):**
```bash
jpackage --input target --name "Notisblokk" --main-jar notisblokk.jar --main-class com.notisblokk.Main --type deb --dest dist
```

**macOS (.dmg):**
```bash
jpackage --input target --name "Notisblokk" --main-jar notisblokk.jar --main-class com.notisblokk.Main --type dmg --dest dist
```

---

## Notas Importantes

⚠️ O instalador contém uma **JRE embutida**, por isso o tamanho é maior (~70MB)
⚠️ Cada build gera um instalador **específico do sistema operacional**
⚠️ Para distribuir em vários SOs, você precisa buildar em cada um
⚠️ O banco de dados SQLite (`notisblokk.db`) é criado no diretório onde o app é executado

---

**Pronto para distribuição profissional!** 🎉
