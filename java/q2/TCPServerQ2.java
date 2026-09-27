import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

/**
 * Questão 2 - Servidor TCP (protocolo binário Big-Endian)
 * Comandos: ADDFILE(1), DELETE(2), GETFILESLIST(3), GETFILE(4)
 *
 * Cabeçalho de solicitação:
 *   byte 1: tipo (0x01)
 *   byte 2: comando (0x01–0x04)
 *   byte 3: tamanho do nome do arquivo (N)
 *   N bytes: nome do arquivo
 *   [ADDFILE only] 4 bytes big-endian: tamanho do arquivo + bytes do arquivo
 *
 * Cabeçalho de resposta:
 *   byte 1: tipo (0x02)
 *   byte 2: comando (0x01–0x04)
 *   byte 3: status (1=SUCCESS, 2=ERROR)
 *   [GETFILESLIST] 2 bytes: qtd de arquivos + lista de nomes
 *   [GETFILE]      4 bytes: tamanho + bytes do arquivo
 */
public class TCPServerQ2 {

    private static final int    SERVER_PORT  = 9090;
    private static final String STORAGE_DIR  = "storage";
    private static final Logger LOGGER       = Logger.getLogger("TCPServerQ2");

    public static void main(String[] args) throws IOException {
        setupLogger();
        Files.createDirectories(Paths.get(STORAGE_DIR));

        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        LOGGER.info("Servidor iniciado na porta " + SERVER_PORT +
                    " | Armazenamento: " + Paths.get(STORAGE_DIR).toAbsolutePath());

        System.out.println("Servidor Q2 aguardando conexões na porta " + SERVER_PORT + "...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            LOGGER.info("Cliente conectado: " + clientSocket.getRemoteSocketAddress());
            new ClientHandlerQ2(clientSocket).start();
        }
    }

    // ── Logger: console + arquivo ─────────────────────────────────────────────

    private static void setupLogger() throws IOException {
        Logger root = Logger.getLogger("");
        // remove handler padrão de console para configurar um formatado
        for (Handler h : root.getHandlers()) root.removeHandler(h);

        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(Level.ALL);
        console.setFormatter(new SimpleFormatter());

        FileHandler file = new FileHandler("server_q2.log", true);
        file.setLevel(Level.ALL);
        file.setFormatter(new SimpleFormatter());

        LOGGER.addHandler(console);
        LOGGER.addHandler(file);
        LOGGER.setLevel(Level.ALL);
        LOGGER.setUseParentHandlers(false);
    }

    // ── Constantes de protocolo ───────────────────────────────────────────────

    static final byte MSG_REQUEST  = 0x01;
    static final byte MSG_RESPONSE = 0x02;

    static final byte CMD_ADDFILE      = 0x01;
    static final byte CMD_DELETE       = 0x02;
    static final byte CMD_GETFILESLIST = 0x03;
    static final byte CMD_GETFILE      = 0x04;

    static final byte STATUS_SUCCESS = 0x01;
    static final byte STATUS_ERROR   = 0x02;

    static String storagePath() { return STORAGE_DIR; }
}

// ── Thread por cliente ────────────────────────────────────────────────────────

class ClientHandlerQ2 extends Thread {

    private static final Logger LOGGER = Logger.getLogger("TCPServerQ2");

    private final Socket         socket;
    private DataInputStream      in;
    private DataOutputStream     out;

