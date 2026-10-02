"""
Descrição: Cliente TCP interativo para o protocolo binário da Questão 2.
           Permite ao usuário enviar arquivos (ADDFILE), deletar arquivos (DELETE),
           listar arquivos remotos (GETFILESLIST) e baixar arquivos (GETFILE)
           utilizando formatação Big-Endian e cabeçalhos binários.
Autores: Daniel Suzuki Naves e Pedro Borges De Araujo
Data de criação: 25/09/2026
Última atualização: 30/09/2026
"""

import logging
import os
import socket
import struct

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - [%(levelname)s] - %(message)s"
)

# Porta do servidor Java (TCPServerQ2.java)
SERVER_HOST = "127.0.0.1"
SERVER_PORT = 9090

# Constantes do Protocolo Binário
MSG_REQUEST = 0x01
MSG_RESPONSE = 0x02

CMD_ADDFILE = 0x01
CMD_DELETE = 0x02
CMD_GETFILESLIST = 0x03
CMD_GETFILE = 0x04

STATUS_SUCCESS = 0x01
STATUS_ERROR = 0x02


def _recv_exact(sock: socket.socket, length: int) -> bytes:
    """Lê exatamente 'length' bytes do socket TCP evitando fragmentação.

    Args:
        sock (socket.socket): Conexão socket ativa.
        length (int): Quantidade exata de bytes esperada.

    Returns:
        bytes: Sequência exata de bytes lida do socket.

    Raises:
        EOFError: Se a conexão for encerrada antes de ler a quantidade necessária.
    """
    data = bytearray()
    while len(data) < length:
        packet = sock.recv(length - len(data))
        if not packet:
            raise EOFError("Conexão encerrada prematuramente pelo servidor.")
        data.extend(packet)
    return bytes(data)


