package br.go9x9.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Servidor do Go 9x9: aceita dois jogadores (o primeiro joga de Preto, o
 * segundo de Branco), cria uma thread para cada um e fica de árbitro até o
 * fim da partida.
 *
 * <p>Uso: {@code java br.go9x9.servidor.Servidor [porta]} (padrão 5000).</p>
 */
public final class Servidor {

    public static final int PORTA_PADRAO = 5000;
    private static final int JOGADORES = 2;

    private final ServerSocket serverSocket;

    public Servidor(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public static void main(String[] args) {
        int porta = PORTA_PADRAO;
        if (args.length > 0) {
            try {
                porta = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida: " + args[0]);
                System.exit(1);
            }
        }

        try (ServerSocket socketDoServidor = new ServerSocket(porta)) {
            System.out.println("[servidor] Go 9x9 aguardando jogadores na porta " + porta + "...");
            new Servidor(socketDoServidor).executar();
            System.out.println("[servidor] partida encerrada.");
        } catch (IOException e) {
            System.err.println("Falha no servidor: " + e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Aceita os dois jogadores e espera a partida terminar. */
    public void executar() throws IOException, InterruptedException {
        PartidaCompartilhada partida = new PartidaCompartilhada();
        List<Thread> threads = new ArrayList<>();

        for (int numero = 1; numero <= JOGADORES; numero++) {
            Socket socket = serverSocket.accept();
            System.out.println("[servidor] jogador " + numero + " conectou de " + socket.getRemoteSocketAddress());
            Thread thread = new Thread(new ConexaoJogador(numero, socket, partida), "jogador-" + numero);
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
