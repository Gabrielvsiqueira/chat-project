package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import common.ProtocolMessage;
import common.ClientInfo;
import common.MessageUtils;

/**
 * Servidor UDP que gerencia conexões de clientes, autenticação e operações de fórum
 * de acordo com um protocolo JSON definido.
 * Utiliza GSON para serialização/desserialização de mensagens.
 */
public class UDPServer extends JFrame {
    private DatagramSocket socket;
    private boolean running;
    private int port;
    // Mapeia chaves de endpoint (IP:Porta) para ClientInfo, rastreando todos os clientes que enviaram uma mensagem.
    private Map<String, ClientInfo> connectedClients;
    // Mapeia tokens de autenticação para ClientInfo, rastreando usuários logados.
    private Map<String, ClientInfo> authenticatedUsers;
    // Simulação de banco de dados de usuários em memória (username -> User object).
    private Map<String, User> userDatabase;
    // Simulação de banco de dados de tópicos de fórum em memória (topicId -> Topic object).
    private Map<String, Topic> topicDatabase;
    private AtomicInteger nextTopicId; // Gerador de IDs sequenciais para tópicos
    private AtomicInteger nextTokenId; // Gerador de IDs sequenciais para tokens

    // Componentes da GUI
    private DefaultListModel<ClientInfo> listModel;
    private JList<ClientInfo> clientList;
    private JTextArea logArea;

    /**
     * Construtor do servidor UDP. Inicializa estruturas de dados, GUI e solicita a porta.
     */
    public UDPServer() {
        connectedClients = new ConcurrentHashMap<>();
        authenticatedUsers = new ConcurrentHashMap<>();
        userDatabase = new ConcurrentHashMap<>();
        topicDatabase = new ConcurrentHashMap<>();
        nextTopicId = new AtomicInteger(1);
        nextTokenId = new AtomicInteger(1);

        // Adiciona alguns usuários de teste para facilitar o desenvolvimento
        userDatabase.put("admin", new User("admin", "adminpass", "Administrator", "admin"));
        userDatabase.put("user1", new User("user1", "user1pass", "User One", "common"));
        userDatabase.put("user2", new User("user2", "user2pass", "User Two", "common"));

        initializeGUI();
        askForPort();
        startServer();
    }

    /**
     * Inicializa os componentes da interface gráfica do servidor.
     */
    private void initializeGUI() {
        setTitle("UDP Server (Forum)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(700, 500); // Tamanho aumentado para melhor layout
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Painel da lista de clientes conectados e autenticados
        listModel = new DefaultListModel<>();
        clientList = new JList<>(listModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        clientScrollPane.setBorder(BorderFactory.createTitledBorder("Connected & Authenticated Clients"));
        clientScrollPane.setPreferredSize(new Dimension(250, 0));

        // Área de log do servidor
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Server Log"));

        mainPanel.add(clientScrollPane, BorderLayout.WEST);
        mainPanel.add(logScrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // Listener para o evento de fechamento da janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer(); // Garante que o socket seja fechado ao sair
                System.exit(0);
            }
        });

        setVisible(true);
    }

    /**
     * Solicita ao usuário a porta em que o servidor deve escutar.
     * Usa a porta 12345 como padrão se a entrada for inválida.
     */
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

    /**
     * Inicia o servidor UDP, criando o DatagramSocket e o thread de escuta.
     */
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

    /**
     * Loop principal do servidor para receber pacotes UDP.
     * Desserializa os pacotes para ProtocolMessage e os encaminha para o manipulador.
     */
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

    /**
     * Manipula as mensagens recebidas de acordo com o código de operação do protocolo.
     *
     * @param message       A mensagem do protocolo recebida.
     * @param clientAddress O endereço IP do cliente remetente.
     * @param clientPort    A porta do cliente remetente.
     */
    private void handleMessage(ProtocolMessage message, InetAddress clientAddress, int clientPort) {
        String opCode = message.getOperationCode();
        String clientEndpointKey = clientAddress.getHostAddress() + ":" + clientPort;

        logMessage("Received message from " + clientEndpointKey + " with op: " + opCode);

        // Obtém ou cria um ClientInfo para este endpoint.
        // Isso garante que cada endpoint de rede tenha um ClientInfo associado.
        ClientInfo currentClient = connectedClients.computeIfAbsent(clientEndpointKey, k ->
                new ClientInfo("Guest", clientAddress, clientPort));

        // Direciona a mensagem para o manipulador específico com base no código de operação
        switch (opCode) {
            case "000": // Login
                handleLogin(message, currentClient);
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
            default:
                // Responde com um erro se o código de operação for desconhecido
                sendErrorMessage("999", "Unknown operation code: " + opCode, clientAddress, clientPort);
                logMessage("Unknown operation code received: " + opCode + " from " + clientEndpointKey);
        }
    }

