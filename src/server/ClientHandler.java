package server;

import common.ClientInfo;
import common.ProtocolMessage;
import common.SerializationHelper;
import server.service.AdminHandler;
import server.service.AuthHandler;
import server.service.ProfileHandler;
import server.service.TopicHandler;
import server.service.UserDataHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private ClientInfo clientInfo;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final Consumer<String> logConsumer;
    private final Consumer<ClientInfo> clientListUpdater;
    private final Consumer<ClientHandler> clientDisconnectedCallback;

    private final AuthHandler authHandler;
    private final ProfileHandler profileHandler;
    private final TopicHandler topicHandler;
    private final UserDataHandler userDataHandler;
    private final AdminHandler adminHandler;
    private final Map<String, ObjectOutputStream> activeClientOutputs;

    private volatile boolean running = true;

    public ClientHandler(Socket clientSocket,
                         Consumer<String> logConsumer,
                         Consumer<ClientInfo> clientListUpdater,
                         Consumer<ClientHandler> clientDisconnectedCallback,
                         AuthHandler authHandler,
                         ProfileHandler profileHandler,
                         TopicHandler topicHandler,
                         UserDataHandler userDataHandler,
                         AdminHandler adminHandler,
                         Map<String, ObjectOutputStream> activeClientOutputs) throws IOException {
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

        this.out = new ObjectOutputStream(clientSocket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(clientSocket.getInputStream());

        this.clientInfo = new ClientInfo("Guest", clientSocket.getInetAddress(), clientSocket.getPort());
        this.activeClientOutputs.put(clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort(), out);
        logMessageWithClientContext("New client connected: " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        clientListUpdater.accept(clientInfo);
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    private void logMessageWithClientContext(String message) {
        String clientContext = clientInfo.getUserId() != null && !clientInfo.getUserId().isEmpty()
                ? clientInfo.getUserId() + " (" + clientInfo.getName() + ")"
                : clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort();
        logConsumer.accept("[CLIENT: " + clientContext + "] " + message);
    }

    @Override
    public void run() {
        try {
            while (running) {
                ProtocolMessage request = SerializationHelper.readMessage(in);
                logMessageWithClientContext("Received op: " + request.getOperationCode() + " -> " + request.toString());

                if ("000".equals(request.getOperationCode())) {
                    ProtocolMessage loginResponse = authHandler.handleLogin(request, clientInfo);
                    if ("001".equals(loginResponse.getOperationCode())) {
                        String oldKey = clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort();
                        activeClientOutputs.remove(oldKey);
                        activeClientOutputs.put(clientInfo.getToken(), out);
                        logMessageWithClientContext("Login successful. Token: " + clientInfo.getToken());
                    } else {
                        logMessageWithClientContext("Login failed. Response: " + loginResponse.getMessageContent());
                    }
                    SerializationHelper.writeMessage(loginResponse, out);
                    continue;
                }

                ProtocolMessage response = processMessage(request);
                if (response != null) {
                    SerializationHelper.writeMessage(response, out);
                    logMessageWithClientContext("Sent response op: " + response.getOperationCode() + " -> " + response.toString());
                }
            }
        } catch (SocketException e) {
            logMessageWithClientContext("Disconnected (SocketException): " + e.getMessage());
        } catch (java.io.EOFException e) {
            logMessageWithClientContext("Disconnected gracefully (EOFException).");
        } catch (IOException e) {
            logMessageWithClientContext("I/O error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            logMessageWithClientContext("Error: Class not found during deserialization: " + e.getMessage());
            e.printStackTrace();
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
            case "005": return userDataHandler.handleRetrieveUserData(request, clientInfo);
            case "010": return authHandler.handleRegister(request, clientInfo);
            case "020":
                if (clientInfo.getToken() != null) {
                    activeClientOutputs.remove(clientInfo.getToken());
                }
                return authHandler.handleLogout(request, clientInfo);
            case "030": return profileHandler.handleChangeProfile(request, clientInfo);
            case "040":
                if (clientInfo.getToken() != null) {
                    activeClientOutputs.remove(clientInfo.getToken());
                }
                return profileHandler.handleDeleteAccount(request, clientInfo);
            case "050": return topicHandler.handleCreateTopic(request, clientInfo);
            case "060": return topicHandler.handleReplyMessage(request, clientInfo);
            case "070": return topicHandler.handleGetReplies(request, clientInfo);
            case "075": return topicHandler.handleGetTopics(request, clientInfo);
            case "080": return adminHandler.handleChangeUserByAdmin(request, clientInfo);
            case "090": return adminHandler.handleDeleteUserByAdmin(request, clientInfo);
            case "100": return adminHandler.handleDeleteMessage(request, clientInfo);
            case "110": return adminHandler.handleListAllUsers(request, clientInfo); // Handle new 110 opcode
            case "999": return ProtocolMessage.createErrorMessage("999", "Client-side error received: " + request.getMessageContent()); // Handle client-side errors
            default:
                logMessageWithClientContext("Unknown operation code: " + opCode);
                return ProtocolMessage.createErrorMessage("999", "Unknown operation code: " + opCode);
        }
    }

    public void stop() {
        this.running = false;
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logMessageWithClientContext("Error closing client socket: " + e.getMessage());
        }
    }

    private void closeConnection() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logMessageWithClientContext("Error closing streams/socket: " + e.getMessage());
        } finally {
            if (clientInfo.getToken() != null) {
                activeClientOutputs.remove(clientInfo.getToken());
            } else {
                activeClientOutputs.remove(clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());
            }
            clientDisconnectedCallback.accept(this);
            logMessageWithClientContext("Disconnected.");
        }
    }
}