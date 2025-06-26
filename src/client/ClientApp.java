package client;

import client.ui.*; // Importa o novo pacote com os painéis da UI
import common.ProtocolMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ClientApp extends JFrame {
    //<editor-fold desc="STATE AND CONNECTION FIELDS">
    private final ClientConnection connection;
    private String currentUsername;
    private String currentToken;
    //</editor-fold>

    //<editor-fold desc="UI PANEL FIELDS">
    private AuthPanel authPanel;
    private DataRetrievalPanel dataRetrievalPanel;
    private TopicPanel topicPanel;
    private ReplyPanel replyPanel;
    private AdminPanel adminPanel;
    private LogPanel logPanel;
    private JTabbedPane mainTabbedPane;
    //</editor-fold>

    //<editor-fold desc="CONSTRUCTOR">
    public ClientApp() {
        // A lógica de negócio é inicializada primeiro
        connection = new ClientConnection(this::handleReceivedMessage, this::appendLogMessage);

        // A UI é construída e os eventos são registrados
        initializeGUI();
        registerActionListeners();
        updateGUIState(); // Define o estado inicial da UI

        // A janela se torna visível por último
        setVisible(true);
    }
    //</editor-fold>

    //<editor-fold desc="GUI INITIALIZATION">
    /**
     * Monta a janela principal usando os painéis customizados.
     * Este método agora é muito mais limpo e organizado.
     */
    private void initializeGUI() {
        setTitle("TCP Client (Forum) - Refactored");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 950);
        setLocationRelativeTo(null);

        // 1. Instancia os painéis da UI
        authPanel = new AuthPanel();
        dataRetrievalPanel = new DataRetrievalPanel();
        topicPanel = new TopicPanel();
        replyPanel = new ReplyPanel();
        adminPanel = new AdminPanel();
        logPanel = new LogPanel();

        // 2. Monta o layout principal
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(authPanel, BorderLayout.NORTH);
        topPanel.add(dataRetrievalPanel, BorderLayout.CENTER);

        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.addTab("Create Topic", topicPanel);
        mainTabbedPane.addTab("Replies", replyPanel);
        mainTabbedPane.addTab("Admin Operations", adminPanel);

        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(topPanel, BorderLayout.NORTH);
        mainContentPanel.add(mainTabbedPane, BorderLayout.CENTER);
        mainContentPanel.add(logPanel, BorderLayout.SOUTH);

        add(mainContentPanel);

        // 3. Adiciona o listener para fechar a janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connection.isConnected() && currentToken != null) {
                    sendLogoutRequest();
                    try {
                        Thread.sleep(500); // Dá um tempo para a mensagem de logout ser enviada
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
                connection.disconnect();
                appendLogMessage("Client application closing.");
                System.exit(0);
            }
        });
    }

    /**
     * NOVO MÉTODO: Centraliza o registro de todos os ActionListeners.
     * Ele conecta a UI (botões dos painéis) com a lógica (métodos send...Request).
     */
    private void registerActionListeners() {
        // Painel de Autenticação
        authPanel.getLoginButton().addActionListener(e -> sendLoginRequest());
        authPanel.getRegisterButton().addActionListener(e -> sendRegisterRequest());
        authPanel.getLogoutButton().addActionListener(e -> sendLogoutRequest());
        authPanel.getUpdateProfileButton().addActionListener(e -> sendUpdateProfileRequest());
        authPanel.getDeleteAccountButton().addActionListener(e -> sendDeleteAccountRequest());

        // Painel de Busca de Dados
        dataRetrievalPanel.getRetrieveUserDataButton().addActionListener(e -> sendRetrieveUserDataRequest());
        dataRetrievalPanel.getGetAllTopicsButton().addActionListener(e -> sendGetAllTopicsRequest());

        // Painel de Tópicos
        topicPanel.getCreateTopicButton().addActionListener(e -> sendCreateTopicRequest());

        // Painel de Respostas
        replyPanel.getSendReplyButton().addActionListener(e -> sendReplyMessageRequest());
        replyPanel.getGetRepliesButton().addActionListener(e -> sendGetRepliesRequest());

        // Painel de Admin
        adminPanel.getUpdateUserButton().addActionListener(e -> sendAdminUpdateUserRequest());
        adminPanel.getDeleteUserButton().addActionListener(e -> sendAdminDeleteUserRequest());
        adminPanel.getDeleteMessageButton().addActionListener(e -> sendAdminDeleteMessageRequest());
        adminPanel.getListAllUsersButton().addActionListener(e -> sendAdminListAllUsersRequest());
    }

    /**
     * Atualiza o estado de todos os painéis com base no status de login.
     */
    private void updateGUIState() {
        boolean isLoggedIn = connection.isConnected() && currentToken != null;
        boolean isAdmin = isLoggedIn && currentToken.startsWith("a");

        authPanel.updateState(isLoggedIn);
        dataRetrievalPanel.updateState(isLoggedIn);
        topicPanel.updateState(isLoggedIn);
        replyPanel.updateState(isLoggedIn);
        adminPanel.updateState(isAdmin);

        if (!isAdmin) {
            adminPanel.clearUpdateFields();
        }
    }
    //</editor-fold>

    //<editor-fold desc="SERVER RESPONSE HANDLING">
    /**
     * Processa as mensagens recebidas do servidor e atualiza a UI.
     */
    private void handleReceivedMessage(ProtocolMessage message) {
        SwingUtilities.invokeLater(() -> {
            String opCode = message.getOperationCode();
            String messageDetails = message.getMessageContent();

            switch (opCode) {
                case "001": // Login Success
                    currentToken = message.getToken();
                    currentUsername = authPanel.getUsername();
                    appendLogMessage("Login successful! Welcome, " + currentUsername + ".");
                    updateGUIState();
                    mainTabbedPane.setSelectedIndex(0);
                    break;
                case "002": // Login Error
                    appendLogMessage("SERVER ERROR (002 - Login Failed): " + messageDetails);
                    connection.disconnect();
                    updateGUIState();
                    break;
                case "006": // Retrieve User Data Success
                    appendLogMessage("\n--- User Data Retrieved (006) ---\n" +
                            "Username: " + message.getUser() + "\n" +
                            "Nickname: " + message.getNickname() + "\n" +
                            "----------------------------------\n");
                    break;
                case "007": appendLogMessage("SERVER ERROR (007 - Get User Data Failed): " + messageDetails); break;
                case "011": // Register Success
                    appendLogMessage("Registration successful! " + messageDetails);
                    authPanel.clearRegisterFields();
                    break;
                case "012": appendLogMessage("SERVER ERROR (012 - Registration Failed): " + messageDetails); break;
                case "021": // Logout Success
                    currentToken = null;
                    currentUsername = null;
                    updateGUIState();
                    appendLogMessage("Logged out successfully. " + messageDetails);
                    mainTabbedPane.setSelectedIndex(0);
                    break;
                case "022": appendLogMessage("SERVER ERROR (022 - Logout Failed): " + messageDetails); break;
                case "031": // Update Profile Success
                    appendLogMessage("Profile updated successfully! " + messageDetails);
                    break;
                case "032": appendLogMessage("SERVER ERROR (032 - Profile Update Failed): " + messageDetails); break;
                case "041": // Delete Account Success
                    currentToken = null;
                    currentUsername = null;
                    updateGUIState();
                    appendLogMessage("Account deleted successfully. " + messageDetails);
                    mainTabbedPane.setSelectedIndex(0);
                    break;
                case "042": appendLogMessage("SERVER ERROR (042 - Account Deletion Failed): " + messageDetails); break;
                case "051": // Create Topic Success
                    appendLogMessage("Forum topic created successfully! " + messageDetails);
                    topicPanel.clearFields();
                    break;
                case "052": appendLogMessage("SERVER ERROR (052 - Topic Creation Failed): " + messageDetails); break;
                case "061": // Send Reply Success
                    appendLogMessage("Reply sent successfully! " + messageDetails);
                    replyPanel.clearReplyFields();
                    break;
                case "062": appendLogMessage("SERVER ERROR (062 - Send Reply Failed): " + messageDetails); break;
                case "071": // Get Replies Response
                case "076": // Get Topics Response
                case "111": // List All Users Response
                    // A lógica para listas complexas pode ser extraída para métodos auxiliares
                    displayListFromServer(opCode, message);
                    break;
                case "072": appendLogMessage("SERVER ERROR (072 - Get Replies Failed): " + messageDetails); break;
                case "077": appendLogMessage("SERVER ERROR (077 - Get Topics Failed): " + messageDetails); break;
                case "081": // Admin Update User Success
                    appendLogMessage("User profile updated by admin successfully! " + messageDetails);
                    adminPanel.clearUpdateFields();
                    break;
                case "082": appendLogMessage("SERVER ERROR (082 - Admin Update User Failed): " + messageDetails); break;
                case "091": // Admin Delete User Success
                    appendLogMessage("User account deleted by admin successfully! " + messageDetails);
                    adminPanel.clearUpdateFields();
                    break;
                case "092": appendLogMessage("SERVER ERROR (092 - Admin Delete User Failed): " + messageDetails); break;
                case "101": // Admin Delete Message Success
                    appendLogMessage("Message/Topic deleted by admin successfully! " + messageDetails);
                    break;
                case "102": appendLogMessage("SERVER ERROR (102 - Admin Delete Message Failed): " + messageDetails); break;
                case "112": appendLogMessage("SERVER ERROR (112 - List All Users Failed): " + messageDetails); break;
                default:
                    appendLogMessage("Unknown server response: " + opCode + " - " + messageDetails);
            }
        });
    }

    private void displayListFromServer(String opCode, ProtocolMessage message) {
        // Lógica para exibir listas de forma organizada
    }

    private void appendLogMessage(String message) {
        logPanel.appendMessage(message);
    }
    //</editor-fold>

    //<editor-fold desc="REQUEST SENDING METHODS">
    private void sendLoginRequest() {
        try {
            String host = authPanel.getHost();
            int port = Integer.parseInt(authPanel.getPort());

            if (!connection.isConnected()) {
                connection.connect(host, port);
            }

            String user = authPanel.getUsername();
            String pass = new String(authPanel.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage loginMsg = new ProtocolMessage("000");
            loginMsg.setUser(user);
            loginMsg.setPassword(pass);
            connection.sendMessage(loginMsg);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            connection.disconnect();
            updateGUIState();
        }
    }

    private void sendRegisterRequest() {
        try {
            String host = authPanel.getHost();
            int port = Integer.parseInt(authPanel.getPort());

            if (!connection.isConnected()) {
                connection.connect(host, port);
            }

            String user = authPanel.getUsername();
            String pass = new String(authPanel.getPassword());
            String nick = authPanel.getNickname();

            if (user.isEmpty() || pass.isEmpty() || nick.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username, password, and nickname cannot be empty for registration.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage registerMsg = new ProtocolMessage("010");
            registerMsg.setUser(user);
            registerMsg.setPassword(pass);
            registerMsg.setNickname(nick);
            connection.sendMessage(registerMsg);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server for registration: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendLogoutRequest() {
        if (!connection.isConnected() || currentUsername == null || currentToken == null) {
            appendLogMessage("Not logged in. No logout request sent.");
            return;
        }
        try {
            ProtocolMessage logoutMsg = new ProtocolMessage("020");
            logoutMsg.setUser(currentUsername);
            logoutMsg.setToken(currentToken);
            connection.sendMessage(logoutMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending logout request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendLogMessage("Error sending logout request: " + e.getMessage());
        }
    }

    private void sendUpdateProfileRequest() {
        if (!connection.isConnected() || currentUsername == null || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to update your profile.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String user = authPanel.getUsername();
            String pass = new String(authPanel.getPassword());
            String newNick = authPanel.getNickname();
            String newPass = new String(authPanel.getNewPassword());

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
            updateMsg.setToken(currentToken);
            if (!newNick.isEmpty()) {
                updateMsg.setNewNickname(newNick);
            }
            if (!newPass.isEmpty()) {
                updateMsg.setNewPassword(newPass);
            }
            connection.sendMessage(updateMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending profile update request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendDeleteAccountRequest() {
        if (!connection.isConnected() || currentUsername == null || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to delete your account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete your account? This action cannot be undone.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            String user = authPanel.getUsername();
            String pass = new String(authPanel.getPassword());
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password are required to delete the account.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage deleteMsg = new ProtocolMessage("040");
            deleteMsg.setUser(user);
            deleteMsg.setToken(currentToken);
            deleteMsg.setPassword(pass);
            connection.sendMessage(deleteMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending account deletion request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendCreateTopicRequest() {
        if (!connection.isConnected() || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to create a topic.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String title = topicPanel.getTitleText();
            String subject = topicPanel.getSubjectText();
            String msgContent = topicPanel.getMessageText();

            if (title.isEmpty() || subject.isEmpty() || msgContent.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title, subject, and message content cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage topicMsg = new ProtocolMessage("050");
            topicMsg.setToken(currentToken);
            topicMsg.setTitle(title);
            topicMsg.setSubject(subject);
            topicMsg.setMessageContent(msgContent);
            connection.sendMessage(topicMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending topic creation request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendRetrieveUserDataRequest() {
        if (!connection.isConnected() || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to retrieve user data.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String username = dataRetrievalPanel.getUsernameToRetrieve();
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a Username to retrieve data.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage retrieveUserMsg = new ProtocolMessage("005");
            retrieveUserMsg.setToken(currentToken);
            retrieveUserMsg.setUser(username); // Protocol expects username
            connection.sendMessage(retrieveUserMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending retrieve user data request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendReplyMessageRequest() {
        if (!connection.isConnected() || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to reply.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String topicId = replyPanel.getReplyTopicId();
            String msgContent = replyPanel.getReplyMessage();

            if (topicId.isEmpty() || msgContent.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Topic ID and reply content cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage replyMsg = new ProtocolMessage("060");
            replyMsg.setToken(currentToken);
            replyMsg.setId(topicId);
            replyMsg.setMessageContent(msgContent);
            connection.sendMessage(replyMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending reply request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendGetRepliesRequest() {
        if (!connection.isConnected()) {
            JOptionPane.showMessageDialog(this, "You must be connected to get replies.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String topicId = replyPanel.getRepliesTopicId();
            if (topicId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Topic ID cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ProtocolMessage getRepliesMsg = new ProtocolMessage("070");
            getRepliesMsg.setId(topicId);
            connection.sendMessage(getRepliesMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending get replies request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendGetAllTopicsRequest() {
        if (!connection.isConnected()) {
            JOptionPane.showMessageDialog(this, "You must be connected to get all topics.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            ProtocolMessage getAllTopicsMsg = new ProtocolMessage("075");
            connection.sendMessage(getAllTopicsMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending get all topics request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendAdminUpdateUserRequest() {
        if (!connection.isConnected() || currentToken == null || !currentToken.startsWith("a")) {
            JOptionPane.showMessageDialog(this, "Admin privileges required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String targetUser = adminPanel.getTargetUser();
            String newNick = adminPanel.getNewNick();
            String newPass = new String(adminPanel.getNewPass());

            if (targetUser.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Target username cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (newNick.isEmpty() && newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a new nickname or a new password.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage adminUpdateMsg = new ProtocolMessage("080");
            adminUpdateMsg.setToken(currentToken);
            adminUpdateMsg.setUser(targetUser);
            if (!newNick.isEmpty()) {
                adminUpdateMsg.setNewNickname(newNick);
            }
            if (!newPass.isEmpty()) {
                adminUpdateMsg.setNewPassword(newPass);
            }
            connection.sendMessage(adminUpdateMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending admin update user request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendAdminDeleteUserRequest() {
        if (!connection.isConnected() || currentToken == null || !currentToken.startsWith("a")) {
            JOptionPane.showMessageDialog(this, "Admin privileges required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String targetUser = adminPanel.getTargetUser();
            if (targetUser.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Target username cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ProtocolMessage adminDeleteUserMsg = new ProtocolMessage("090");
            adminDeleteUserMsg.setToken(currentToken);
            adminDeleteUserMsg.setUser(targetUser);
            connection.sendMessage(adminDeleteUserMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending admin delete user request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendAdminDeleteMessageRequest() {
        if (!connection.isConnected() || currentToken == null || !currentToken.startsWith("a")) {
            JOptionPane.showMessageDialog(this, "Admin privileges required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this message/topic?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String messageId = adminPanel.getDeleteMessageId();
            if (messageId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Message/Topic ID cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ProtocolMessage adminDeleteMessageMsg = new ProtocolMessage("100");
            adminDeleteMessageMsg.setToken(currentToken);
            adminDeleteMessageMsg.setId(messageId);
            connection.sendMessage(adminDeleteMessageMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending admin delete message request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendAdminListAllUsersRequest() {
        if (!connection.isConnected() || currentToken == null || !currentToken.startsWith("a")) {
            JOptionPane.showMessageDialog(this, "Admin privileges required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            ProtocolMessage listUsersMsg = new ProtocolMessage("110");
            listUsersMsg.setToken(currentToken);
            connection.sendMessage(listUsersMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending list all users request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    //</editor-fold>

    //<editor-fold desc="MAIN METHOD">
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientApp::new);
    }
    //</editor-fold>
}