    // --- Métodos de Manipulação do Protocolo ---

    /**
     * Manipula a operação de Login (000).
     * Valida credenciais, gera token e autentica o cliente.
     *
     * @param request    A mensagem de requisição de login.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     */
    private void handleLogin(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String pass = request.getPassword();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        // Validação de entrada
        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            sendErrorMessage("002", "User or password cannot be null/empty.", address, port);
            return;
        }
        if (!userDatabase.containsKey(user)) {
            sendErrorMessage("002", "User '" + user + "' does not exist.", address, port);
            return;
        }

        User storedUser = userDatabase.get(user);
        if (!storedUser.getPassword().equals(pass)) {
            sendErrorMessage("002", "Incorrect password for user '" + user + "'.", address, port);
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
            logMessage("Client '" + user + "' logged in with token: " + token + " from " + address.getHostAddress() + ":" + port);
        });

        // Envia resposta de sucesso ao cliente
        ProtocolMessage response = new ProtocolMessage("001");
        response.setToken(token);
        response.setUser(user); // Inclui o usuário na resposta de sucesso
        sendMessage(response, address, port);
    }

    /**
     * Manipula a operação de Cadastro (010).
     * Registra um novo usuário no sistema.
     *
     * @param request    A mensagem de requisição de cadastro.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     */
    private void handleRegister(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String nick = request.getNickname();
        String pass = request.getPassword();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        // Validação de entrada
        if (user == null || user.isEmpty() || nick == null || nick.isEmpty() || pass == null || pass.isEmpty()) {
            sendErrorMessage("012", "User, nickname, or password cannot be null/empty.", address, port);
            return;
        }
        if (userDatabase.containsKey(user)) {
            sendErrorMessage("012", "User '" + user + "' already exists.", address, port);
            return;
        }

        // Validação de formato (conforme o protocolo)
        if (user.length() < 6 || user.length() > 16 || !user.matches("[a-zA-Z0-9]+")) {
            sendErrorMessage("012", "Username must be 6-16 alphanumeric characters.", address, port);
            return;
        }
        if (pass.length() < 6 || pass.length() > 32 || !pass.matches("[a-zA-Z0-9]+")) {
            sendErrorMessage("012", "Password must be 6-32 alphanumeric characters.", address, port);
            return;
        }
        if (nick.length() < 6 || nick.length() > 16 || !nick.matches("[a-zA-Z0-9]+")) {
            sendErrorMessage("012", "Nickname must be 6-16 alphanumeric characters.", address, port);
            return;
        }

        User newUser = new User(user, pass, nick, "common"); // Novo usuário com papel "common"
        userDatabase.put(user, newUser); // Adiciona ao "banco de dados" de usuários

        logMessage("New user registered: " + user + " (" + nick + ")");

        // Envia resposta de sucesso
        ProtocolMessage response = new ProtocolMessage("011");
        sendMessage(response, address, port);
    }

    /**
     * Manipula a operação de Logout (020).
     * Remove o token de autenticação e desautentica o cliente.
     *
     * @param request    A mensagem de requisição de logout.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     */
    private void handleLogout(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String token = request.getToken();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        // Validação de entrada
        if (user == null || user.isEmpty() || token == null || token.isEmpty()) {
            sendErrorMessage("022", "User or token cannot be null/empty.", address, port);
            return;
        }

        // Verifica se o token é válido e pertence ao usuário
        if (!authenticatedUsers.containsKey(token) || !authenticatedUsers.get(token).getUserId().equals(user)) {
            sendErrorMessage("022", "Invalid token or token does not match user.", address, port);
            return;
        }

        authenticatedUsers.remove(token); // Remove da lista de autenticados
        clientInfo.setUserId(null); // Limpa as informações de autenticação do ClientInfo
        clientInfo.setToken(null);
        clientInfo.setName("Guest"); // Reseta o nome de exibição

        // Remove da lista da GUI (executado na EDT)
        SwingUtilities.invokeLater(() -> {
            listModel.removeElement(clientInfo);
            logMessage("Client '" + user + "' logged out. Token: " + token);
        });

        // Envia resposta de sucesso
        ProtocolMessage response = new ProtocolMessage("021");
        sendMessage(response, address, port);
    }

    /**
     * Manipula a operação de Alterar Cadastro (Próprio) (030).
     * Permite que um usuário autenticado altere seu apelido ou senha.
     *
     * @param request    A mensagem de requisição de alteração de perfil.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     */
    private void handleChangeProfile(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String pass = request.getPassword(); // Senha atual para verificação
        String newNick = request.getNewNickname();
        String newPass = request.getNewPassword();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        // Validação de entrada
        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            sendErrorMessage("032", "User or current password cannot be null/empty.", address, port);
            return;
        }
        if (!userDatabase.containsKey(user)) {
            sendErrorMessage("032", "User '" + user + "' does not exist.", address, port);
            return;
        }

        User storedUser = userDatabase.get(user);
        if (!storedUser.getPassword().equals(pass)) {
            sendErrorMessage("032", "Incorrect current password for user '" + user + "'.", address, port);
            return;
        }

        boolean changed = false;
        // Atualiza apelido se fornecido e válido
        if (newNick != null && !newNick.isEmpty()) {
            if (newNick.length() < 6 || newNick.length() > 16 || !newNick.matches("[a-zA-Z0-9]+")) {
                sendErrorMessage("032", "New nickname must be 6-16 alphanumeric characters.", address, port);
                return;
            }
            storedUser.setNickname(newNick);
            clientInfo.setName(newNick); // Atualiza o nome de exibição no ClientInfo
            changed = true;
        }
        // Atualiza senha se fornecida e válida
        if (newPass != null && !newPass.isEmpty()) {
            if (newPass.length() < 6 || newPass.length() > 32 || !newPass.matches("[a-zA-Z0-9]+")) {
                sendErrorMessage("032", "New password must be 6-32 alphanumeric characters.", address, port);
                return;
            }
            storedUser.setPassword(newPass);
            changed = true;
        }

        if (changed) {
            logMessage("User '" + user + "' profile updated.");
            // Força a atualização visual da lista de clientes para refletir a mudança de apelido
            SwingUtilities.invokeLater(() -> clientList.repaint());
        } else {
            logMessage("User '" + user + "' sent profile update request but no changes were made.");
        }

        // Envia resposta de sucesso
        ProtocolMessage response = new ProtocolMessage("031");
        sendMessage(response, address, port);
    }

    /**
     * Manipula a operação de Apagar Cadastro (Próprio) (040).
     * Exclui a conta de um usuário.
     *
     * @param request    A mensagem de requisição de exclusão de conta.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     */
    private void handleDeleteAccount(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String token = request.getToken();
        String pass = request.getPassword();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        // Validação de entrada
        if (user == null || user.isEmpty() || token == null || token.isEmpty() || pass == null || pass.isEmpty()) {
            sendErrorMessage("042", "User, token, or password cannot be null/empty.", address, port);
            return;
        }

        // Verifica se o token é válido e pertence ao usuário
        if (!authenticatedUsers.containsKey(token) || !authenticatedUsers.get(token).getUserId().equals(user)) {
            sendErrorMessage("042", "Invalid token or token does not match user.", address, port);
            return;
        }

        User storedUser = userDatabase.get(user);
        if (storedUser == null || !storedUser.getPassword().equals(pass)) {
            sendErrorMessage("042", "Incorrect password or user does not exist.", address, port);
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
            logMessage("User account '" + user + "' deleted.");
        });

        // Envia resposta de sucesso
        ProtocolMessage response = new ProtocolMessage("041");
        sendMessage(response, address, port);
    }

    /**
     * Manipula a operação de Enviar Mensagem (Criar Tópico) (050).
     * Cria um novo tópico no fórum e o transmite para todos os clientes autenticados.
     *
     * @param request    A mensagem de requisição de criação de tópico.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     */
    private void handleCreateTopic(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String title = request.getTitle();
        String subject = request.getSubject();
        String msgContent = request.getMessageContent();
        InetAddress address = clientInfo.getAddress();
        int port = clientInfo.getPort();

        // Validação de entrada
        if (token == null || token.isEmpty() || title == null || title.isEmpty() ||
                subject == null || subject.isEmpty() || msgContent == null || msgContent.isEmpty()) {
            sendErrorMessage("052", "Token, title, subject, or message cannot be null/empty.", address, port);
            return;
        }

        // Verifica se o token é válido e o cliente está autenticado
        ClientInfo authClient = authenticatedUsers.get(token);
        if (authClient == null || !authClient.getUserId().equals(clientInfo.getUserId())) {
            sendErrorMessage("052", "Invalid or expired token.", address, port);
            return;
        }

        // Validação de formato (comprimento) para título, assunto, mensagem
        // O protocolo indica "???" para comprimento, então adicione validações conforme necessário.
        // Exemplo:
        // if (title.length() > 255 || subject.length() > 255 || msgContent.length() > 1024) {
        //     sendErrorMessage("052", "Title, subject, or message content exceeds max length.", address, port);
        //     return;
        // }

        String topicId = String.valueOf(nextTopicId.getAndIncrement()); // Gera ID sequencial para o tópico
        Topic newTopic = new Topic(topicId, title, subject, msgContent, authClient.getUserId());
        topicDatabase.put(topicId, newTopic); // Armazena o tópico

        logMessage("New topic created by " + authClient.getUserId() + ": '" + title + "' (ID: " + topicId + ")");

        // Envia resposta de sucesso para o cliente que criou o tópico
        ProtocolMessage response = new ProtocolMessage("051");
        sendMessage(response, address, port);

        // --- NOVO: Transmite o novo tópico para todos os clientes autenticados ---
        ProtocolMessage broadcastTopicMsg = new ProtocolMessage("055"); // Novo código de operação para broadcast de tópico
        broadcastTopicMsg.setTopicId(newTopic.getId());
        broadcastTopicMsg.setTopicTitle(newTopic.getTitle());
        broadcastTopicMsg.setTopicSubject(newTopic.getSubject());
        broadcastTopicMsg.setTopicContent(newTopic.getContent());
        broadcastTopicMsg.setTopicAuthor(newTopic.getAuthorUserId());

        for (ClientInfo client : authenticatedUsers.values()) {
            // Não envia a mensagem de broadcast de volta para o próprio remetente,
            // pois ele já recebeu a confirmação 051 e pode exibir seu próprio tópico.
            // No entanto, para um fórum, pode-se querer que o remetente também veja
            // a mensagem broadcastada para ter uma experiência consistente.
            // Por enquanto, vamos enviar para todos, incluindo o remetente.
            sendMessage(broadcastTopicMsg, client.getAddress(), client.getPort());
        }
        logMessage("Broadcasted new topic '" + newTopic.getTitle() + "' (ID: " + newTopic.getId() + ") to all authenticated clients.");
    }

    // --- Métodos Auxiliares ---

    /**
     * Envia uma mensagem do protocolo para um endereço IP e porta específicos.
     *
     * @param message O objeto ProtocolMessage a ser enviado.
     * @param address O endereço IP de destino.
     * @param port    A porta de destino.
     */
    private void sendMessage(ProtocolMessage message, InetAddress address, int port) {
        try {
            byte[] data = MessageUtils.serializeMessage(message); // Serializa a mensagem para bytes JSON
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet); // Envia o pacote
        } catch (IOException e) {
            logMessage("Error sending message to " + address.getHostAddress() + ":" + port + ": " + e.getMessage());
        }
    }

    /**
     * Envia uma mensagem de erro padronizada do protocolo para um cliente.
     *
     * @param opCode   O código de operação do erro.
     * @param errorMsg A mensagem de erro.
     * @param address  O endereço IP do cliente.
     * @param port     A porta do cliente.
     */
    private void sendErrorMessage(String opCode, String errorMsg, InetAddress address, int port) {
        ProtocolMessage message = ProtocolMessage.createErrorMessage(opCode, errorMsg);
        sendMessage(message, address, port);
        logMessage("Sent error " + opCode + ": '" + errorMsg + "' to " + address.getHostAddress() + ":" + port);
    }

    /**
     * Adiciona uma mensagem à área de log da GUI do servidor.
     * Garante que a atualização da GUI seja feita na Event Dispatch Thread (EDT).
     *
     * @param message A mensagem a ser logada.
     */
    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.util.Date() + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // Rola para o final
        });
    }

    /**
     * Para o servidor, fechando o DatagramSocket e interrompendo o loop de escuta.
     */
    private void stopServer() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Fecha o socket, liberando a porta
        }
        logMessage("Server stopped");
    }

    /**
     * Método principal para iniciar o servidor.
     * Garante que a inicialização da GUI seja feita na Event Dispatch Thread (EDT).
     *
     * @param args Argumentos da linha de comando (não utilizados).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UDPServer());
    }

    // --- Classes de Dados Internas para o Servidor (Simulação de DB) ---

    /**
     * Classe interna que representa um usuário registrado no sistema.
     * Usada para simular um banco de dados de usuários em memória.
     */
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

    /**
     * Classe interna que representa um tópico de fórum.
     * Usada para simular um banco de dados de tópicos em memória.
     */
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