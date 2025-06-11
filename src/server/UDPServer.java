package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import common.ProtocolMessage;
import common.ClientInfo;
import common.MessageUtils;

public class UDPServer extends JFrame {
    private DatagramSocket socket;
    private boolean running;
    private int port;
    private Map<String, ClientInfo> connectedClients;
    private Map<String, ClientInfo> authenticatedUsers;
    private Map<String, User> userDatabase;
    private Map<String, Topic> topicDatabase;
    private AtomicInteger nextTopicId; // Gerador de IDs sequenciais para tópicos
    private AtomicInteger nextTokenId; // Gerador de IDs sequenciais para tokens

    private DefaultListModel<ClientInfo> listModel;
    private JList<ClientInfo> clientList;
    private JTextArea logArea;

    public UDPServer() {
        connectedClients = new ConcurrentHashMap<>();
        authenticatedUsers = new ConcurrentHashMap<>();
        userDatabase = new ConcurrentHashMap<>();
        topicDatabase = new ConcurrentHashMap<>();
        nextTopicId = new AtomicInteger(1);
        nextTokenId = new AtomicInteger(1);

        // Usuarios de teste
        userDatabase.put("admin", new User("admin", "adminpass", "Administrator", "admin"));
        userDatabase.put("user1", new User("user1", "user1pass", "User One", "common"));
        userDatabase.put("user2", new User("user2", "user2pass", "User Two", "common"));

        topicDatabase.put("1", new Topic("1", "Bem-vindos ao Fórum", "Introdução", "Olá a todos! Este é o primeiro tópico do nosso fórum.", "admin"));
        topicDatabase.put("2", new Topic("2", "Dicas de Programação Java", "Desenvolvimento", "Compartilhe suas melhores dicas e truques de Java aqui!", "user1"));

        initializeGUI();
        askForPort();
        startServer();
    }

    private void initializeGUI() {
        setTitle("UDP Server (Forum)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(700, 500); // Tamanho aumentado para melhor layout
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        listModel = new DefaultListModel<>();
        clientList = new JList<>(listModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        clientScrollPane.setBorder(BorderFactory.createTitledBorder("Connected & Authenticated Clients"));
        clientScrollPane.setPreferredSize(new Dimension(250, 0));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Server Log"));

        mainPanel.add(clientScrollPane, BorderLayout.WEST);
        mainPanel.add(logScrollPane, BorderLayout.CENTER);

        add(mainPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer(); // Garante que o socket seja fechado ao sair
                System.exit(0);
            }
        });

        setVisible(true);
    }

    private void askForPort() {
        String portStr = JOptionPane.showInputDialog(this, "Enter server port:", "Server Port", JOptionPane.QUESTION_MESSAGE);
        try {
            port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) { // Portas válidas para aplicações
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number. Using default port 12345.", "Warning", JOptionPane.WARNING_MESSAGE);
            port = 12345;
        }
    }

    private void startServer() {
        try {
            socket = new DatagramSocket(port); // Cria o socket UDP na porta especificada
            running = true;
            logMessage("Server started on port " + port);

            // Inicia um novo thread para escutar mensagens UDP, para não bloquear a GUI
            Thread serverThread = new Thread(this::serverLoop);
            serverThread.setDaemon(true); // Define como daemon para que o thread termine com a aplicação
            serverThread.start();

        } catch (SocketException e) {
            logMessage("Error starting server: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error starting server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void serverLoop() {
        byte[] buffer = new byte[8192]; // Buffer para receber dados do pacote

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Bloqueia até que um pacote seja recebido

                // Desserializa a mensagem usando GSON
                ProtocolMessage message = MessageUtils.deserializeMessage(packet);
                // Manipula a mensagem, passando a mensagem desserializada e o endereço/porta do remetente
                handleMessage(message, packet.getAddress(), packet.getPort());

            } catch (IOException e) {
                // Se o servidor ainda estiver rodando, registra o erro de IO
                if (running) {
                    logMessage("Error receiving message: " + e.getMessage());
                }
            } catch (Exception e) {
                // Captura quaisquer outros erros durante a desserialização ou processamento
                logMessage("Error processing message: " + e.getMessage());
            }
        }
    }

