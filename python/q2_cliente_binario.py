import logging
import os
import socket
import struct

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - [%(levelname)s] - %(message)s"
)

# Porta definida no servidor Java (TCPServerQ2.java)
SERVER_HOST = "127.0.0.1"
SERVER_PORT = 9090

# Constantes do Protocolo Binário (TCPServerQ2.java)
MSG_REQUEST = 0x01
MSG_RESPONSE = 0x02

CMD_ADDFILE = 0x01
CMD_DELETE = 0x02
CMD_GETFILESLIST = 0x03
CMD_GETFILE = 0x04

STATUS_SUCCESS = 0x01
STATUS_ERROR = 0x02


class ClienteBinarioQ2:

    def __init__(
        self,
        host: str = SERVER_HOST,
        port: int = SERVER_PORT,
        download_dir: str = "./downloads_cliente",
    ):
        self.host = host
        self.port = port
        self.download_dir = download_dir
        if not os.path.exists(self.download_dir):
            os.makedirs(self.download_dir)

    def _enviar_cabecalho_req(self, sock: socket.socket, cmd: int, filename: str):
        """Envia o cabeçalho base de requisição:
        1 byte (MSG_REQUEST=0x01), 1 byte (CMD), 1 byte (Tamanho do Nome), Nome em bytes.
        """
        fn_bytes = filename.encode("utf-8")
        fn_size = len(fn_bytes)
        if fn_size > 255:
            raise ValueError("Nome do arquivo excede 255 bytes.")

        header = struct.pack(">BBB", MSG_REQUEST, cmd, fn_size)
        sock.sendall(header + fn_bytes)

    def _ler_cabecalho_resp(self, sock: socket.socket) -> tuple:
        """Lê os 3 bytes do cabeçalho de resposta: MSG_RESPONSE, CMD_ID, STATUS."""
        resp_bytes = sock.recv(3)
        if len(resp_bytes) < 3:
            raise ConnectionError("Resposta incompleta do servidor.")
        msg_type, cmd_id, status = struct.unpack(">BBB", resp_bytes)
        return msg_type, cmd_id, status

    def add_file(self, local_filepath: str):
        """ADDFILE (1): Envia arquivo local para o servidor Java."""
        if not os.path.exists(local_filepath):
            print("Arquivo local não encontrado.")
            return

        filename = os.path.basename(local_filepath)
        file_size = os.path.getsize(local_filepath)

        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.connect((self.host, self.port))

            # 1. Cabeçalho básico
            self._enviar_cabecalho_req(sock, CMD_ADDFILE, filename)

            # 2. 4 bytes big-endian com o tamanho do arquivo
            sock.sendall(struct.pack(">I", file_size))

            # 3. Envia o conteúdo do arquivo em blocos
            with open(local_filepath, "rb") as f:
                while chunk := f.read(4096):
                    sock.sendall(chunk)

            # 4. Lê o status da resposta
            _, _, status = self._ler_cabecalho_resp(sock)
            if status == STATUS_SUCCESS:
                print(f"[ADDFILE] Arquivo '{filename}' enviado com SUCESSO!")
            else:
                print(f"[ADDFILE] ERRO ao enviar arquivo '{filename}'.")

    def delete_file(self, filename: str):
        """DELETE (2): Remove arquivo no servidor Java."""
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.connect((self.host, self.port))

            self._enviar_cabecalho_req(sock, CMD_DELETE, filename)
            _, _, status = self._ler_cabecalho_resp(sock)

            if status == STATUS_SUCCESS:
                print(f"[DELETE] Arquivo '{filename}' removido com SUCESSO!")
            else:
                print(f"[DELETE] ERRO: Arquivo '{filename}' não encontrado.")

    def get_files_list(self):
        """GETFILESLIST (3): Lista arquivos do servidor Java."""
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.connect((self.host, self.port))

            self._enviar_cabecalho_req(sock, CMD_GETFILESLIST, "")
            _, _, status = self._ler_cabecalho_resp(sock)

            if status == STATUS_SUCCESS:
                # Lê 2 bytes Big-Endian com a quantidade de arquivos
                count_bytes = sock.recv(2)
                count = struct.unpack(">H", count_bytes)[0]
                print(f"\n--- Lista de Arquivos no Servidor ({count}) ---")

                for _ in range(count):
                    fn_len = struct.unpack(">B", sock.recv(1))[0]
                    fn = sock.recv(fn_len).decode("utf-8")
                    print(f" - {fn}")
                print("-------------------------------------------\n")
            else:
                print("[GETFILESLIST] ERRO ao obter lista de arquivos.")

    def get_file(self, filename: str):
        """GETFILE (4): Realiza download de arquivo do servidor Java."""
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.connect((self.host, self.port))

            self._enviar_cabecalho_req(sock, CMD_GETFILE, filename)
            _, _, status = self._ler_cabecalho_resp(sock)

            if status == STATUS_SUCCESS:
                # Lê 4 bytes Big-Endian com o tamanho do arquivo
                file_size_bytes = sock.recv(4)
                file_size = struct.unpack(">I", file_size_bytes)[0]

                dest_path = os.path.join(self.download_dir, filename)
                received = 0

                with open(dest_path, "wb") as f:
                    while received < file_size:
                        chunk = sock.recv(min(4096, file_size - received))
                        if not chunk:
                            break
                        f.write(chunk)
                        received += len(chunk)

                print(
                    f"[GETFILE] Download concluído: '{filename}' ({received} bytes) em '{dest_path}'"
                )
            else:
                print(f"[GETFILE] ERRO: Arquivo '{filename}' não encontrado.")


def menu():
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