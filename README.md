# Trabalho 01 — Programação com Sockets TCP
**Universidade Tecnológica Federal do Paraná (UTFPR)**  
**Disciplina:** Sistemas Distribuídos (7º Período)  
**Professor:** Prof. Rodrigo Campiolo  
**Autores:** Daniel Suzuki Naves e Pedro Borges De Araujo  
**Data de Criação:** 25/09/2026 | **Última Atualização:** 30/09/2026  

---

## Descrição do Projeto
Este projeto consiste no desenvolvimento de uma aplicação distribuída para gerenciamento remoto de arquivos utilizando **Sockets TCP**. O objetivo principal é demonstrar a comunicação entre processos e a interoperabilidade de sistemas heterogêneos desenvolvidos em **Python** e **Java**:

1. **Questão 1 — Protocolo Textual em UTF-8 (Porta 8080):**
   - **Servidor (Python):** Servidor TCP multithreaded que gerencia autenticação via hash SHA-512 e comandos de navegação em árvore de diretórios (`PWD`, `CHDIR`, `GETFILES`, `GETDIRS`, `EXIT`).
   - **Cliente (Java):** Aplicação interativa em Java que realiza comunicação estruturada em UTF-8 usando `DataOutputStream` e `DataInputStream`.

2. **Questão 2 — Protocolo Binário Customizado Big-Endian (Porta 9090):**
   - **Servidor (Java):** Servidor TCP multithreaded em Java que processa requisições binárias estritas com ordenação Big-Endian e registra auditorias em arquivo de log (`server_q2.log`).
   - **Cliente (Python):** Aplicação Python interativa que utiliza a biblioteca `struct` para empacotar e desempacotar mensagens binárias de upload (`ADDFILE`), remoção (`DELETE`), listagem (`GETFILESLIST`) e download (`GETFILE`).

---

## Bibliotecas Utilizadas
A aplicação foi construída utilizando exclusivamente as **bibliotecas padrão** de cada linguagem (sem dependências externas de terceiros):

- **Linguagem Java:**
  - `java.net.*`: Gerenciamento de sockets TCP (`Socket`, `ServerSocket`).
  - `java.io.*`: Manipulação de fluxos de entrada/saída (`DataInputStream`, `DataOutputStream`).
  - `java.security.*`: Algoritmo de hash SHA-512 (`MessageDigest`).
  - `java.util.logging.*`: Registro de logs em console e arquivo (`Logger`, `FileHandler`).
  - `java.nio.file.*`: Operações do sistema de arquivos local (`Files`, `Paths`).

- **Linguagem Python:**
  - `socket`: Comunicação por Sockets TCP do sistema operacional.
  - `struct`: Empacotamento e desempacotamento de dados binários em ordem Big-Endian.
  - `threading`: Atendimento concorrente de múltiplos clientes em Threads separadas.
  - `hashlib`: Cálculo de hash SHA-512 para validação de credenciais.
  - `logging`: Formatação e exibição de mensagens de log no console.
  - `os`: Manipulação de diretórios e validação de caminhos no disco.

---

## Estrutura do Repositório
```text
trabalho-sd-tcp-utfpr/
├── README.md                  # Documentação e guia de execução do projeto
├── .gitignore                 # Filtro de arquivos binários e temporários
│
├── python/                    # Módulos desenvolvidos em Python (Daniel Suzuki Naves)
│   ├── q1_servidor_textual.py # Servidor do Protocolo Textual (Questão 1) - Porta 8080
│   ├── q2_cliente_binario.py  # Cliente do Protocolo Binário (Questão 2) - Porta 9090
│   ├── storage_servidor/      # Diretório de arquivos do Servidor Q1
│   └── downloads_cliente/     # Diretório de downloads do Cliente Q2
│
├── java/                      # Módulos desenvolvidos em Java (Pedro Borges De Araujo)
│   ├── q1/
│   │   └── TCPClientQ1.java   # Cliente do Protocolo Textual (Questão 1)
│   └── q2/
│       ├── TCPServerQ2.java   # Servidor do Protocolo Binário (Questão 2) - Porta 9090
│       └── storage/           # Diretório de arquivos do Servidor
```
---

## Configuração de Portas e Credenciais
* **Porta da Questão 1 (Servidor Textual):** `8080`
* **Porta da Questão 2 (Servidor Binário):** `9090`
* **Usuários Cadastrados para Autenticação (Questão 1):**
  * Usuário: `admin` | Senha: `123456` (Enviada em Hash SHA-512)
  * Usuário: `aluno` | Senha: `utfpr2026` (Enviada em Hash SHA-512)

---

## Como Executar as Aplicações
**Pré-requisitos**
* **Python 3.9+** instalado.
* **JDK 17+** instalado.
---
1️⃣ **Executando a Questão 1 (Servidor Python + Cliente Java)**
1. **Inicie o Servidor Textual em Python:**
```bash
cd python
python q1_servidor_textual.py
```
2. **Em outro terminal, compile e execute o Cliente Java:**
```bash
cd java
cd q1
javac Q1_TCPClientTextual.java
java Q1_TCPClientTextual
```
---
2️⃣ ***Executando a Questão 2 (Servidor Java + Cliente Python)***
1. **Inicie o Servidor Binário em Java:**
```bash
cd java
cd q2
javac Q2_TCPServerBinario.java
java Q2_TCPServerBinario
```
2. **Em outro terminal, execute o Cliente Binário em Python:**
```bash
cd python
python q2_cliente_binario.py
```
---
## Exemplo de Uso
### Exemplo de Interação — Questão 1 (Cliente Java):
```text
Conectado ao servidor 127.0.0.1:8080

> CONNECT
Usuário: admin
Senha: 123456
Resposta: SUCCESS

> PWD
Diretório atual: /Users/.../storage_servidor

> GETFILES
Arquivos (2):
  aula1.txt
  relatorio.pdf

> GETDIRS
Diretórios (1):
  documents

> CHDIR documents
Resposta: SUCCESS

> EXIT
Encerrando conexão.
```
---
### Exemplo de Interação — Questão 2 (Cliente Python):
```text
=== CLIENTE BINÁRIO Q2 (TCP) ===
1. Enviar Arquivo (ADDFILE)
2. Deletar Arquivo (DELETE)
3. Listar Arquivos (GETFILESLIST)
4. Baixar Arquivo (GETFILE)
0. Sair
Escolha uma opção: 1
Caminho do arquivo local: /Users/.../README.md
[ADDFILE] Arquivo 'README.md' enviado com SUCESSO!

Escolha uma opção: 3
--- Lista de Arquivos no Servidor (1) ---
 - README.md
-------------------------------------------

Escolha uma opção: 4
Nome do arquivo para baixar: README.md
[GETFILE] Download concluído: 'README.md' (4240 bytes) em './downloads_cliente/README.md'

Escolha uma opção: 2
Nome do arquivo no servidor: README.md
[DELETE] Arquivo 'README.md' removido com SUCESSO!
```
---