    private void handleMessage(ProtocolMessage message, InetAddress clientAddress, int clientPort) throws IOException {
        String opCode = message.getOperationCode();
        String clientEndpointKey = clientAddress.getHostAddress() + ":" + clientPort;

        logMessage("Received message from " + clientEndpointKey + " with op: " + opCode + " and payload: " + new String(MessageUtils.serializeMessage(message)));

        ClientInfo currentClient = connectedClients.computeIfAbsent(clientEndpointKey, k ->
                new ClientInfo("Guest", clientAddress, clientPort));
        switch (opCode) {
            case "000": // Login
                handleLogin(message, currentClient);
                break;
            case "005": // Retornar Dados de Usuário (opcode 005)
                handleRetrieveUserData(message, currentClient);
                break;
            case "010": // Cadastrar
                handleRegister(message, currentClient);
                break;
            case "020": // Logout
                handleLogout(message, currentClient);
                break;
            case "030": // Alterar Cadastro (Próprio)
                handleChangeProfile(message, currentClient);
                break;
            case "040": // Apagar Cadastro (Próprio)
                handleDeleteAccount(message, currentClient);
                break;
            case "050": // Enviar Mensagem (Criar Tópico)
                handleCreateTopic(message, currentClient);
                break;
            // Opcodes 060 e 070 (listar tópicos e usuários) não estão mais explicitamente
            // implementados aqui, conforme a mudança de foco para 005/006/007.
            // Se precisar deles futuramente, será necessário reintroduzir a lógica.
            default:
                // Responde com um erro se o código de operação for desconhecido
                sendErrorMessage("999", "Unknown operation code: " + opCode, clientAddress, clientPort);
                logMessage("Unknown operation code received: " + opCode + " from " + clientEndpointKey);
        }
    }

