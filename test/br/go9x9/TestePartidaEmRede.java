package br.go9x9;

import br.go9x9.servidor.Servidor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static br.go9x9.Verificador.igual;
import static br.go9x9.Verificador.secao;
import static br.go9x9.Verificador.verdadeiro;

public final class TestePartidaEmRede {
    private static final int TIMEOUT_MS = 5000;

    private TestePartidaEmRede() {
    }

    private static final class ClienteCru implements Closeable {
        private final Socket socket;
        private final BufferedReader entrada;
        private final BufferedWriter saida;

        ClienteCru(int porta) throws IOException {
            socket = new Socket(InetAddress.getLoopbackAddress(), porta);
            socket.setSoTimeout(TIMEOUT_MS);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void enviar(String linha) throws IOException {
            saida.write(linha + "\n");
            saida.flush();
        }

        String ler() throws IOException {
            return entrada.readLine();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    public static void executar() throws Exception {
        partidaCompleta();
        abandono();
    }

    private static Thread iniciarServidor(ServerSocket socketDoServidor) {
        Thread thread = new Thread(() -> {
            try {
                new Servidor(socketDoServidor).executar();
            } catch (IOException | InterruptedException e) {
                System.err.println("Servidor de teste falhou: " + e);
            }
        }, "servidor-de-teste");
        thread.start();
        return thread;
    }

    private static void partidaCompleta() throws Exception {
        secao("Partida completa pela rede");
        try (ServerSocket socketDoServidor = new ServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
            int porta = socketDoServidor.getLocalPort();
            Thread servidor = iniciarServidor(socketDoServidor);

            try (ClienteCru preto = new ClienteCru(porta)) {
                igual("preto recebe seu número", "JOGADOR:1", preto.ler());
                igual("preto espera o adversário", "ESPERA", preto.ler());

                try (ClienteCru branco = new ClienteCru(porta)) {
                    igual("branco recebe seu número", "JOGADOR:2", branco.ler());
                    igual("preto é avisado do início", "INICIO:1", preto.ler());
                    igual("branco é avisado do início", "INICIO:1", branco.ler());

                    branco.enviar("2JA1");
                    igual("branco fora da vez recebe erro", "ERRO:nao_e_sua_vez", branco.ler());

                    preto.enviar("2JA1");
                    igual("preto se passando pelo jogador 2 recebe erro", "ERRO:jogador_invalido", preto.ler());

                    preto.enviar("lixo");
                    igual("linha ilegível recebe erro", "ERRO:mensagem_invalida", preto.ler());

                    preto.enviar("1JE5");
                    igual("preto vê a jogada confirmada", "JOGADA:1:E5:-:2", preto.ler());
                    igual("branco vê a mesma jogada", "JOGADA:1:E5:-:2", branco.ler());

                    branco.enviar("2JE5");
                    igual("posição ocupada só avisa quem errou", "ERRO:ocupada", branco.ler());

                    branco.enviar("2JD5");
                    igual("preto vê a jogada do branco", "JOGADA:2:D5:-:1", preto.ler());
                    igual("branco vê a própria jogada", "JOGADA:2:D5:-:1", branco.ler());

                    preto.enviar("1P");
                    igual("preto passa (visto pelo preto)", "PASSE:1:2", preto.ler());
                    igual("preto passa (visto pelo branco)", "PASSE:1:2", branco.ler());

                    branco.enviar("2P");
                    igual("branco passa", "PASSE:2:1", preto.ler());
                    igual("branco passa (visto pelo branco)", "PASSE:2:1", branco.ler());

                    igual("fim por dois passes (preto)", "FIM:PASSES:1.0:6.5:2", preto.ler());
                    igual("fim por dois passes (branco)", "FIM:PASSES:1.0:6.5:2", branco.ler());
                }
            }
            servidor.join(TIMEOUT_MS);
            verdadeiro("o servidor termina sozinho depois do fim da partida", !servidor.isAlive());
        }
    }

    private static void abandono() throws Exception {
        secao("Adversário desconecta no meio da partida");
        try (ServerSocket socketDoServidor = new ServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
            int porta = socketDoServidor.getLocalPort();
            Thread servidor = iniciarServidor(socketDoServidor);

            try (ClienteCru preto = new ClienteCru(porta)) {
                preto.ler();
                preto.ler();
                ClienteCru branco = new ClienteCru(porta);
                branco.ler();
                preto.ler();
                branco.ler();
                branco.close();
                igual("quem ficou é avisado do abandono", "ADVERSARIO_SAIU", preto.ler());
            }
            servidor.join(TIMEOUT_MS);
            verdadeiro("o servidor termina depois do abandono", !servidor.isAlive());
        }
    }
}
