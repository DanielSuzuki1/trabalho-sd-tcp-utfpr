import java.net.*;
import java.io.*;
import java.security.*;
import java.util.Scanner;

/**
 * Questão 1 - Cliente TCP (protocolo textual com String UTF)
 */
public class TCPClientQ1 {

    private static final String SERVER_IP   = "127.0.0.1";
    private static final int    SERVER_PORT = 8080;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            DataInputStream  in  = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            System.out.println("Conectado ao servidor " + SERVER_IP + ":" + SERVER_PORT);
            printMenu();

            while (true) {
                System.out.print("\n> ");
                String line = scanner.nextLine().trim();

                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+", 2);
                String cmd = parts[0].toUpperCase();

                switch (cmd) {

                    case "CONNECT": {
                        System.out.print("Usuário: ");
                        String user = scanner.nextLine().trim();
                        System.out.print("Senha: ");
                        String pass = scanner.nextLine().trim();

                        String hashPass = sha512(pass);
                        String msg = "CONNECT " + user + ", " + hashPass;
                        out.writeUTF(msg);

                        String resp = in.readUTF();
                        System.out.println("Resposta: " + resp);
                        break;
                    }

                    case "PWD": {
                        out.writeUTF("PWD");
                        String resp = in.readUTF();
                        System.out.println("Diretório atual: " + resp);
                        break;
                    }

                    case "CHDIR": {
                        if (parts.length < 2 || parts[1].isBlank()) {
                            System.out.print("Caminho: ");
                            parts = new String[]{"CHDIR", scanner.nextLine().trim()};
                        }
                        out.writeUTF("CHDIR " + parts[1]);
                        String resp = in.readUTF();
                        System.out.println("Resposta: " + resp);
                        break;
                    }

                    case "GETFILES": {
                        out.writeUTF("GETFILES");
                        int count = Integer.parseInt(in.readUTF());
                        System.out.println("Arquivos (" + count + "):");
                        for (int i = 0; i < count; i++) {
                            System.out.println("  " + in.readUTF());
                        }
                        break;
                    }

                    case "GETDIRS": {
                        out.writeUTF("GETDIRS");
                        int count = Integer.parseInt(in.readUTF());
                        System.out.println("Diretórios (" + count + "):");
                        for (int i = 0; i < count; i++) {
                            System.out.println("  " + in.readUTF());
                        }
                        break;
                    }

                    case "EXIT": {
                        out.writeUTF("EXIT");
                        System.out.println("Encerrando conexão.");
                        return;
                    }

                    default:
                        System.out.println("Comando desconhecido. Digite HELP para ver os comandos.");
                        printMenu();
                }
            }

        } catch (EOFException e) {
            System.out.println("Servidor encerrou a conexão.");
        } catch (UnknownHostException e) {
            System.out.println("Host desconhecido: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erro de IO: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("SHA-512 não disponível: " + e.getMessage());
        }
    }

    /** Calcula SHA-512 e retorna como hex lowercase. */
    private static String sha512(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] bytes = md.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void printMenu() {
        System.out.println("\nComandos disponíveis:");
        System.out.println("  CONNECT          - Autenticar no servidor");
        System.out.println("  PWD              - Mostrar diretório atual");
        System.out.println("  CHDIR <caminho>  - Mudar de diretório");
        System.out.println("  GETFILES         - Listar arquivos do diretório atual");
        System.out.println("  GETDIRS          - Listar subdiretórios do diretório atual");
        System.out.println("  EXIT             - Encerrar conexão");
    }
}
