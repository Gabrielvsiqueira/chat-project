package server;

import common.ClientInfo;
import common.ProtocolMessage;
import common.SerializationHelper;
import server.service.AdminHandler;
import server.service.AuthHandler;
import server.service.ProfileHandler;
import server.service.TopicHandler;
import server.service.UserDataHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {
    // --- CAMPOS DA CLASSE (VARIÁVEIS DE INSTÂNCIA) ---
    private final Socket clientSocket;
    private ClientInfo clientInfo;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Consumer<String> logConsumer;
    private final Consumer<ClientInfo> clientListUpdater;
    private final Consumer<ClientHandler> clientDisconnectedCallback;

    private final AuthHandler authHandler;
    private final ProfileHandler profileHandler;
    private final TopicHandler topicHandler;
    private final UserDataHandler userDataHandler;
    private final AdminHandler adminHandler;
    private final Map<String, PrintWriter> activeClientOutputs; // Corrigido para PrintWriter

    private volatile boolean running = true;

    // --- CONSTRUTOR ---
    public ClientHandler(Socket clientSocket,
                         Consumer<String> logConsumer,
                         Consumer<ClientInfo> clientListUpdater,
                         Consumer<ClientHandler> clientDisconnectedCallback,
                         AuthHandler authHandler,
                         ProfileHandler profileHandler,
                         TopicHandler topicHandler,
                         UserDataHandler userDataHandler,
                         AdminHandler adminHandler,
                         Map<String, PrintWriter> activeClientOutputs) throws IOException {

        // Atribui todos os parâmetros recebidos para os campos da classe
        this.clientSocket = clientSocket;
        this.logConsumer = logConsumer;
        this.clientListUpdater = clientListUpdater;
        this.clientDisconnectedCallback = clientDisconnectedCallback;
        this.authHandler = authHandler;
        this.profileHandler = profileHandler;
        this.topicHandler = topicHandler;
        this.userDataHandler = userDataHandler;
        this.adminHandler = adminHandler;
        this.activeClientOutputs = activeClientOutputs;

        // Inicializa os fluxos de I/O baseados em texto
        this.out = new PrintWriter(this.clientSocket.getOutputStream(), true); // 'true' para auto-flush
        this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

        // Cria a informação inicial do cliente como "Convidado"
        this.clientInfo = new ClientInfo("Guest", this.clientSocket.getInetAddress(), this.clientSocket.getPort());
        logMessageWithClientContext("New client connected: " + this.clientInfo.getAddress().getHostAddress() + ":" + this.clientInfo.getPort());

        // Atualiza a GUI do servidor com o novo cliente
        this.clientListUpdater.accept(this.clientInfo);
    }

    public ClientInfo getClientInfo() {
        return this.clientInfo;
    }

    private void logMessageWithClientContext(String message) {
        String clientContext = (this.clientInfo.getUserId() != null && !this.clientInfo.getUserId().isEmpty())
                ? this.clientInfo.getUserId() + " (" + this.clientInfo.getName() + ")"
                : this.clientInfo.getAddress().getHostAddress() + ":" + this.clientInfo.getPort();
        this.logConsumer.accept("[CLIENT: " + clientContext + "] " + message);
    }

    @Override
    public void run() {
        try {
            while (this.running) {
                ProtocolMessage request = SerializationHelper.readMessage(this.in);
                if (request == null) {
                    logMessageWithClientContext("Client disconnected gracefully (stream closed).");
                    break; // Sai do loop para fechar a conexão
                }

                logMessageWithClientContext("Received op: " + request.getOperationCode() + " -> " + request.toString());

                if ("000".equals(request.getOperationCode())) {
                    ProtocolMessage loginResponse = this.authHandler.handleLogin(request, this.clientInfo);
                    if ("001".equals(loginResponse.getOperationCode())) {
                        this.activeClientOutputs.put(this.clientInfo.getToken(), this.out);
                        logMessageWithClientContext("Login successful. Token: " + this.clientInfo.getToken());
                    } else {
                        logMessageWithClientContext("Login failed. Response: " + loginResponse.getMessageContent());
                    }
                    SerializationHelper.writeMessage(loginResponse, this.out);
                    continue;
                }

                ProtocolMessage response = processMessage(request);
                if (response != null) {
                    SerializationHelper.writeMessage(response, this.out);
                    logMessageWithClientContext("Sent response op: " + response.getOperationCode() + " -> " + response.toString());
                }
            }
        } catch (SocketException e) {
            logMessageWithClientContext("Disconnected (SocketException): " + e.getMessage());
        } catch (java.io.EOFException e) {
            logMessageWithClientContext("Disconnected gracefully (EOFException).");
        } catch (IOException e) {
            logMessageWithClientContext("I/O error: " + e.getMessage());
        } catch (Exception e) {
            logMessageWithClientContext("Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private ProtocolMessage processMessage(ProtocolMessage request) {
        String opCode = request.getOperationCode();

        switch (opCode) {
            case "005": return this.userDataHandler.handleRetrieveUserData(request, this.clientInfo);
            case "010": return this.authHandler.handleRegister(request, this.clientInfo);
            case "020":
                if (this.clientInfo.getToken() != null) {
                    this.activeClientOutputs.remove(this.clientInfo.getToken());
                }
                return this.authHandler.handleLogout(request, this.clientInfo);
            case "030": return this.profileHandler.handleChangeProfile(request, this.clientInfo);
            case "040":
                if (this.clientInfo.getToken() != null) {
                    this.activeClientOutputs.remove(this.clientInfo.getToken());
                }
                return this.profileHandler.handleDeleteAccount(request, this.clientInfo);
            case "050": return this.topicHandler.handleCreateTopic(request, this.clientInfo);
            case "060": return this.topicHandler.handleReplyMessage(request, this.clientInfo);
            case "070": return this.topicHandler.handleGetReplies(request, this.clientInfo);
            case "075": return this.topicHandler.handleGetTopics(request, this.clientInfo);
            case "080": return this.adminHandler.handleChangeUserByAdmin(request, this.clientInfo);
            case "090": return this.adminHandler.handleDeleteUserByAdmin(request, this.clientInfo);
            case "100": return this.adminHandler.handleDeleteMessage(request, this.clientInfo);
            case "110": return this.adminHandler.handleListAllUsers(request, this.clientInfo);
            case "999": return ProtocolMessage.createErrorMessage("999", "Client-side error received: " + request.getMessageContent());
            default:
                logMessageWithClientContext("Unknown operation code: " + opCode);
                return ProtocolMessage.createErrorMessage("999", "Unknown operation code: " + opCode);
        }
    }

    public void stop() {
        this.running = false;
        try {
            if (this.clientSocket != null && !this.clientSocket.isClosed()) {
                this.clientSocket.close();
            }
        } catch (IOException e) {
            logMessageWithClientContext("Error closing client socket: " + e.getMessage());
        }
    }

    private void closeConnection() {
        this.running = false;
        try {
            if (this.out != null) this.out.close();
            if (this.in != null) this.in.close();
            if (this.clientSocket != null && !this.clientSocket.isClosed()) {
                this.clientSocket.close();
            }
        } catch (IOException e) {
            logMessageWithClientContext("Error closing streams/socket: " + e.getMessage());
        } finally {
            if (this.clientInfo.getToken() != null) {
                this.activeClientOutputs.remove(this.clientInfo.getToken());
            } else {
                this.activeClientOutputs.remove(this.clientInfo.getAddress().getHostAddress() + ":" + this.clientInfo.getPort());
            }
            this.clientDisconnectedCallback.accept(this);
            logMessageWithClientContext("Disconnected.");
        }
    }
}