    ClientHandlerQ2(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String client = socket.getRemoteSocketAddress().toString();
        try {
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            while (true) {
                // Lê cabeçalho comum da requisição
                byte msgType = in.readByte();
                if (msgType != TCPServerQ2.MSG_REQUEST) {
                    LOGGER.warning("[" + client + "] Tipo de mensagem inválido: " + msgType);
                    break;
                }

                byte cmdId       = in.readByte();
                byte nameSize    = in.readByte();
                byte[] nameBytes = new byte[nameSize & 0xFF]; // unsigned
                in.readFully(nameBytes);
                String filename  = new String(nameBytes);

                LOGGER.info("[" + client + "] Comando=" + cmdId + " Arquivo=\"" + filename + "\"");

                switch (cmdId) {
                    case TCPServerQ2.CMD_ADDFILE      -> handleAddFile(client, filename);
                    case TCPServerQ2.CMD_DELETE       -> handleDelete(client, filename);
                    case TCPServerQ2.CMD_GETFILESLIST -> handleGetFilesList(client);
                    case TCPServerQ2.CMD_GETFILE      -> handleGetFile(client, filename);
                    default -> {
                        LOGGER.warning("[" + client + "] Comando desconhecido: " + cmdId);
                        sendHeader(cmdId, TCPServerQ2.STATUS_ERROR);
                        out.flush();
                    }
                }
            }

        } catch (EOFException e) {
            LOGGER.info("[" + client + "] Cliente desconectou.");
        } catch (IOException e) {
            LOGGER.warning("[" + client + "] Erro de IO: " + e.getMessage());
        } finally {
            close();
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    /** ADDFILE (1): lê 4 bytes de tamanho + bytes do arquivo e salva no disco. */
    private void handleAddFile(String client, String filename) throws IOException {
        int fileSize = in.readInt(); // big-endian via DataInputStream
        byte[] buffer = new byte[Math.min(fileSize, 4096)];
        Path dest = Paths.get(TCPServerQ2.storagePath(), sanitize(filename));

        try (OutputStream fos = Files.newOutputStream(dest)) {
            int remaining = fileSize;
            while (remaining > 0) {
                int toRead = Math.min(buffer.length, remaining);
                int read   = in.read(buffer, 0, toRead);
                if (read < 0) throw new EOFException("Stream encerrado antes do esperado.");
                fos.write(buffer, 0, read);
                remaining -= read;
            }
        }

        LOGGER.info("[" + client + "] ADDFILE \"" + filename + "\" (" + fileSize + " bytes) -> " + dest);
        sendHeader(TCPServerQ2.CMD_ADDFILE, TCPServerQ2.STATUS_SUCCESS);
        out.flush();
    }

    /** DELETE (2): remove o arquivo do disco. */
    private void handleDelete(String client, String filename) throws IOException {
        Path target = Paths.get(TCPServerQ2.storagePath(), sanitize(filename));
        boolean deleted = Files.deleteIfExists(target);

        if (deleted) {
            LOGGER.info("[" + client + "] DELETE \"" + filename + "\" OK");
            sendHeader(TCPServerQ2.CMD_DELETE, TCPServerQ2.STATUS_SUCCESS);
        } else {
            LOGGER.warning("[" + client + "] DELETE \"" + filename + "\" ERRO: arquivo não encontrado");
            sendHeader(TCPServerQ2.CMD_DELETE, TCPServerQ2.STATUS_ERROR);
        }
        out.flush();
    }

    /**
     * GETFILESLIST (3):
     *   cabeçalho (3 bytes) + 2 bytes big-endian (qtd) +
     *   para cada arquivo: 1 byte (tam nome) + bytes do nome
     */
    private void handleGetFilesList(String client) throws IOException {
        File dir = new File(TCPServerQ2.storagePath());
        String[] files = dir.list((d, n) -> new File(d, n).isFile());
        if (files == null) files = new String[0];

        sendHeader(TCPServerQ2.CMD_GETFILESLIST, TCPServerQ2.STATUS_SUCCESS);
        out.writeShort(files.length); // 2 bytes big-endian
        for (String name : files) {
            byte[] nameBytes = name.getBytes();
            out.writeByte(nameBytes.length); // 1 byte
            out.write(nameBytes);
        }
        out.flush();
        LOGGER.info("[" + client + "] GETFILESLIST -> " + files.length + " arquivo(s)");
    }

    /**
     * GETFILE (4):
     *   cabeçalho (3 bytes) + 4 bytes big-endian (tam) + bytes do arquivo
     */
    private void handleGetFile(String client, String filename) throws IOException {
        Path src = Paths.get(TCPServerQ2.storagePath(), sanitize(filename));

        if (!Files.exists(src)) {
            LOGGER.warning("[" + client + "] GETFILE \"" + filename + "\" ERRO: não encontrado");
            sendHeader(TCPServerQ2.CMD_GETFILE, TCPServerQ2.STATUS_ERROR);
            out.flush();
            return;
        }

        byte[] data = Files.readAllBytes(src);
        sendHeader(TCPServerQ2.CMD_GETFILE, TCPServerQ2.STATUS_SUCCESS);
        out.writeInt(data.length); // 4 bytes big-endian

        // Envio byte a byte conforme enunciado
        for (byte b : data) out.writeByte(b);

        out.flush();
        LOGGER.info("[" + client + "] GETFILE \"" + filename + "\" (" + data.length + " bytes) enviado");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sendHeader(byte cmd, byte status) throws IOException {
        out.writeByte(TCPServerQ2.MSG_RESPONSE);
        out.writeByte(cmd);
        out.writeByte(status);
    }

    /** Evita path traversal (ex: "../etc/passwd"). */
    private String sanitize(String filename) {
        return Paths.get(filename).getFileName().toString();
    }

    private void close() {
        try {
            if (in  != null) in.close();
            if (out != null) out.close();
            socket.close();
        } catch (IOException e) {
            LOGGER.warning("Erro ao fechar socket: " + e.getMessage());
        }
    }
}
