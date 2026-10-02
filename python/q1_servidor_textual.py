"""
Descrição: Servidor TCP multithreaded para o protocolo textual da Questão 1.
           Atende múltiplos clientes simultâneos em UTF-8, processando
           autenticação segura via hash SHA-512 e comandos de navegação
           de diretórios (PWD, CHDIR, GETFILES, GETDIRS, EXIT).
Autores: Daniel Suzuki Naves e Pedro Borges De Araujo
Data de criação: 25/09/2026
Última atualização: 01/10/2026
"""

import hashlib
import logging
import os
import socket
import struct
import threading

# Configuração de logs no console
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - [%(levelname)s] - %(message)s"
)

# Porta definida para a Questão 1
SERVER_PORT = 8080

# Base de usuários cadastrados (senhas armazenadas em Hash SHA-512)
# admin: 123456 | aluno: utfpr2026
USERS_DB = {
    "admin": hashlib.sha512(b"123456").hexdigest(),
    "aluno": hashlib.sha512(b"utfpr2026").hexdigest(),
}


def recv_utf(sock: socket.socket) -> str:
    """Lê uma String UTF enviada pelo DataOutputStream.writeUTF() do Java.

    Args:
        sock (socket.socket): Socket TCP da conexão do cliente.

    Returns:
        str: Texto decodificado em UTF-8 recebido do cliente.

    Raises:
        EOFError: Se a conexão for encerrada ou interrompida antes da leitura completa.
    """
    length_bytes = sock.recv(2)
    if not length_bytes or len(length_bytes) < 2:
        raise EOFError("Conexão encerrada pelo cliente.")

    # Desempacota a tupla para obter o inteiro com o tamanho em bytes
    (length,) = struct.unpack(">H", length_bytes)

    data = bytearray()
    while len(data) < length:
        packet = sock.recv(length - len(data))
        if not packet:
            raise EOFError("Conexão interrompida antes de receber todo o pacote.")
        data.extend(packet)

    return data.decode("utf-8")


def send_utf(sock: socket.socket, text: str) -> None:
    """Envia uma String UTF no formato compatível com DataInputStream.readUTF() do Java.

    Args:
        sock (socket.socket): Socket TCP da conexão do cliente.
        text (str): Texto a ser enviado ao cliente.
    """
    encoded = text.encode("utf-8")
    header = struct.pack(">H", len(encoded))
    sock.sendall(header + encoded)


def handle_client(client_socket: socket.socket, client_address: tuple) -> None:
    """Thread dedicada para atender e processar as requisições de cada cliente Java.

    Args:
        client_socket (socket.socket): Socket ativo com o cliente.
        client_address (tuple): Endereço IP e porta do cliente.
    """
    logging.info(f"Cliente conectado: {client_address}")
    authenticated = False

    # Garante diretório de trabalho padrão do servidor
    storage_dir = os.path.abspath("./storage_servidor")
    if not os.path.exists(storage_dir):
        os.makedirs(storage_dir)

    current_dir = storage_dir

    try:
        while True:
            cmd_str = recv_utf(client_socket)
            logging.info(f"[{client_address}] Comando recebido: {cmd_str}")

            if cmd_str.startswith("CONNECT"):
                # O cliente Java envia: "CONNECT user, password_hash"
                try:
                    payload = cmd_str[7:].strip()
                    parts = [p.strip() for p in payload.split(",", 1)]
                    user = parts[0] if len(parts) > 0 else ""
                    pass_hash = parts[1] if len(parts) > 1 else ""

                    if user in USERS_DB and USERS_DB[user] == pass_hash:
                        authenticated = True
                        send_utf(client_socket, "SUCCESS")
                        logging.info(f"[{client_address}] Usuário '{user}' autenticado.")
                    else:
                        send_utf(client_socket, "ERROR")
                        logging.warning(
                            f"[{client_address}] Falha de autenticação para usuário '{user}'."
                        )
                except Exception as e:
                    logging.error(f"Erro no processamento de CONNECT: {e}")
                    send_utf(client_socket, "ERROR")

            elif not authenticated:
                # Rejeita comandos de clientes não autenticados
                send_utf(client_socket, "ERROR")

            elif cmd_str == "PWD":
                formatted_path = current_dir.replace("\\", "/")
                send_utf(client_socket, formatted_path)

            elif cmd_str.startswith("CHDIR"):
                new_path = cmd_str[5:].strip()
                target_dir = os.path.abspath(os.path.join(current_dir, new_path))

                if os.path.exists(target_dir) and os.path.isdir(target_dir):
                    current_dir = target_dir
                    send_utf(client_socket, "SUCCESS")
                    logging.info(
                        f"[{client_address}] Diretório alterado para: {current_dir}"
                    )
                else:
                    send_utf(client_socket, "ERROR")

            elif cmd_str == "GETFILES":
                files = [
                    f
                    for f in os.listdir(current_dir)
                    if os.path.isfile(os.path.join(current_dir, f))
                ]
                send_utf(client_socket, str(len(files)))
                for f in files:
                    send_utf(client_socket, f)

            elif cmd_str == "GETDIRS":
                dirs = [
                    d
                    for d in os.listdir(current_dir)
                    if os.path.isdir(os.path.join(current_dir, d))
                ]
                send_utf(client_socket, str(len(dirs)))
                for d in dirs:
                    send_utf(client_socket, d)

            elif cmd_str == "EXIT":
                logging.info(f"[{client_address}] Conexão encerrada via EXIT.")
                break

            else:
                send_utf(client_socket, "ERROR")

    except (EOFError, ConnectionResetError):
        logging.info(f"[{client_address}] Cliente desconectou.")
    except Exception as e:
        logging.error(f"[{client_address}] Erro na conexão: {e}")
    finally:
        client_socket.close()


def main() -> None:
    """Inicializa o Socket servidor TCP e aguarda conexões de novos clientes."""
    host = "0.0.0.0"
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind((host, SERVER_PORT))
    server_socket.listen(5)

    logging.info(f"Servidor Q1 (Python) escutando na porta {SERVER_PORT}...")

    while True:
        sock, addr = server_socket.accept()
        thread = threading.Thread(
            target=handle_client, args=(sock, addr), daemon=True
        )
        thread.start()


if __name__ == "__main__":
    main()
