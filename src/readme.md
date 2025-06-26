# Fórum com Comunicação TCP em Java

Este projeto implementa um sistema de fórum cliente-servidor utilizando o protocolo TCP em Java. A comunicação é realizada através da troca de mensagens no formato JSON, e o servidor é capaz de lidar com múltiplos clientes simultaneamente usando threads.

## Funcionalidades

### Servidor
- **Multithreaded:** Cada cliente conectado é gerenciado por uma thread dedicada, permitindo múltiplas conexões simultâneas.
- **Interface Gráfica (GUI):** Possui uma GUI em Swing para monitorar os clientes conectados e autenticados, além de visualizar um log de eventos em tempo real.
- **Persistência em Memória:** Os dados de usuários, tópicos e respostas são armazenados em repositórios em memória durante a execução do servidor.
- **Gerenciamento de Usuários:** Suporte para dois tipos de papéis: `comum` e `admin`, com permissões distintas.

### Cliente
- **Interface Gráfica (GUI):** Interface completa em Swing para permitir que o usuário interaja com o sistema.
- **Autenticação de Usuário:** Funcionalidades de login, cadastro, logout, alteração de perfil e exclusão de conta.
- **Interação com Fórum:** Criação de tópicos, visualização de todos os tópicos, envio de respostas e visualização de respostas de um tópico.
- **Operações de Administrador:** Painel dedicado para administradores realizarem operações especiais, como gerenciar usuários e mensagens.

### Protocolo de Comunicação
- O sistema utiliza **TCP/IP** para uma comunicação confiável.
- As mensagens trocam informações no formato **JSON**, com cada objeto JSON sendo enviado como uma única linha de texto.
- Cada mensagem contém um código de operação (`op`) que define a ação a ser executada (ex: `"op": "000"` para login).

## Estrutura do Projeto

- `src/client`: Contém as classes da aplicação cliente, incluindo a interface gráfica (`ClientApp`) e a lógica de conexão (`ClientConnection`).
- `src/server`: Contém as classes da aplicação servidora, como `ServerApp` (principal), `ClientHandler` (lógica por cliente) e os pacotes de `repository` e `service`.
- `src/common`: Classes compartilhadas entre cliente e servidor, como `ProtocolMessage` (a estrutura da mensagem) e `SerializationHelper` (para converter objetos para/de JSON).

## Como Executar

### Pré-requisitos
1.  **JDK (Java Development Kit)**: Versão 8 ou superior.
2.  **Biblioteca GSON:** Necessária para a manipulação de JSON.
    - **Download:** [Faça o download do JAR do GSON aqui](https://search.maven.org/artifact/com.google.code.gson/gson/2.10.1/jar).

### Configurando a Dependência (GSON)

Você precisa adicionar o arquivo `gson-2.10.1.jar` ao classpath do seu projeto. Em uma IDE como IntelliJ ou Eclipse:

1.  Vá em `File > Project Structure...`.
2.  Navegue até `Modules > Dependencies`.
3.  Clique no botão `+` e selecione `JARs or directories...`.
4.  Encontre e selecione o arquivo `gson-2.10.1.jar` que você baixou.
5.  Clique em **Apply** e **OK**.

### Compilando e Executando via Linha de Comando

Abra um terminal na pasta `src` do projeto.

**1. Compile todos os arquivos Java:**

*No Windows:*
   ```bash
   javac -cp ".;c:\path\to\gson-2.10.1.jar" */*.java */*/*.java */*/*/*.java