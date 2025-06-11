package client;

import common.ProtocolMessage;
import common.MessageUtils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPClient extends JFrame {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private String currentUsername; // O nome de usuário atualmente logado
    private String currentToken; // O token de autenticação recebido do servidor
    private AtomicBoolean connected = new AtomicBoolean(false); // Estado de autenticação com o servidor (logado)
    private AtomicBoolean running = new AtomicBoolean(false); // Estado do thread de escuta

    private JTextField serverHostField;
    private JTextField serverPortField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField nicknameField; // Para registro e atualização de perfil
    private JPasswordField newPasswordField; // Para atualização de perfil

    private JButton loginButton;
    private JButton registerButton;
    private JButton logoutButton;
    private JButton updateProfileButton;
    private JButton deleteAccountButton;

    private JTextField topicTitleField;
    private JTextField topicSubjectField;
    private JTextArea topicMessageArea;
    private JButton createTopicButton;

    private JTextField userIdToRetrieveField;
    private JButton retrieveUserDataButton; // Para opcode 005
    private JButton listTopicsButton; // Declarado aqui
    private JTextArea receivedMessagesArea; // Área para exibir mensagens recebidas e logs do cliente

    public UDPClient() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("UDP Client (Forum)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(700, 750); // Tamanho aumentado para acomodar mais painéis
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout()); // Usar BorderLayout para topPanel
        topPanel.add(createAuthPanel(), BorderLayout.NORTH); // Painel de autenticação em cima
        topPanel.add(createDataRetrievalPanel(), BorderLayout.CENTER); // Painel de retorno de dados no centro
        mainPanel.add(topPanel, BorderLayout.NORTH);

        mainPanel.add(createTopicPanel(), BorderLayout.CENTER); // Painel de tópico agora no centro

        // Painel Inferior (Mensagens Recebidas / Log do Cliente) -- Funcionalidade ainda não está funcionando para uso.
        JPanel receivedPanel = new JPanel(new BorderLayout());
        receivedPanel.setBorder(BorderFactory.createTitledBorder("Forum Messages /  Client Log"));

        receivedMessagesArea = new JTextArea(10, 0);
        receivedMessagesArea.setEditable(false);
        receivedMessagesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(receivedMessagesArea);
        receivedPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(receivedPanel, BorderLayout.SOUTH);

        add(mainPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connected.get()) { // Se estiver logado, tenta enviar logout
                    sendLogoutRequest();
                }
                // Garante que o socket do cliente seja fechado
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    appendReceivedMessage("Client socket closed.");
                }
                running.set(false); // Garante que o thread de escuta pare
                System.exit(0);
            }
        });

        updateGUIState();
        setVisible(true);
    }

    private JPanel createAuthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Server Connection & Authentication"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Espaçamento entre componentes
        gbc.anchor = GridBagConstraints.WEST; // Alinha componentes à esquerda

        // Campos de Host e Porta do Servidor
        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Server Host:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        serverHostField = new JTextField("localhost", 15); panel.add(serverHostField, gbc);

        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 3;
        serverPortField = new JTextField("12345", 8); panel.add(serverPortField, gbc);

        // Campos de Usuário e Senha para Login/Registro
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        usernameField = new JTextField(15); panel.add(usernameField, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 3;
        passwordField = new JPasswordField(15); panel.add(passwordField, gbc);

        // Campo de Apelido para Registro
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Nickname (Register):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        nicknameField = new JTextField(15); panel.add(nicknameField, gbc);

        // Campo de Nova Senha para Atualização de Perfil
        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel("New Password (Update):"), gbc);
        gbc.gridx = 3;
        newPasswordField = new JPasswordField(15); panel.add(newPasswordField, gbc);

        // Botões de Ação dos eventos
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        loginButton = new JButton("Login (000)");
        loginButton.addActionListener(e -> sendLoginRequest());
        panel.add(loginButton, gbc);

        gbc.gridx = 1;
        registerButton = new JButton("Register (010)");
        registerButton.addActionListener(e -> sendRegisterRequest());
        panel.add(registerButton, gbc);

        gbc.gridx = 2;
        logoutButton = new JButton("Logout (020)");
        logoutButton.addActionListener(e -> sendLogoutRequest());
        panel.add(logoutButton, gbc);

        gbc.gridx = 3;
        updateProfileButton = new JButton("Update Profile (030)");
        updateProfileButton.addActionListener(e -> sendUpdateProfileRequest());
        panel.add(updateProfileButton, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4;
        deleteAccountButton = new JButton("Delete Account (040)");
        deleteAccountButton.addActionListener(e -> sendDeleteAccountRequest());
        panel.add(deleteAccountButton, gbc);

        return panel;
    }

    private JPanel createDataRetrievalPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Retrieve Server Data (Op: 005)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(new JLabel("User ID to Retrieve:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        userIdToRetrieveField = new JTextField(10);
        panel.add(userIdToRetrieveField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0; gbc.gridwidth = 1;
        retrieveUserDataButton = new JButton("Get User Data (005)");
        retrieveUserDataButton.addActionListener(e -> sendRetrieveUserDataRequest());
        panel.add(retrieveUserDataButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        listTopicsButton = new JButton("List Topics (060)"); // Instanciação aqui
        listTopicsButton.addActionListener(e -> sendListTopicsRequest());
        panel.add(listTopicsButton, gbc);

        return panel;
    }

    private JPanel createTopicPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Create Forum Topic (050)"));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        topicTitleField = new JTextField(20); inputPanel.add(topicTitleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        topicSubjectField = new JTextField(20); inputPanel.add(topicSubjectField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        inputPanel.add(new JLabel("Message Content:"), gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        topicMessageArea = new JTextArea(5, 20);
        topicMessageArea.setLineWrap(true); // Quebra de linha automática
        topicMessageArea.setWrapStyleWord(true); // Quebra de linha por palavra
        JScrollPane messageScrollPane = new JScrollPane(topicMessageArea);
        inputPanel.add(messageScrollPane, gbc);

        panel.add(inputPanel, BorderLayout.CENTER);

        createTopicButton = new JButton("Create Topic");
        createTopicButton.addActionListener(e -> sendCreateTopicRequest());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(createTopicButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateGUIState() {
        boolean isLoggedIn = connected.get();

        serverHostField.setEnabled(!isLoggedIn);
        serverPortField.setEnabled(!isLoggedIn);

        usernameField.setEnabled(!isLoggedIn);
        passwordField.setEnabled(!isLoggedIn);

        nicknameField.setEnabled(true);
        newPasswordField.setEnabled(true);

        loginButton.setEnabled(!isLoggedIn);
        registerButton.setEnabled(!isLoggedIn);
        logoutButton.setEnabled(isLoggedIn);
        updateProfileButton.setEnabled(isLoggedIn);
        deleteAccountButton.setEnabled(isLoggedIn);

        topicTitleField.setEnabled(isLoggedIn);
        topicSubjectField.setEnabled(isLoggedIn);
        topicMessageArea.setEnabled(isLoggedIn);
        createTopicButton.setEnabled(isLoggedIn);

        userIdToRetrieveField.setEnabled(isLoggedIn);
        retrieveUserDataButton.setEnabled(isLoggedIn);
        listTopicsButton.setEnabled(isLoggedIn);
    }

    private void setupClientSocket() throws UnknownHostException, SocketException {
        String host = serverHostField.getText().trim();
        String portStr = serverPortField.getText().trim();

        if (host.isEmpty() || portStr.isEmpty()) {
            throw new IllegalArgumentException("Please fill in server host and port.");
        }

        serverPort = Integer.parseInt(portStr);
        serverAddress = InetAddress.getByName(host);

        if (socket == null || socket.isClosed()) {
            socket = new DatagramSocket();
            appendReceivedMessage("Client socket opened on local port: " + socket.getLocalPort());
        }

        if (!running.get()) {
            running.set(true);
            Thread listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();
            appendReceivedMessage("Listener thread started.");
        }
        appendReceivedMessage("Server target set to " + host + ":" + serverPort);
    }

    private void sendMessageToServer(ProtocolMessage message) {
        try {
            if (socket == null || socket.isClosed()) {
                throw new IllegalStateException("Client socket is not initialized or is closed. Please try to connect first.");
            }
            byte[] dataToSend = MessageUtils.serializeMessage(message);
            appendReceivedMessage("Sending " + message.getOperationCode() + " request: " + new String(dataToSend));
            System.out.println("CLIENT DEBUG - Sending " + message.getOperationCode() + " to " + serverAddress.getHostAddress() + ":" + serverPort + ": " + new String(dataToSend));

            DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (IOException e) {
            appendReceivedMessage("Error sending message: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to send message: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalStateException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage(e.getMessage());
        } catch (Exception e) {
            appendReceivedMessage("Unexpected error before sending: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Unexpected error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listenForMessages() {
        byte[] buffer = new byte[8192];

        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                ProtocolMessage message = MessageUtils.deserializeMessage(packet); // Desserializa a mensagem
                byte[] receivedData = MessageUtils.serializeMessage(message); // Serializa novamente para obter o JSON string
                appendReceivedMessage("Received " + message.getOperationCode() + " response: " + new String(receivedData));
                System.out.println("CLIENT DEBUG - Received " + message.getOperationCode() + " from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + ": " + new String(receivedData));
                handleReceivedMessage(message);

            } catch (SocketException e) {
                if (running.get()) { // Só loga se o socket não foi fechado intencionalmente
                    SwingUtilities.invokeLater(() ->
                            appendReceivedMessage("Socket error (likely closed): " + e.getMessage()));
                }
                running.set(false); // Para o loop de escuta se o socket for fechado
            } catch (IOException e) {
                // Se o cliente ainda estiver rodando, registra o erro de IO
                if (running.get()) {
                    SwingUtilities.invokeLater(() ->
                            appendReceivedMessage("Error receiving message: " + e.getMessage()));
                }
            } catch (Exception e) {
                // Captura quaisquer outros erros durante a desserialização ou processamento
                SwingUtilities.invokeLater(() ->
                        appendReceivedMessage("Error processing received message: " + e.getMessage()));
            }
        }
    }

    private void handleReceivedMessage(ProtocolMessage message) {
        SwingUtilities.invokeLater(() -> { // Garante que as atualizações da GUI sejam na EDT
            String opCode = message.getOperationCode();
            String messageDetails = message.getMessageContent(); // Usado para mensagens de erro ou conteúdo geral

            switch (opCode) {
                case "001": // Login Sucesso
                    currentToken = message.getToken();
                    currentUsername = message.getUser() != null ? message.getUser() : usernameField.getText();
                    connected.set(true);
                    updateGUIState();
                    appendReceivedMessage("Login successful! Welcome, " + currentUsername + ".");
                    break;
                case "002": // Erro de Login
                    appendReceivedMessage("SERVER ERROR (002 - Login Failed): " + messageDetails);
                    break;
                case "006": // S->C: Retornar Dados de Usuário Sucesso (resposta para 005)
                    String retrievedUser = message.getUser();
                    String retrievedNickname = message.getNickname();
                    appendReceivedMessage("\n--- User Data Retrieved (006) ---");
                    appendReceivedMessage("Username: " + retrievedUser);
                    appendReceivedMessage("Nickname: " + retrievedNickname);
                    appendReceivedMessage("----------------------------------\n");
                    break;
                case "007": // S->C: Erro de Retornar Dados de Usuário (resposta para 005)
                    appendReceivedMessage("SERVER ERROR (007 - Get User Data Failed): " + messageDetails);
                    break;
                case "011": // Registro Sucesso
                    appendReceivedMessage("Registration successful! You can now log in.");
                    break;
                case "012": // Erro de Registro
                    appendReceivedMessage("SERVER ERROR (012 - Registration Failed): " + messageDetails);
                    break;
                case "021": // Logout Sucesso
                    currentToken = null;
                    currentUsername = null;
                    connected.set(false);
                    updateGUIState();
                    appendReceivedMessage("Logged out successfully.");
                    break;
                case "022": // Erro de Logout
                    appendReceivedMessage("SERVER ERROR (022 - Logout Failed): " + messageDetails);
                    break;
                case "031": // Atualização de Perfil Sucesso
                    appendReceivedMessage("Profile updated successfully!");
                    break;
                case "032": // Erro de Atualização de Perfil
                    appendReceivedMessage("SERVER ERROR (032 - Profile Update Failed): " + messageDetails);
                    break;
                case "041": // Exclusão de Conta Sucesso
                    currentToken = null;
                    currentUsername = null;
                    connected.set(false);
                    updateGUIState();
                    appendReceivedMessage("Account deleted successfully.");
                    break;
                case "042": // Erro de Exclusão de Conta
                    appendReceivedMessage("SERVER ERROR (042 - Account Deletion Failed): " + messageDetails);
                    break;
                case "051": // Criação de Tópico Sucesso (confirmação para o cliente que criou)
                    appendReceivedMessage("Forum topic created successfully!");
                    topicTitleField.setText("");
                    topicSubjectField.setText("");
                    topicMessageArea.setText("");
                    break;
                case "052": // Erro de Criação de Tópico
                    appendReceivedMessage("SERVER ERROR (052 - Topic Creation Failed): " + messageDetails);
                    break;
                case "055": // Tópico de Fórum Transmitido (para todos os clientes)
                    // Este bloco depende que seu common.ProtocolMessage possua os getters para os campos de tópico.
                    String topicId = message.getTopicId();
                    String topicTitle = message.getTopicTitle();
                    String topicSubject = message.getTopicSubject();
                    String topicContent = message.getTopicContent();
                    String topicAuthor = message.getTopicAuthor();

                    appendReceivedMessage("\n--- NOVO TÓPICO DO FÓRUM ---");
                    appendReceivedMessage("ID: " + topicId);
                    appendReceivedMessage("Título: " + topicTitle);
                    appendReceivedMessage("Assunto: " + topicSubject);
                    appendReceivedMessage("Autor: " + topicAuthor);
                    appendReceivedMessage("Conteúdo:\n" + topicContent);
                    appendReceivedMessage("---------------------------\n");
                    break;
                case "061": // Resposta de Listar Tópicos (mantido se ProtocolMessage tiver 'topics')
                    List<Map<String, String>> topics = message.getTopics();
                    if (topics != null && !topics.isEmpty()) {
                        appendReceivedMessage("\n--- TÓPICOS DO FÓRUM (061) ---");
                        for (Map<String, String> topic : topics) {
                            appendReceivedMessage(
                                    "ID: " + topic.get("id") +
                                            ", Título: " + topic.get("title") +
                                            ", Assunto: " + topic.get("subject") +
                                            ", Autor: " + topic.get("author")
                            );
                        }
                        appendReceivedMessage("-------------------------------\n");
                    } else {
                        appendReceivedMessage("\n--- Nenhum tópico encontrado. ---");
                    }
                    break;
                case "062": // Erro ao Listar Tópicos
                    appendReceivedMessage("SERVER ERROR (062 - List Topics Failed): " + messageDetails);
                    break;
                // Os casos 071 e 072 não são mais necessários para a funcionalidade de "Listar Todos os Usuários Autenticados"
                // porque o protocolo foca no 005/006/007 para dados de *um* usuário.
                default:
                    // Mensagens de resposta desconhecidas ou não tratadas
                    appendReceivedMessage("Unknown server response: " + opCode + " - " + messageDetails);
            }
        });
    }

    private void sendLoginRequest() {
        try {
            setupClientSocket(); // Garante que o socket esteja pronto e o listener rodando

            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage loginMsg = new ProtocolMessage("000");
            loginMsg.setUser(user);
            loginMsg.setPassword(pass);
            sendMessageToServer(loginMsg);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Input error: " + e.getMessage());
        } catch (UnknownHostException | SocketException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Connection setup error: " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending login request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending login request: " + e.getMessage());
        }
    }

    private void sendRegisterRequest() {
        try {
            setupClientSocket(); // Garante que o socket esteja pronto e o listener rodando

            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();
            String nick = nicknameField.getText().trim();

            if (user.isEmpty() || pass.isEmpty() || nick.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username, password, and nickname cannot be empty for registration.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage registerMsg = new ProtocolMessage("010");
            registerMsg.setUser(user);
            registerMsg.setPassword(pass);
            registerMsg.setNickname(nick);
            sendMessageToServer(registerMsg);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Input error: " + e.getMessage());
        } catch (UnknownHostException | SocketException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server for registration: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Connection setup error: " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending registration request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending registration request: " + e.getMessage());
        }
    }

    private void sendLogoutRequest() {
        if (!connected.get() || currentUsername == null || currentToken == null) {
            appendReceivedMessage("Not logged in. No logout request sent.");
            return;
        }
        try {
            ProtocolMessage logoutMsg = new ProtocolMessage("020");
            logoutMsg.setUser(currentUsername);
            logoutMsg.setToken(currentToken);
            sendMessageToServer(logoutMsg);
        } catch (Exception e) { // sendMessageToServer já trata IOException/IllegalStateException
            JOptionPane.showMessageDialog(this, "Error sending logout request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending logout request: " + e.getMessage());
        }
    }

    private void sendUpdateProfileRequest() {
        if (!connected.get() || currentUsername == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to update your profile.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String user = usernameField.getText().trim(); // Usa o nome de usuário atual logado
            String pass = new String(passwordField.getPassword()).trim(); // Senha atual para verificação
            String newNick = nicknameField.getText().trim(); // Novo apelido (opcional)
            String newPass = new String(newPasswordField.getPassword()).trim(); // Nova senha (opcional)

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Current username and password are required to update profile.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (newNick.isEmpty() && newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a new nickname or a new password to update.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage updateMsg = new ProtocolMessage("030");
            updateMsg.setUser(user);
            updateMsg.setPassword(pass);
            updateMsg.setToken(currentToken); // Inclua o token para autenticação da requisição
            if (!newNick.isEmpty()) {
                updateMsg.setNewNickname(newNick);
            }
            if (!newPass.isEmpty()) {
                updateMsg.setNewPassword(newPass);
            }

            sendMessageToServer(updateMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending profile update request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending profile update request: " + e.getMessage());
        }
    }

    private void sendDeleteAccountRequest() {
        if (!connected.get() || currentUsername == null || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to delete your account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete your account? This action cannot be undone.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password are required to delete the account.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage deleteMsg = new ProtocolMessage("040");
            deleteMsg.setUser(user);
            deleteMsg.setToken(currentToken);
            deleteMsg.setPassword(pass);
            sendMessageToServer(deleteMsg);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending account deletion request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending account deletion request: " + e.getMessage());
        }
    }

    private void sendCreateTopicRequest() {
        if (!connected.get() || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to create a topic.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String title = topicTitleField.getText().trim();
            String subject = topicSubjectField.getText().trim();
            String msgContent = topicMessageArea.getText().trim();

            if (title.isEmpty() || subject.isEmpty() || msgContent.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title, subject, and message content cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage topicMsg = new ProtocolMessage("050");
            topicMsg.setToken(currentToken);
            topicMsg.setTitle(title);
            topicMsg.setSubject(subject);
            topicMsg.setMessageContent(msgContent);
            sendMessageToServer(topicMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending topic creation request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending topic creation request: " + e.getMessage());
        }
    }

    private void sendListTopicsRequest() {
        if (!connected.get() || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to list topics.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            ProtocolMessage listTopicsMsg = new ProtocolMessage("060");
            listTopicsMsg.setToken(currentToken);
            sendMessageToServer(listTopicsMsg);
            appendReceivedMessage("Sending list topics request (060).");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending list topics request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending list topics request: " + e.getMessage());
        }
    }

    private void sendRetrieveUserDataRequest() {
        if (!connected.get() || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to retrieve user data.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String userId = userIdToRetrieveField.getText().trim();

            if (userId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a User ID to retrieve data.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage retrieveUserMsg = new ProtocolMessage("005");
            retrieveUserMsg.setToken(currentToken);
            retrieveUserMsg.setUser(userId); // O usuário que você quer obter os dados
            sendMessageToServer(retrieveUserMsg);
            appendReceivedMessage("Sending retrieve user data request (005) for user: " + userId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending retrieve user data request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending retrieve user data request: " + e.getMessage());
        }
    }

    private void appendReceivedMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            receivedMessagesArea.append("[" + new java.util.Date() + "] " + message + "\n");
            receivedMessagesArea.setCaretPosition(receivedMessagesArea.getDocument().getLength()); // Rola para o final
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UDPClient());
    }
}