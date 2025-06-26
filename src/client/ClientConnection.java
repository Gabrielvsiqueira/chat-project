package client;

import common.ProtocolMessage;
import common.SerializationHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class ClientConnection {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private final Consumer<ProtocolMessage> messageHandler;
    private final Consumer<String> logConsumer;
    private final BlockingQueue<ProtocolMessage> outgoingQueue;

    private volatile boolean connected = false;

    public ClientConnection(Consumer<ProtocolMessage> messageHandler, Consumer<String> logConsumer) {
        this.messageHandler = messageHandler;
        this.logConsumer = logConsumer;
        this.outgoingQueue = new LinkedBlockingQueue<>();
    }

    public boolean isConnected() {
        return this.connected && this.socket != null && !this.socket.isClosed();
    }

    public void connect(String host, int port) throws IOException, UnknownHostException {
        if (this.connected) {
            this.logConsumer.accept("Already connected. Disconnecting first.");
            this.disconnect();
        }
        try {
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.connected = true;
            this.logConsumer.accept("Connected to server: " + host + ":" + port);

            this.listenerThread = new Thread(this::listenForMessages);
            this.listenerThread.setDaemon(true);
            this.listenerThread.start();

            Thread senderThread = new Thread(this::sendMessages);
            senderThread.setDaemon(true);
            senderThread.start();

        } catch (IOException e) {
            this.logConsumer.accept("Failed to connect: " + e.getMessage());
            throw e;
        }
    }

    public void disconnect() {
        if (!this.connected) {
            this.logConsumer.accept("Not connected.");
            return;
        }
        this.connected = false;
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                this.socket.close();
            }
            if (this.out != null) this.out.close();
            if (this.in != null) this.in.close();
            this.logConsumer.accept("Disconnected from server.");
        } catch (IOException e) {
            this.logConsumer.accept("Error during disconnection: " + e.getMessage());
        } finally {
            this.socket = null;
            this.out = null;
            this.in = null;
        }
    }

    public void sendMessage(ProtocolMessage message) {
        if (!this.connected) {
            this.logConsumer.accept("Cannot send message: not connected to server.");
            return;
        }
        try {
            this.outgoingQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.logConsumer.accept("Failed to queue message for sending: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        while (this.connected) {
            try {
                ProtocolMessage message = SerializationHelper.readMessage(this.in);
                if (message == null) {
                    if (this.connected) {
                        this.logConsumer.accept("Server closed the connection.");
                        this.disconnect();
                    }
                    break;
                }
                this.messageHandler.accept(message);
            } catch (IOException e) {
                if (this.connected) {
                    this.logConsumer.accept("Server disconnected or IO error: " + e.getMessage());
                    this.disconnect();
                }
            } catch (Exception e) {
                if (this.connected) {
                    this.logConsumer.accept("Unexpected error while listening: " + e.getMessage());
                }
            }
        }
        this.logConsumer.accept("Listener thread stopped.");
    }

    private void sendMessages() {
        while (this.connected) {
            try {
                ProtocolMessage message = this.outgoingQueue.take();
                SerializationHelper.writeMessage(message, this.out);
                this.logConsumer.accept("Sent " + message.getOperationCode() + " request.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                this.logConsumer.accept("Sender thread interrupted.");
                break;
            } catch (IOException e) {
                if(this.connected) {
                    this.logConsumer.accept("Error sending message: " + e.getMessage());
                    this.disconnect();
                }
                break;
            }
        }
        this.logConsumer.accept("Sender thread stopped.");
    }
}