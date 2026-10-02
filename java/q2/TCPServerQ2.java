import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

/**
 * Descrição: Servidor TCP multithreaded em Java para o protocolo binário da Questão 2.
 *            Escuta na porta 9090 e atende requisições concorrentes de gerenciamento
 *            de arquivos (ADDFILE, DELETE, GETFILESLIST, GETFILE) formatadas em Big-Endian.
 *            Registra auditorias e operações via java.util.logging em console e arquivo.
 * Autores: Daniel Suzuki Naves e Pedro Borges De Araujo
 * Data de criação: 25/09/2026
 * Última atualização: 01/10/2026
 */
public class TCPServerQ2 {

    private static final int    SERVER_PORT  = 9090;
    private static final String STORAGE_DIR  = "storage";
    private static final Logger LOGGER       = Logger.getLogger("TCPServerQ2");

    /**
     * Método principal que inicializa o logger, cria o diretório de armazenamento local
     * e entra no loop infinito aceitando conexões de clientes TCP na porta 9090.
     *
     * @param args Argumentos de linha de comando (não utilizados).
     * @throws IOException Caso ocorra falha de E/S na inicialização do ServerSocket ou diretórios.
     */
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

    /**
     * Configura o sistema de logs para direcionar mensagens de auditoria
     * simultaneamente para a saída padrão (Console) e para o arquivo de log 'server_q2.log'.
     *
     * @throws IOException Caso ocorra falha ao criar ou acessar o arquivo 'server_q2.log'.
     */
    private static void setupLogger() throws IOException {
        Logger root = Logger.getLogger("");
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

    // ── Constantes do Protocolo Binário ──────────────────────────────────────────
    static final byte MSG_REQUEST  = 0x01;
    static final byte MSG_RESPONSE = 0x02;

    static final byte CMD_ADDFILE      = 0x01;
    static final byte CMD_DELETE       = 0x02;
    static final byte CMD_GETFILESLIST = 0x03;
    static final byte CMD_GETFILE      = 0x04;

    static final byte STATUS_SUCCESS = 0x01;
    static final byte STATUS_ERROR   = 0x02;

    /**
     * Retorna o caminho relativo do diretório de armazenamento do servidor.
     *
     * @return String representando a pasta 'storage'.
     */
    static String storagePath() { return STORAGE_DIR; }
}

/**
 * Thread responsável por tratar a conexão TCP individual de um cliente,
 * efetuando o parsing do protocolo binário e a execução das operações de arquivo.
 */
class ClientHandlerQ2 extends Thread {

    private static final Logger LOGGER = Logger.getLogger("TCPServerQ2");

    private final Socket         socket;
    private DataInputStream      in;
    private DataOutputStream     out;

    /**
     * Construtor da Thread de atendimento ao cliente.
     *
     * @param socket Socket TCP da conexão ativa com o cliente.
     */
    ClientHandlerQ2(Socket socket) {
        this.socket = socket;
    }

    /**
     * Execução da Thread: processa requisições binárias contínuas enquanto a conexão
     * permanecer aberta, efetuando o despacho para os métodos de cada comando.
     */
    @Override
    public void run() {
        String client = socket.getRemoteSocketAddress().toString();
        try {
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            while (true) {
                // Lê o byte inicial de tipo de mensagem (1 byte)
                byte msgType = in.readByte();
                if (msgType != TCPServerQ2.MSG_REQUEST) {
                    LOGGER.warning("[" + client + "] Tipo de mensagem inválido: " + msgType);
                    break;
                }

                byte cmdId       = in.readByte();
                byte nameSize    = in.readByte();
                byte[] nameBytes = new byte[nameSize & 0xFF]; // conversão para int sem sinal
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

    /**
     * Trata o comando ADDFILE (0x01): lê o tamanho do arquivo (4 bytes Big-Endian)
     * e recebe o payload em blocos de até 4KB, salvando no diretório do servidor.
     *
     * @param client Endereço remoto do cliente para fins de log.
     * @param filename Nome do arquivo enviado pelo cliente.
     * @throws IOException Lançada em caso de falha na leitura do socket ou escrita no disco.
     */
    private void handleAddFile(String client, String filename) throws IOException {
        int fileSize = in.readInt();
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

    /**
     * Trata o comando DELETE (0x02): remove do disco o arquivo informado pelo cliente.
     *
     * @param client Endereço remoto do cliente para fins de log.
     * @param filename Nome do arquivo a ser removido.
     * @throws IOException Lançada em caso de erro na operação do sistema de arquivos.
     */
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
     * Trata o comando GETFILESLIST (0x03): envia o número total de arquivos (2 bytes Big-Endian)
     * seguido de cada nome de arquivo pré-formatado com seu tamanho de nome (1 byte).
     *
     * @param client Endereço remoto do cliente para fins de log.
     * @throws IOException Lançada em caso de falha no envio dos dados de rede.
     */
    private void handleGetFilesList(String client) throws IOException {
        File dir = new File(TCPServerQ2.storagePath());
        String[] files = dir.list((d, n) -> new File(d, n).isFile());
        if (files == null) files = new String[0]; // Instanciação de vetor com tamanho 0

        sendHeader(TCPServerQ2.CMD_GETFILESLIST, TCPServerQ2.STATUS_SUCCESS);
        out.writeShort(files.length); // 2 bytes Big-Endian
        for (String name : files) {
            byte[] nameBytes = name.getBytes();
            out.writeByte(nameBytes.length); // 1 byte tamanho do nome
            out.write(nameBytes);
        }
        out.flush();
        LOGGER.info("[" + client + "] GETFILESLIST -> " + files.length + " arquivo(s)");
    }

    /**
     * Trata o comando GETFILE (0x04): transmite o tamanho do arquivo (4 bytes Big-Endian)
     * e envia byte a byte todo o conteúdo do arquivo localizado no servidor.
     *
     * @param client Endereço remoto do cliente para fins de log.
     * @param filename Nome do arquivo a ser transmitido.
     * @throws IOException Lançada em caso de falha de leitura no disco ou escrita no socket.
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
        out.writeInt(data.length); // 4 bytes Big-Endian

        // Envio byte a byte conforme especificação
        for (byte b : data) out.writeByte(b);

        out.flush();
        LOGGER.info("[" + client + "] GETFILE \"" + filename + "\" (" + data.length + " bytes) enviado");
    }

    /**
     * Envia os 3 bytes do cabeçalho de resposta da aplicação (MSG_RESPONSE, CMD, STATUS).
     *
     * @param cmd Código do comando que gerou a resposta.
     * @param status Indicador de sucesso (0x01) ou erro (0x02).
     * @throws IOException Lançada em caso de falha ao escrever no socket.
     */
    private void sendHeader(byte cmd, byte status) throws IOException {
        out.writeByte(TCPServerQ2.MSG_RESPONSE);
        out.writeByte(cmd);
        out.writeByte(status);
    }

    /**
     * Sanitiza o nome do arquivo recebido para evitar vulnerabilidades de navegação
     * indesejada no sistema de arquivos (Path Traversal).
     *
     * @param filename Nome bruto ou caminho recebido da rede.
     * @return String contendo estritamente o nome base do arquivo.
     */
    private String sanitize(String filename) {
        return Paths.get(filename).getFileName().toString();
    }

    /**
     * Fecha com segurança os fluxos de entrada/saída e o socket associado ao cliente.
     */
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