    private void handleLogin(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String pass = request.getPassword();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        logMessage("Attempting login for user: '" + user + "' from " + address.getHostAddress() + ":" + port);

        // Validação de entrada
        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            sendErrorMessage("002", "User or password cannot be null/empty.", address, port);
            logMessage("Login failed for " + address.getHostAddress() + ":" + port + ": User or password null/empty.");
            return;
        }
        if (!userDatabase.containsKey(user)) {
            sendErrorMessage("002", "User '" + user + "' does not exist.", address, port);
            logMessage("Login failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": User not found.");
            return;
        }

        User storedUser = userDatabase.get(user);
        if (!storedUser.getPassword().equals(pass)) {
            sendErrorMessage("002", "Incorrect password for user '" + user + "'.", address, port);
            logMessage("Login failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": Incorrect password.");
            return;
        }

        // Gera token e autentica o cliente
        // O token é 'a' para admin, 'c' para comum, seguido de um ID sequencial
        String token = storedUser.getRole().substring(0, 1) + String.format("%04d", nextTokenId.getAndIncrement());
        clientInfo.setUserId(user);
        clientInfo.setName(storedUser.getNickname()); // Define o apelido como nome de exibição
        clientInfo.setToken(token);
        authenticatedUsers.put(token, clientInfo); // Adiciona à lista de usuários autenticados

        // Atualiza a lista de clientes na GUI (executado na EDT)
        SwingUtilities.invokeLater(() -> {
            if (!listModel.contains(clientInfo)) {
                listModel.addElement(clientInfo);
            } else {
                // Se o cliente já estava na lista (ex: reconexão), atualiza a exibição
                listModel.setElementAt(clientInfo, listModel.indexOf(clientInfo));
            }
            logMessage("Client '" + user + "' logged in with token: " + token + " from " + address.getHostAddress() + ":" + port + ". Nickname: " + storedUser.getNickname());
        });

        // Envia resposta de sucesso ao cliente
        ProtocolMessage response = new ProtocolMessage("001");
        response.setToken(token);
        response.setUser(user); // Inclui o usuário na resposta de sucesso
        sendMessage(response, address, port); // Este método agora imprimirá o objeto completo
    }

    private void handleRetrieveUserData(ProtocolMessage request, ClientInfo clientInfo) {
        String requestedUser = request.getUser(); // O usuário cujos dados são solicitados
        String token = request.getToken(); // O token do cliente que faz a requisição
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        logMessage("Attempting to retrieve data for user: '" + requestedUser + "' by client token '" + token + "' from " + address.getHostAddress() + ":" + port);

        // Validação: Token e usuário solicitado não podem ser nulos/vazios
        if (token == null || token.isEmpty() || requestedUser == null || requestedUser.isEmpty()) {
            sendErrorMessage("007", "User or token cannot be null/empty for data retrieval.", address, port);
            logMessage("User data retrieval failed for " + address.getHostAddress() + ":" + port + ": User or token null/empty in request.");
            return;
        }

        // Validação: Verifica se o token é válido e o cliente está autenticado
        ClientInfo authenticatingClient = authenticatedUsers.get(token);
        if (authenticatingClient == null || !authenticatingClient.getUserId().equals(clientInfo.getUserId())) {
            // Se o token não for válido ou não corresponder ao cliente que enviou a requisição
            sendErrorMessage("007", "Invalid token or token does not match requesting client for data retrieval.", address, port);
            logMessage("User data retrieval failed for " + requestedUser + " from " + address.getHostAddress() + ":" + port + ": Invalid or mismatched token for requesting client.");
            return;
        }

        // Recupera os dados do usuário do banco de dados
        User storedUser = userDatabase.get(requestedUser);
        if (storedUser == null) {
            sendErrorMessage("007", "User '" + requestedUser + "' not found.", address, port);
            logMessage("User data retrieval failed for '" + requestedUser + "' from " + address.getHostAddress() + ":" + port + ": Requested user not found in database.");
            return;
        }

        // Envia resposta de sucesso (006) com o username e nickname do usuário encontrado
        ProtocolMessage response = new ProtocolMessage("006");
        response.setUser(storedUser.getUsername()); // Retorna o username do usuário encontrado
        response.setNickname(storedUser.getNickname()); // Retorna o nickname do usuário encontrado
        sendMessage(response, address, port);
        logMessage("Sent 006 response with data for user '" + requestedUser + "' (Nickname: " + storedUser.getNickname() + ") to " + address.getHostAddress() + ":" + port);
    }

    private void handleRegister(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String nick = request.getNickname();
        String pass = request.getPassword();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        logMessage("Attempting registration for user: '" + user + "', nickname: '" + nick + "' from " + address.getHostAddress() + ":" + port);

        // Validação de entrada
        if (user == null || user.isEmpty() || nick == null || nick.isEmpty() || pass == null || pass.isEmpty()) {
            sendErrorMessage("012", "User, nickname, or password cannot be null/empty.", address, port);
            logMessage("Registration failed for " + address.getHostAddress() + ":" + port + ": User, nickname or password null/empty.");
            return;
        }
        if (userDatabase.containsKey(user)) {
            sendErrorMessage("012", "User '" + user + "' already exists.", address, port);
            logMessage("Registration failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": User already exists.");
            return;
        }

        // Validação de formato (conforme o protocolo)
        if (user.length() < 6 || user.length() > 16 || !user.matches("[a-zA-Z0-9]+")) {
            sendErrorMessage("012", "Username must be 6-16 alphanumeric characters.", address, port);
            logMessage("Registration failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": Invalid username format.");
            return;
        }
        if (pass.length() < 6 || pass.length() > 32 || !pass.matches("[a-zA-Z0-9]+")) {
            sendErrorMessage("012", "Password must be 6-32 alphanumeric characters.", address, port);
            logMessage("Registration failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": Invalid password format.");
            return;
        }
        if (nick.length() < 6 || nick.length() > 16 || !nick.matches("[a-zA-Z0-9]+")) {
            sendErrorMessage("012", "Nickname must be 6-16 alphanumeric characters.", address, port);
            logMessage("Registration failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": Invalid nickname format.");
            return;
        }

        User newUser = new User(user, pass, nick, "common"); // Novo usuário com papel "common"
        userDatabase.put(user, newUser); // Adiciona ao "banco de dados" de usuários

        logMessage("New user registered: '" + user + "' (" + nick + ") from " + address.getHostAddress() + ":" + port);

        // Envia resposta de sucesso
        ProtocolMessage response = new ProtocolMessage("011");
        sendMessage(response, address, port); // Este método agora imprimirá o objeto completo
    }

    private void handleLogout(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String token = request.getToken();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        logMessage("Attempting logout for user: '" + user + "' with token: '" + token + "' from " + address.getHostAddress() + ":" + port);


        // Validação de entrada
        if (user == null || user.isEmpty() || token == null || token.isEmpty()) {
            sendErrorMessage("022", "User or token cannot be null/empty.", address, port);
            logMessage("Logout failed for " + address.getHostAddress() + ":" + port + ": User or token null/empty.");
            return;
        }

        // Verifica se o token é válido e pertence ao usuário
        if (!authenticatedUsers.containsKey(token) || !authenticatedUsers.get(token).getUserId().equals(user)) {
            sendErrorMessage("022", "Invalid token or token does not match user.", address, port);
            logMessage("Logout failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": Invalid or mismatched token.");
            return;
        }

        authenticatedUsers.remove(token); // Remove da lista de autenticados
        clientInfo.setUserId(null); // Limpa as informações de autenticação do ClientInfo
        clientInfo.setToken(null);
        clientInfo.setName("Guest"); // Reseta o nome de exibição

        // Remove da lista da GUI (executado na EDT)
        SwingUtilities.invokeLater(() -> {
            listModel.removeElement(clientInfo);
            logMessage("Client '" + user + "' logged out. Token: " + token + " from " + address.getHostAddress() + ":" + port);
        });

        // Envia resposta de sucesso
        ProtocolMessage response = new ProtocolMessage("021");
        sendMessage(response, address, port); // Este método agora imprimirá o objeto completo
    }

    private void handleChangeProfile(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String pass = request.getPassword(); // Senha atual para verificação
        String newNick = request.getNewNickname();
        String newPass = request.getNewPassword();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        logMessage("Attempting profile change for user: '" + user + "' from " + address.getHostAddress() + ":" + port + ". New Nick: " + newNick + ", New Pass Provided: " + (newPass != null && !newPass.isEmpty()));

        // Validação de entrada
        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            sendErrorMessage("032", "User or current password cannot be null/empty.", address, port);
            logMessage("Profile change failed for " + address.getHostAddress() + ":" + port + ": User or current password null/empty.");
            return;
        }
        if (!userDatabase.containsKey(user)) {
            sendErrorMessage("032", "User '" + user + "' does not exist.", address, port);
            logMessage("Profile change failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": User not found.");
            return;
        }

        User storedUser = userDatabase.get(user);
        if (!storedUser.getPassword().equals(pass)) {
            sendErrorMessage("032", "Incorrect current password for user '" + user + "'.", address, port);
            logMessage("Profile change failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": Incorrect current password.");
            return;
        }

        boolean changed = false;
        String oldNick = storedUser.getNickname();
        // Atualiza apelido se fornecido e válido
        if (newNick != null && !newNick.isEmpty()) {
            if (newNick.length() < 6 || newNick.length() > 16 || !newNick.matches("[a-zA-Z0-9]+")) {
                sendErrorMessage("032", "New nickname must be 6-16 alphanumeric characters.", address, port);
                logMessage("Profile change failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": Invalid new nickname format.");
                return;
            }
            if (!oldNick.equals(newNick)) {
                storedUser.setNickname(newNick);
                clientInfo.setName(newNick); // Atualiza o nome de exibição no ClientInfo
                changed = true;
                logMessage("User '" + user + "' changed nickname from '" + oldNick + "' to '" + newNick + "'.");
            }
        }
        // Atualiza senha se fornecida e válida
        if (newPass != null && !newPass.isEmpty()) {
            if (newPass.length() < 6 || newPass.length() > 32 || !newPass.matches("[a-zA-Z0-9]+")) {
                sendErrorMessage("032", "New password must be 6-32 alphanumeric characters.", address, port);
                logMessage("Profile change failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": Invalid new password format.");
                return;
            }
            if (!storedUser.getPassword().equals(newPass)) { // Check if new password is different
                storedUser.setPassword(newPass);
                changed = true;
                logMessage("User '" + user + "' changed password.");
            }
        }

        if (changed) {
            logMessage("User '" + user + "' profile updated successfully.");
            // Força a atualização visual da lista de clientes para refletir a mudança de apelido
            SwingUtilities.invokeLater(() -> clientList.repaint());
        } else {
            logMessage("User '" + user + "' sent profile update request but no changes were made to nickname or password.");
        }

        // Envia resposta de sucesso
        ProtocolMessage response = new ProtocolMessage("031");
        sendMessage(response, address, port); // Este método agora imprimirá o objeto completo
    }

    private void handleDeleteAccount(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String token = request.getToken();
        String pass = request.getPassword();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        logMessage("Attempting account deletion for user: '" + user + "' with token: '" + token + "' from " + address.getHostAddress() + ":" + port);

        // Validação de entrada
        if (user == null || user.isEmpty() || token == null || token.isEmpty() || pass == null || pass.isEmpty()) {
            sendErrorMessage("042", "User, token, or password cannot be null/empty.", address, port);
            logMessage("Account deletion failed for " + address.getHostAddress() + ":" + port + ": User or token null/empty.");
            return;
        }

        // Verifica se o token é válido e pertence ao usuário
        if (!authenticatedUsers.containsKey(token) || !authenticatedUsers.get(token).getUserId().equals(user)) {
            sendErrorMessage("042", "Invalid token or token does not match user.", address, port);
            logMessage("Account deletion failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": Invalid or mismatched token.");
            return;
        }

        User storedUser = userDatabase.get(user);
        if (storedUser == null || !storedUser.getPassword().equals(pass)) {
            sendErrorMessage("042", "Incorrect password or user does not exist.", address, port);
            logMessage("Account deletion failed for user '" + user + "' from " + address.getHostAddress() + ":" + port + ": Incorrect password or user not found.");
            return;
        }

        userDatabase.remove(user); // Remove do "banco de dados" de usuários
        authenticatedUsers.remove(token); // Remove da lista de autenticados
        clientInfo.setUserId(null); // Limpa as informações de autenticação do ClientInfo
        clientInfo.setToken(null);
        clientInfo.setName("Guest"); // Reseta o nome de exibição

        // Remove da lista da GUI (executado na EDT)
        SwingUtilities.invokeLater(() -> {
            listModel.removeElement(clientInfo);
            logMessage("User account '" + user + "' deleted from " + address.getHostAddress() + ":" + port);
        });

        // Envia resposta de sucesso
        ProtocolMessage response = new ProtocolMessage("041");
        sendMessage(response, address, port); // Este método agora imprimirá o objeto completo
    }

    private void handleCreateTopic(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String title = request.getTitle();
        String subject = request.getSubject();
        String msgContent = request.getMessageContent();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        logMessage("Attempting to create topic by client: '" + clientInfo.getName() + "' (ID: " + clientInfo.getUserId() + ") from " + address.getHostAddress() + ":" + port + ". Title: '" + title + "'");

        // Validação de entrada
        if (token == null || token.isEmpty() || title == null || title.isEmpty() ||
                subject == null || subject.isEmpty() || msgContent == null || msgContent.isEmpty()) {
            sendErrorMessage("052", "Token, title, subject, or message cannot be null/empty.", address, port);
            logMessage("Topic creation failed for " + address.getHostAddress() + ":" + port + ": Missing token, title, subject, or message content.");
            return;
        }

        // Verifica se o token é válido e o cliente está autenticado
        ClientInfo authClient = authenticatedUsers.get(token);
        if (authClient == null || !authClient.getUserId().equals(clientInfo.getUserId())) {
            sendErrorMessage("052", "Invalid or expired token.", address, port);
            logMessage("Topic creation failed for " + address.getHostAddress() + ":" + port + ": Invalid or expired token for user '" + clientInfo.getUserId() + "'.");
            return;
        }

        String topicId = String.valueOf(nextTopicId.getAndIncrement()); // Gera ID sequencial para o tópico
        Topic newTopic = new Topic(topicId, title, subject, msgContent, authClient.getUserId());
        topicDatabase.put(topicId, newTopic); // Armazena o tópico

        logMessage("New topic created by " + authClient.getUserId() + ": '" + title + "' (ID: " + topicId + ") from " + address.getHostAddress() + ":" + port);

        // Envia resposta de sucesso para o cliente que criou o tópico
        ProtocolMessage response = new ProtocolMessage("051");
        sendMessage(response, address, port); // Este método agora imprimirá o objeto completo

        ProtocolMessage broadcastTopicMsg = new ProtocolMessage("055"); // Novo código de operação para broadcast de tópico
        broadcastTopicMsg.setTopicId(newTopic.getId()); // Exemplo: Se ProtocolMessage tem setTopicId
        broadcastTopicMsg.setTopicTitle(newTopic.getTitle());
        broadcastTopicMsg.setTopicSubject(newTopic.getSubject());
        broadcastTopicMsg.setTopicContent(newTopic.getContent());
        broadcastTopicMsg.setTopicAuthor(newTopic.getAuthorUserId());

        logMessage("Broadcasting new topic '" + newTopic.getTitle() + "' (ID: " + newTopic.getId() + ") to all authenticated clients.");
        for (ClientInfo client : authenticatedUsers.values()) {
            sendMessage(broadcastTopicMsg, client.getAddress(), client.getPort()); // Comentar se ProtocolMessage não tem os campos para broadcast
            logMessage("  - Broadcasted (attempted) to " + client.getAddress().getHostAddress() + ":" + client.getPort() + " (User: " + client.getUserId() + ")");
        }
    }

    private void sendMessage(ProtocolMessage message, InetAddress address, int port) {
        try {
            // Serializa a mensagem para bytes JSON
            byte[] data = MessageUtils.serializeMessage(message);

            // --- AQUI É ONDE O OBJETO ProtocolMessage É IMPRESSO NO CONSOLE DO SERVIDOR ---
            // Converte os bytes serializados para uma String antes de imprimir
            System.out.println("SERVER DEBUG - Sending " + message.getOperationCode() + " to " + address.getHostAddress() + ":" + port + ": " + new String(data));
            // -----------------------------------------------------------------------------

            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet); // Envia o pacote
            logMessage("Sent message op '" + message.getOperationCode() + "' to " + address.getHostAddress() + ":" + port + " (Size: " + data.length + " bytes).");
        } catch (IOException e) {
            logMessage("Error sending message op '" + message.getOperationCode() + "' to " + address.getHostAddress() + ":" + port + ": " + e.getMessage());
        }
    }

    private void sendErrorMessage(String opCode, String errorMsg, InetAddress address, int port) {
        ProtocolMessage message = ProtocolMessage.createErrorMessage(opCode, errorMsg);
        sendMessage(message, address, port); // Este método agora imprimirá o objeto completo
        logMessage("Sent error " + opCode + ": '" + errorMsg + "' to " + address.getHostAddress() + ":" + port);
    }

    private void logMessage(String message) {
        String logEntry = "[" + new java.util.Date() + "] " + message;
        System.out.println(logEntry);
        SwingUtilities.invokeLater(() -> {
            logArea.append(logEntry + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void stopServer() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Fecha o socket, liberando a porta
        }
        logMessage("Server stopped");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UDPServer());
    }

    private static class User {
        private String username;
        private String password;
        private String nickname;
        private String role; // "admin" ou "common"

        public User(String username, String password, String nickname, String role) {
            this.username = username;
            this.password = password;
            this.nickname = nickname;
            this.role = role;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getNickname() { return nickname; }
        public String getRole() { return role; }

        public void setPassword(String password) { this.password = password; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    private static class Topic {
        private String id;
        private String title;
        private String subject;
        private String content;
        private String authorUserId; // O ID de usuário do autor do tópico

        public Topic(String id, String title, String subject, String content, String authorUserId) {
            this.id = id;
            this.title = title;
            this.subject = subject;
            this.content = content;
            this.authorUserId = authorUserId;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getSubject() { return subject; }
        public String getContent() { return content; }
        public String getAuthorUserId() { return authorUserId; }
    }
}