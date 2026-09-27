# Atividade 01 — Programação com Sockets TCP

**Universidade Tecnológica Federal do Paraná (UTFPR)**  
**Disciplina:** Sistemas Distribuídos (7º Período)  
**Professor:** Prof. Rodrigo Campiolo  

---

## 👥 Integrantes da Dupla

* **[Daniel Suzuki Naves]** — *Desenvolvimento dos módulos em Python*
* **[Pedro Borges Araújo]** — *Desenvolvimento dos módulos em Java*

---

## 📌 Visão Geral do Projeto

Este projeto consiste na implementação de duas aplicações cliente-servidor distribuídas utilizando sockets TCP em linguagens distintas (**Python** e **Java**), garantindo a interoperabilidade entre diferentes sistemas e plataformas através de dois protocolos de comunicação:

1. **Questão 1 — Protocolo Textual (Navegação no Servidor)**:
   * **Servidor (Python)**: Atende múltiplos clientes simultâneos via *Threads* na porta `8080`. Trata autenticação com hash SHA-512 e comandos de navegação de diretórios (`PWD`, `CHDIR`, `GETFILES`, `GETDIRS`, `EXIT`).
   * **Cliente (Java)**: Interage com o usuário e realiza a comunicação em UTF-8 utilizando `DataOutputStream` e `DataInputStream`.

2. **Questão 2 — Protocolo Binário (Gerenciador de Arquivos Remotos)**:
   * **Servidor (Java)**: Servidor multithreaded na porta `7777` que aceita requisições binárias estritas em Big-Endian e registra logs de execução.
   * **Cliente (Python)**: Envia requisições empacotadas em bytes para upload (`ADDFILE`), remoção (`DELETE`), listagem (`GETFILESLIST`) e download (`GETFILE`).

---

## 📂 Estrutura do Repositório

```text
trabalho-sd-tcp-utfpr/
├── README.md                  # Documentação e guia de execução do projeto
├── .gitignore                 # Filtro de arquivos binários e temporários
│
├── python/                    # Módulos desenvolvidos em Python (Integrante 1)
│   ├── q1_servidor_textual.py # Servidor do Protocolo Textual (Questão 1) - Porta 8080
│   ├── q2_cliente_binario.py  # Cliente do Protocolo Binário (Questão 2) - Porta 7777
│   ├── storage_servidor/      # Diretório de arquivos do Servidor Q1
│   └── downloads_cliente/     # Diretório de downloads do Cliente Q2
│
├── java/                      # Módulos desenvolvidos em Java (Integrante 2)
│   ├── Q1_TCPClientTextual.java # Cliente do Protocolo Textual (Questão 1)
│   └── Q2_TCPServerBinario.java # Servidor do Protocolo Binário (Questão 2) - Porta 7777
│
└── tests_local/               # Scripts de testes locais em Python
    ├── test_q1_cliente.py     # Cliente para teste local da Questão 1
    └── test_q2_servidor.py    # Servidor mock para teste local da Questão 2
```
---

## ⚙️ Configuração de Portas e Credenciais
* **Porta da Questão 1 (Servidor Textual):** `8080`
* **Porta da Questão 2 (Servidor Binário):** `7777`
* **Usuários Cadastrados para Autenticação (Questão 1):**
  * Usuário: `admin` | Senha: `123456` (Enviada em Hash SHA-512)
  * Usuário: `aluno` | Senha: `utfpr2026` (Enviada em Hash SHA-512)

---

## 🚀 Como Executar as Aplicações
**Pré-requisitos**
* **Python 3.10+** instalado.
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
javac Q1_TCPClientTextual.java
java Q1_TCPClientTextual
```
---
2️⃣ ***Executando a Questão 2 (Servidor Java + Cliente Python)***
1. **Inicie o Servidor Binário em Java:**
```bash
cd java
javac Q2_TCPServerBinario.java
java Q2_TCPServerBinario
```
2. **Em outro terminal, execute o Cliente Binário em Python:**
```bash
cd python
python q2_cliente_binario.py
```

---

## 🧪 Executando os Testes Locais em Python
Para testar os scripts Python localmente antes da integração com o código Java:
* **Testar o Servidor Textual (Q1):**
python python/q1_servidor_textual.py
**Em outro terminal:**
python tests_local/test_q1_cliente.py
* **Testar o Cliente Binário (Q2):**
python tests_local/test_q2_servidor.py
**Em outro terminal:**
python python/q2_cliente_binario.py

---

