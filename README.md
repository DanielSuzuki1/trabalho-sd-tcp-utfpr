# Atividade 01 — Programação com Sockets TCP

**Universidade Tecnológica Federal do Paraná (UTFPR)**  
**Disciplina:** Sistemas Distribuídos (7º Período)  
**Professor:** Prof. Rodrigo Campiolo

---

## 👥 Integrantes da Dupla

- **[Daniel Suzuki Naves]** — _Desenvolvimento em Python_
- **[Pedro Borges Araújo]** — _Desenvolvimento em Java_

---

## 📌 Visão Geral do Projeto

Este projeto consiste na implementação de duas aplicações cliente-servidor distribuídas utilizando sockets TCP em linguagens distintas (**Python** e **Java**), garantindo a interoperabilidade entre diferentes sistemas operacionais e plataformas através de dois protocolos de comunicação:

1. **Questão 1 — Protocolo Textual (Navegação no Servidor)**:
   - **Servidor (Python)**: Atende múltiplos clientes simultâneos via _Threads_ na porta `6666`. Trata autenticação com hash SHA-512 e comandos de navegação de diretórios (`PWD`, `CHDIR`, `GETFILES`, `GETDIRS`, `EXIT`).
   - **Cliente (Java)**: Interage com o usuário e realiza a comunicação em UTF-8 utilizando `DataOutputStream` e `DataInputStream`.

2. **Questão 2 — Protocolo Binário (Gerenciador de Arquivos Remotos)**:
   - **Servidor (Java)**: Servidor multithreaded na porta `7777` que aceita requisições binárias estritas em Big-Endian e registra logs de execução.
   - **Cliente (Python)**: Envia requisições empacotadas em bytes para upload (`ADDFILE`), remoção (`DELETE`), listagem (`GETFILESLIST`) e download (`GETFILE`).

---

## 📂 Estrutura do Repositório

```text
trabalho-sd-tcp-utfpr/
├── README.md                  # Documentação do projeto
├── .gitignore                 # Arquivo para ignorar temporários e binários
│
├── python/                    # Módulos desenvolvidos em Python
│   ├── q1_servidor_textual.py # Servidor do Protocolo Textual (Questão 1)
│   ├── q2_cliente_binario.py  # Cliente do Protocolo Binário (Questão 2)
│   ├── storage_servidor/      # Diretório de arquivos do Servidor Q1
│   └── downloads_cliente/     # Diretório de downloads do Cliente Q2
│
├── java/                      # Módulos desenvolvidos em Java
│   ├── Q1_TCPClientTextual.java # Cliente do Protocolo Textual (Questão 1)
│   └── Q2_TCPServerBinario.java # Servidor do Protocolo Binário (Questão 2)
│
└── tests_local/               # Módulos auxiliares de testes locais
    ├── test_q1_cliente.py     # Cliente de teste local para a Questão 1
    └── test_q2_servidor.py    # Servidor mock de teste local para a Questão 2
⚙️ Mapeamento de Portas e Credenciais
Porta da Questão 1 (Textual): 6666
Porta da Questão 2 (Binária): 7777
Usuários Cadastrados para Autenticação (Q1):
Usuario: admin | Senha: 123456 (Hash SHA-512)
Usuario: aluno | Senha: utfpr2026 (Hash SHA-512)
🚀 Como Executar as Aplicações
Pró-requisitos
Python 3.10+ instalado.
JDK 17+ e compilador javac instalados.
1️⃣ Executando a Questão 1 (Servidor Python + Cliente Java)
Inicie o Servidor Textual em Python:
cd python
python q1_servidor_textual.py
Em outro terminal, compile e execute o Cliente Java:
cd java
javac Q1_TCPClientTextual.java
java Q1_TCPClientTextual
2️⃣ Executando a Questão 2 (Servidor Java + Cliente Python)
Inicie o Servidor Binário em Java:
cd java
javac Q2_TCPServerBinario.java
java Q2_TCPServerBinario
Em outro terminal, execute o Cliente Binário em Python:
cd python
python q2_cliente_binario.py
🧪 Testes Locais Unitários
Para testar os módulos em Python individualmente antes da integração com Java:
Testar o Servidor Python (Q1):
python python/q1_servidor_textual.py
# Em outro terminal:
python tests_local/test_q1_cliente.py
Testar o Cliente Python (Q2):
python tests_local/test_q2_servidor.py
# Em outro terminal:
python python/q2_cliente_binario.py

---
```