class ClienteBinarioQ2:
    """Classe responsável por gerenciar a comunicação binária com o servidor Java."""

    def __init__(
        self,
        host: str = SERVER_HOST,
        port: int = SERVER_PORT,
        download_dir: str = "./downloads_cliente",
    ):
        """Inicializa as configurações da conexão e diretório de downloads.

        Args:
            host (str): Endereço IP do servidor Java.
            port (int): Porta de escuta do servidor Java.
            download_dir (str): Diretório local para salvar arquivos baixados.
        """
        self.host = host
        self.port = port
        self.download_dir = download_dir
        if not os.path.exists(self.download_dir):
            os.makedirs(self.download_dir)

    def _enviar_cabecalho_req(self, sock: socket.socket, cmd: int, filename: str) -> None:
        """Envia o cabeçalho base de requisição (1 byte MSG_REQUEST, 1 byte CMD, 1 byte N_LEN, N_BYTES filename).

        Args:
            sock (socket.socket): Socket TCP ativo.
            cmd (int): Código do comando (1 a 4).
            filename (str): Nome do arquivo alvo.
        """
        fn_bytes = filename.encode("utf-8")
        fn_size = len(fn_bytes)
        if fn_size > 255:
            raise ValueError("Nome do arquivo excede 255 bytes.")

        header = struct.pack(">BBB", MSG_REQUEST, cmd, fn_size)
        sock.sendall(header + fn_bytes)

    def _ler_cabecalho_resp(self, sock: socket.socket) -> tuple:
        """Lê os 3 bytes do cabeçalho de resposta: MSG_RESPONSE, CMD_ID, STATUS.

        Args:
            sock (socket.socket): Socket TCP ativo.

        Returns:
            tuple: (msg_type, cmd_id, status)
        """
        resp_bytes = _recv_exact(sock, 3)
        msg_type, cmd_id, status = struct.unpack(">BBB", resp_bytes)
        return msg_type, cmd_id, status

    def add_file(self, local_filepath: str) -> None:
        """ADDFILE (1): Envia um arquivo local para o servidor Java.

        Args:
            local_filepath (str): Caminho local do arquivo no computador do cliente.
        """
        if not os.path.exists(local_filepath):
            print("Arquivo local não encontrado.")
            return

        filename = os.path.basename(local_filepath)
        file_size = os.path.getsize(local_filepath)

        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.connect((self.host, self.port))

                self._enviar_cabecalho_req(sock, CMD_ADDFILE, filename)
                sock.sendall(struct.pack(">I", file_size))

                with open(local_filepath, "rb") as f:
                    while chunk := f.read(4096):
                        sock.sendall(chunk)

                _, _, status = self._ler_cabecalho_resp(sock)
                if status == STATUS_SUCCESS:
                    print(f"[ADDFILE] Arquivo '{filename}' enviado com SUCESSO!")
                else:
                    print(f"[ADDFILE] ERRO ao enviar arquivo '{filename}'.")
        except Exception as e:
            print(f"[ADDFILE] Erro no envio: {e}")

    def delete_file(self, filename: str) -> None:
        """DELETE (2): Solicita a remoção de um arquivo no servidor Java.

        Args:
            filename (str): Nome do arquivo a ser removido do servidor.
        """
        filename = os.path.basename(filename)
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.connect((self.host, self.port))

                self._enviar_cabecalho_req(sock, CMD_DELETE, filename)
                _, _, status = self._ler_cabecalho_resp(sock)

                if status == STATUS_SUCCESS:
                    print(f"[DELETE] Arquivo '{filename}' removido com SUCESSO!")
                else:
                    print(f"[DELETE] ERRO: Arquivo '{filename}' não encontrado.")
        except Exception as e:
            print(f"[DELETE] Erro de comunicação: {e}")

    def get_files_list(self) -> None:
        """GETFILESLIST (3): Solicita e exibe a lista de arquivos disponíveis no servidor Java."""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.connect((self.host, self.port))

                self._enviar_cabecalho_req(sock, CMD_GETFILESLIST, "")
                _, _, status = self._ler_cabecalho_resp(sock)

                if status == STATUS_SUCCESS:
                    count_bytes = _recv_exact(sock, 2)
                    (count,) = struct.unpack(">H", count_bytes)
                    print(f"\n--- Lista de Arquivos no Servidor ({count}) ---")

                    for _ in range(count):
                        fn_len_bytes = _recv_exact(sock, 1)
                        (fn_len,) = struct.unpack(">B", fn_len_bytes)
                        fn_bytes = _recv_exact(sock, fn_len)
                        fn = fn_bytes.decode("utf-8")
                        print(f" - {fn}")
                    print("-------------------------------------------\n")
                else:
                    print("[GETFILESLIST] ERRO ao obter lista de arquivos.")
        except Exception as e:
            print(f"[GETFILESLIST] Erro de comunicação: {e}")

    def get_file(self, filename: str) -> None:
        """GETFILE (4): Baixa um arquivo do servidor Java e o salva na pasta de downloads local.

        Args:
            filename (str): Nome do arquivo a ser baixado do servidor.
        """
        filename = os.path.basename(filename)
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.connect((self.host, self.port))

                self._enviar_cabecalho_req(sock, CMD_GETFILE, filename)
                _, _, status = self._ler_cabecalho_resp(sock)

                if status == STATUS_SUCCESS:
                    file_size_bytes = _recv_exact(sock, 4)
                    (file_size,) = struct.unpack(">I", file_size_bytes)

                    dest_path = os.path.join(self.download_dir, filename)
                    received = 0

                    with open(dest_path, "wb") as f:
                        while received < file_size:
                            to_read = min(4096, file_size - received)
                            chunk = sock.recv(to_read)
                            if not chunk:
                                break
                            f.write(chunk)
                            received += len(chunk)

                    print(
                        f"[GETFILE] Download concluído: '{filename}' ({received} bytes) em '{dest_path}'"
                    )
                else:
                    print(f"[GETFILE] ERRO: Arquivo '{filename}' não encontrado.")
        except Exception as e:
            print(f"[GETFILE] Erro no download: {e}")


def menu() -> None:
    """Exibe o menu interativo no console para execução de comandos do protocolo binário."""
    cliente = ClienteBinarioQ2()

    while True:
        print("\n=== CLIENTE BINÁRIO Q2 (TCP) ===")
        print("1. Enviar Arquivo (ADDFILE)")
        print("2. Deletar Arquivo (DELETE)")
        print("3. Listar Arquivos (GETFILESLIST)")
        print("4. Baixar Arquivo (GETFILE)")
        print("0. Sair")
        op = input("Escolha uma opção: ").strip()

        if op == "1":
            path = input("Caminho do arquivo local: ").strip()
            cliente.add_file(path)
        elif op == "2":
            name = input("Nome do arquivo no servidor: ").strip()
            cliente.delete_file(name)
        elif op == "3":
            cliente.get_files_list()
        elif op == "4":
            name = input("Nome do arquivo para baixar: ").strip()
            cliente.get_file(name)
        elif op == "0":
            break
        else:
            print("Opção inválida.")


if __name__ == "__main__":
    menu()