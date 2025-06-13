package client;

import common.ProtocolMessage;
import common.SerializationHelper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Gerencia a conexão TCP do cliente com o servidor, incluindo envio e recebimento de mensagens.
 */
public class ClientConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listenerThread;
    private Consumer<ProtocolMessage> messageHandler; // Callback para a GUI principal
    private Consumer<String> logConsumer; // Callback para o log da GUI
    private BlockingQueue<ProtocolMessage> outgoingQueue; // Fila para mensagens de saída

    private volatile boolean connected = false;

    public ClientConnection(Consumer<ProtocolMessage> messageHandler, Consumer<String> logConsumer) {
        this.messageHandler = messageHandler;
        this.logConsumer = logConsumer;
        this.outgoingQueue = new LinkedBlockingQueue<>();
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /**
     * Tenta conectar ao servidor TCP.
     * @param host Endereço do servidor.
     * @param port Porta do servidor.
     * @throws IOException Se a conexão falhar.
     * @throws UnknownHostException Se o host não for encontrado.
     */
    public void connect(String host, int port) throws IOException, UnknownHostException {
        if (connected) {
            logConsumer.accept("Already connected. Disconnecting first.");
            disconnect();
        }
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;
            logConsumer.accept("Connected to server: " + host + ":" + port);

            // Inicia o thread de escuta
            listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true); // Thread encerra com a aplicação principal
            listenerThread.start();

            // Inicia o thread de envio
            Thread senderThread = new Thread(this::sendMessages);
            senderThread.setDaemon(true);
            senderThread.start();

        } catch (IOException e) {
            logConsumer.accept("Failed to connect: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Desconecta do servidor.
     */
    public void disconnect() {
        if (!connected) {
            logConsumer.accept("Not connected.");
            return;
        }
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close(); // Isso deve interromper as operações de I/O nos threads
            }
            if (out != null) out.close();
            if (in != null) in.close();
            logConsumer.accept("Disconnected from server.");
        } catch (IOException e) {
            logConsumer.accept("Error during disconnection: " + e.getMessage());
        } finally {
            socket = null;
            out = null;
            in = null;
        }
    }

    /**
     * Adiciona uma mensagem à fila de envio.
     * @param message A ProtocolMessage a ser enviada.
     */
    public void sendMessage(ProtocolMessage message) {
        if (!connected) {
            logConsumer.accept("Cannot send message: not connected to server.");
            return;
        }
        try {
            outgoingQueue.put(message); // Adiciona a mensagem à fila de saída
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logConsumer.accept("Failed to queue message for sending: " + e.getMessage());
        }
    }

    /**
     * Thread para escutar mensagens recebidas do servidor.
     */
    private void listenForMessages() {
        while (connected) {
            try {
                ProtocolMessage message = SerializationHelper.readMessage(in);
                messageHandler.accept(message); // Envia a mensagem para o manipulador da GUI
            } catch (ClassNotFoundException e) {
                logConsumer.accept("Error: Class not found during deserialization: " + e.getMessage());
            } catch (IOException e) {
                if (connected) { // Se a desconexão não foi intencional
                    logConsumer.accept("Server disconnected or IO error: " + e.getMessage());
                    disconnect(); // Força a desconexão do cliente
                }
            } catch (Exception e) {
                logConsumer.accept("Unexpected error while listening: " + e.getMessage());
            }
        }
        logConsumer.accept("Listener thread stopped.");
    }

    /**
     * Thread para enviar mensagens da fila de saída para o servidor.
     */
    private void sendMessages() {
        while (connected) {
            try {
                ProtocolMessage message = outgoingQueue.take(); // Pega a próxima mensagem da fila (bloqueia se estiver vazia)
                SerializationHelper.writeMessage(message, out);
                logConsumer.accept("Sent " + message.getOperationCode() + " request.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logConsumer.accept("Sender thread interrupted.");
                break;
            } catch (IOException e) {
                logConsumer.accept("Error sending message: " + e.getMessage());
                disconnect(); // Se houver erro de envio, desconecta
                break;
            }
        }
        logConsumer.accept("Sender thread stopped.");
    }
}