package client;

import client.ui.*;
import common.ProtocolMessage;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ClientApp extends JFrame {
    private final ClientConnection connection;
    private String currentUsername;
    private String currentToken;
    private AuthPanel authPanel;
    private DataRetrievalPanel dataRetrievalPanel;
    private TopicPanel topicPanel;
    private ReplyPanel replyPanel;
    private AdminPanel adminPanel;
    private LogPanel logPanel;
    private JTabbedPane mainTabbedPane;

    public ClientApp() {
        connection = new ClientConnection(this::handleReceivedMessage, this::appendLogMessage);

        initializeGUI();
        registerActionListeners();
        updateGUIState();

        setVisible(true);
    }

    private void initializeGUI() {
        setTitle("TCP Client (Forum) - Refactored");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 950);
        setLocationRelativeTo(null);

        authPanel = new AuthPanel();
        dataRetrievalPanel = new DataRetrievalPanel();
        topicPanel = new TopicPanel();
        replyPanel = new ReplyPanel();
        adminPanel = new AdminPanel();
        logPanel = new LogPanel();

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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connection.isConnected() && currentToken != null) {
                    sendLogoutRequest();
                    try {
                        Thread.sleep(500);
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

    private void registerActionListeners() {
        authPanel.getLoginButton().addActionListener(e -> sendLoginRequest());
        authPanel.getRegisterButton().addActionListener(e -> sendRegisterRequest());
        authPanel.getLogoutButton().addActionListener(e -> sendLogoutRequest());
        authPanel.getUpdateProfileButton().addActionListener(e -> sendUpdateProfileRequest());
        authPanel.getDeleteAccountButton().addActionListener(e -> sendDeleteAccountRequest());

        dataRetrievalPanel.getRetrieveUserDataButton().addActionListener(e -> sendRetrieveUserDataRequest());
        dataRetrievalPanel.getGetAllTopicsButton().addActionListener(e -> sendGetAllTopicsRequest());

        topicPanel.getCreateTopicButton().addActionListener(e -> sendCreateTopicRequest());

        replyPanel.getSendReplyButton().addActionListener(e -> sendReplyMessageRequest());
        replyPanel.getGetRepliesButton().addActionListener(e -> sendGetRepliesRequest());

        adminPanel.getUpdateUserButton().addActionListener(e -> sendAdminUpdateUserRequest());
        adminPanel.getDeleteUserButton().addActionListener(e -> sendAdminDeleteUserRequest());
        adminPanel.getDeleteMessageButton().addActionListener(e -> sendAdminDeleteMessageRequest());
        adminPanel.getListAllUsersButton().addActionListener(e -> sendAdminListAllUsersRequest());
    }

    private void updateGUIState() {
        boolean isLoggedIn = connection.isConnected() && currentToken != null;
        boolean isAdmin = isLoggedIn && currentToken != null && currentToken.startsWith("a");

        authPanel.updateState(isLoggedIn);
        dataRetrievalPanel.updateState(isLoggedIn);
        topicPanel.updateState(isLoggedIn);
        replyPanel.updateState(isLoggedIn);
        adminPanel.updateState(isAdmin);

        if (!isAdmin) {
            adminPanel.clearUpdateFields();
        }
    }

    private void handleReceivedMessage(ProtocolMessage message) {
        SwingUtilities.invokeLater(() -> {
            appendJsonLog("RECEIVED", message);
            String opCode = message.getOperationCode();
            String messageDetails = message.getMessageContent();

            switch (opCode) {
                case "001":
                    currentToken = message.getToken();
                    currentUsername = authPanel.getUsername();
                    appendLogMessage("Login successful! Welcome, " + currentUsername + ".");
                    updateGUIState();
                    mainTabbedPane.setSelectedIndex(0);
                    break;
                case "002":
                    appendLogMessage("SERVER ERROR (002 - Login Failed): " + messageDetails);
                    connection.disconnect();
                    updateGUIState();
                    break;
                case "006":
                    appendLogMessage("\n--- User Data Retrieved (006) ---\n" +
                            "Username: " + message.getUser() + "\n" +
                            "Nickname: " + message.getNickname() + "\n" +
                            "----------------------------------\n");
                    break;
                case "007": appendLogMessage("SERVER ERROR (007 - Get User Data Failed): " + messageDetails); break;
                case "011":
                    appendLogMessage("Registration successful! " + messageDetails);
                    authPanel.clearRegisterFields();
                    break;
                case "012": appendLogMessage("SERVER ERROR (012 - Registration Failed): " + messageDetails); break;
                case "021":
                    currentToken = null;
                    currentUsername = null;
                    updateGUIState();
                    appendLogMessage("Logged out successfully. " + messageDetails);
                    mainTabbedPane.setSelectedIndex(0);
                    break;
                case "022": appendLogMessage("SERVER ERROR (022 - Logout Failed): " + messageDetails); break;
                case "031":
                    appendLogMessage("Profile updated successfully! " + messageDetails);
                    break;
                case "032": appendLogMessage("SERVER ERROR (032 - Profile Update Failed): " + messageDetails); break;
                case "041":
                    currentToken = null;
                    currentUsername = null;
                    updateGUIState();
                    appendLogMessage("Account deleted successfully. " + messageDetails);
                    mainTabbedPane.setSelectedIndex(0);
                    break;
                case "042": appendLogMessage("SERVER ERROR (042 - Account Deletion Failed): " + messageDetails); break;
                case "051":
                    appendLogMessage("Forum topic created successfully! " + messageDetails);
                    topicPanel.clearFields();
                    break;
                case "052": appendLogMessage("SERVER ERROR (052 - Topic Creation Failed): " + messageDetails); break;
                case "061":
                    appendLogMessage("Reply sent successfully! " + messageDetails);
                    replyPanel.clearReplyFields();
                    break;
                case "062": appendLogMessage("SERVER ERROR (062 - Send Reply Failed): " + messageDetails); break;
                case "071":
                case "076":
                case "111":
                    displayListFromServer(opCode, message);
                    break;
                case "072": appendLogMessage("SERVER ERROR (072 - Get Replies Failed): " + messageDetails); break;
                case "077": appendLogMessage("SERVER ERROR (077 - Get Topics Failed): " + messageDetails); break;
                case "081":
                    appendLogMessage("User profile updated by admin successfully! " + messageDetails);
                    adminPanel.clearUpdateFields();
                    break;
                case "082": appendLogMessage("SERVER ERROR (082 - Admin Update User Failed): " + messageDetails); break;
                case "091":
                    appendLogMessage("User account deleted by admin successfully! " + messageDetails);
                    adminPanel.clearUpdateFields();
                    break;
                case "092": appendLogMessage("SERVER ERROR (092 - Admin Delete User Failed): " + messageDetails); break;
                case "101":
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
        StringBuilder listText = new StringBuilder();
        String title = "Server Response";

        switch (opCode) {
            case "111":
                title = "All Registered Users";
                List<Map<String, String>> users = message.getUsers();
                if (users == null || users.isEmpty()) {
                    listText.append("No registered users found.");
                } else {
                    for (Map<String, String> user : users) {
                        listText.append("Username: ").append(user.get("user"))
                                .append(" | Nickname: ").append(user.get("nick")).append("\n");
                    }
                }
                break;

            case "076":
                title = "All Topics";
                List<Map<String, String>> topics = message.getTopics();
                if (topics == null || topics.isEmpty()) {
                    listText.append("No topics found.");
                } else {
                    for (Map<String, String> topic : topics) {
                        listText.append("ID: ").append(topic.get("id"))
                                .append(" | Title: ").append(topic.get("title"))
                                .append(" | Author: ").append(topic.get("author")).append("\n")
                                .append("   Subject: ").append(topic.get("subject")).append("\n---\n");
                    }
                }
                break;

            case "071":
                title = "Replies for Topic " + message.getTopicId();
                List<Map<String, String>> replies = message.getMessageList();
                if (replies == null || replies.isEmpty()) {
                    listText.append("No replies found for this topic.");
                } else {
                    for (Map<String, String> reply : replies) {
                        listText.append("ID: ").append(reply.get("id"))
                                .append(" | Author: ").append(reply.get("author")).append("\n")
                                .append("   > ").append(reply.get("content")).append("\n---\n");
                    }
                }
                break;

            default:
                appendLogMessage("List type not implemented for display: " + opCode);
                return;
        }
        showListInDialog(title, listText.toString());
    }

    private void showListInDialog(String title, String content) {
        JTextArea textArea = new JTextArea(20, 50);
        textArea.setText(content);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void appendLogMessage(String message) {
        logPanel.appendMessage(message);
    }

    private void appendJsonLog(String direction, ProtocolMessage message) {
        appendLogMessage(direction + ": " + message.toString());
    }

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
            appendJsonLog("SENT", loginMsg);
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
            appendJsonLog("SENT", registerMsg);
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
            appendJsonLog("SENT", logoutMsg);
            connection.sendMessage(logoutMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending logout request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            appendJsonLog("SENT", updateMsg);
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
            appendJsonLog("SENT", deleteMsg);
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
            appendJsonLog("SENT", topicMsg);
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
            appendJsonLog("SENT", retrieveUserMsg);
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
            appendJsonLog("SENT", replyMsg);
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
            appendJsonLog("SENT", getRepliesMsg);
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
            appendJsonLog("SENT", getAllTopicsMsg);
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
            appendJsonLog("SENT", adminUpdateMsg);
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
            appendJsonLog("SENT", adminDeleteUserMsg);
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
            appendJsonLog("SENT", adminDeleteMessageMsg);
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
            appendJsonLog("SENT", listUsersMsg);
            connection.sendMessage(listUsersMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending list all users request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientApp::new);
    }
}
