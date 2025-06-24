package client;

import common.ProtocolMessage;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientApp extends JFrame {
    private ClientConnection connection;
    private String currentUsername;
    private String currentToken;
    private AtomicBoolean connected = new AtomicBoolean(false);

    // --- GUI Components ---
    private JTextField serverHostField;
    private JTextField serverPortField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField nicknameField;
    private JPasswordField newPasswordField;

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
    private JButton retrieveUserDataButton;
    private JButton getAllTopicsButton; // For 075

    private JTextField replyTopicIdField;
    private JTextArea replyMessageArea;
    private JButton sendReplyButton;

    private JTextField getRepliesTopicIdField;
    private JButton getRepliesButton;

    private JTextField adminTargetUserField;
    private JTextField adminNewNickField;
    private JPasswordField adminNewPassField;
    private JButton adminUpdateUserButton;
    private JButton adminDeleteUserButton;

    private JTextField adminDeleteMessageIdField;
    private JButton adminDeleteMessageButton;

    // New admin button for 110
    private JButton adminListAllUsersButton;

    private JTextArea receivedMessagesArea;

    private JTabbedPane mainTabbedPane; // JTabbedPane to organize the main content sections

    public ClientApp() {
        connection = new ClientConnection(this::handleReceivedMessage, this::appendReceivedMessage);
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("TCP Client (Forum)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 950);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(createAuthPanel(), BorderLayout.NORTH);
        topPanel.add(createDataRetrievalPanel(), BorderLayout.CENTER);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.addTab("Create Topic", createTopicPanel());
        mainTabbedPane.addTab("Replies", createReplyPanel());
        mainTabbedPane.addTab("Admin Operations", createAdminPanel());
        mainPanel.add(mainTabbedPane, BorderLayout.CENTER);

        JPanel receivedPanel = new JPanel(new BorderLayout());
        receivedPanel.setBorder(BorderFactory.createTitledBorder("Forum Messages / Client Log"));
        receivedMessagesArea = new JTextArea(15, 0);
        receivedMessagesArea.setEditable(false);
        receivedMessagesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(receivedMessagesArea);
        receivedPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(receivedPanel, BorderLayout.SOUTH);

        add(mainPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connected.get()) {
                    sendLogoutRequest();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
                connection.disconnect(); // Socket closed only on program exit
                appendReceivedMessage("Client application closing.");
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
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Server Host:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        serverHostField = new JTextField("localhost", 15); panel.add(serverHostField, gbc);

        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 3;
        serverPortField = new JTextField("12345", 8); panel.add(serverPortField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        usernameField = new JTextField(15); panel.add(usernameField, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 3;
        passwordField = new JPasswordField(15); panel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Nickname (Register):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        nicknameField = new JTextField(15); panel.add(nicknameField, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel("New Password (Update):"), gbc);
        gbc.gridx = 3;
        newPasswordField = new JPasswordField(15); panel.add(newPasswordField, gbc);

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
        panel.setBorder(BorderFactory.createTitledBorder("Retrieve Server Data (Op: 005) / Get All Topics (Op: 075)"));
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

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        getAllTopicsButton = new JButton("Get All Topics (075)");
        getAllTopicsButton.addActionListener(e -> sendGetAllTopicsRequest());
        panel.add(getAllTopicsButton, gbc);

        return panel;
    }

    private JPanel createTopicPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        // No longer needs a titled border as the tab provides the title

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
        topicMessageArea.setLineWrap(true);
        topicMessageArea.setWrapStyleWord(true);
        JScrollPane messageScrollPane = new JScrollPane(topicMessageArea);
        inputPanel.add(messageScrollPane, gbc);

        panel.add(inputPanel, BorderLayout.CENTER);

        createTopicButton = new JButton("Create Topic (050)");
        createTopicButton.addActionListener(e -> sendCreateTopicRequest());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(createTopicButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createReplyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        // Removed: panel.setBorder(BorderFactory.createTitledBorder("Reply to Topic (060) / Get Replies (070)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Topic ID to Reply:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        replyTopicIdField = new JTextField(10); panel.add(replyTopicIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        panel.add(new JLabel("Reply Content:"), gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        replyMessageArea = new JTextArea(3, 20);
        replyMessageArea.setLineWrap(true);
        replyMessageArea.setWrapStyleWord(true);
        JScrollPane replyScrollPane = new JScrollPane(replyMessageArea);
        panel.add(replyScrollPane, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        sendReplyButton = new JButton("Send Reply (060)");
        sendReplyButton.addActionListener(e -> sendReplyMessageRequest());
        panel.add(sendReplyButton, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(new JLabel("Topic ID to Get Replies:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1.0;
        getRepliesTopicIdField = new JTextField(10); panel.add(getRepliesTopicIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        getRepliesButton = new JButton("Get Replies (070)");
        getRepliesButton.addActionListener(e -> sendGetRepliesRequest());
        panel.add(getRepliesButton, gbc);

        return panel;
    }

    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        // Removed: panel.setBorder(BorderFactory.createTitledBorder("Admin Operations (Requires Admin Token)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Target User (Admin):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        adminTargetUserField = new JTextField(15); panel.add(adminTargetUserField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("New Nick (Admin):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        adminNewNickField = new JTextField(15); panel.add(adminNewNickField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("New Pass (Admin):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        adminNewPassField = new JPasswordField(15); panel.add(adminNewPassField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        adminUpdateUserButton = new JButton("Update User (Admin) (080)");
        adminUpdateUserButton.addActionListener(e -> sendAdminUpdateUserRequest());
        panel.add(adminUpdateUserButton, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        adminDeleteUserButton = new JButton("Delete User (Admin) (090)");
        adminDeleteUserButton.addActionListener(e -> sendAdminDeleteUserRequest());
        panel.add(adminDeleteUserButton, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(new JLabel("Message/Topic ID to Delete:"), gbc);
        gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 1.0;
        adminDeleteMessageIdField = new JTextField(10); panel.add(adminDeleteMessageIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        adminDeleteMessageButton = new JButton("Delete Message (Admin) (100)");
        adminDeleteMessageButton.addActionListener(e -> sendAdminDeleteMessageRequest());
        panel.add(adminDeleteMessageButton, gbc);

        // New admin button for 110
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        adminListAllUsersButton = new JButton("List All Users (Admin) (110)");
        adminListAllUsersButton.addActionListener(e -> sendAdminListAllUsersRequest());
        panel.add(adminListAllUsersButton, gbc);

        return panel;
    }

    private void updateGUIState() {
        boolean isLoggedIn = connection.isConnected() && currentToken != null;

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
        getAllTopicsButton.setEnabled(isLoggedIn);

        replyTopicIdField.setEnabled(isLoggedIn);
        replyMessageArea.setEnabled(isLoggedIn);
        sendReplyButton.setEnabled(isLoggedIn);

        getRepliesTopicIdField.setEnabled(isLoggedIn);
        getRepliesButton.setEnabled(isLoggedIn);

        boolean isAdmin = isLoggedIn && currentToken != null && currentToken.startsWith("a");
        adminTargetUserField.setEnabled(isAdmin);
        adminNewNickField.setEnabled(isAdmin);
        adminNewPassField.setEnabled(isAdmin);
        adminUpdateUserButton.setEnabled(isAdmin);
        adminDeleteUserButton.setEnabled(isAdmin);
        adminDeleteMessageIdField.setEnabled(isAdmin);
        adminDeleteMessageButton.setEnabled(isAdmin);
        adminListAllUsersButton.setEnabled(isAdmin); // Enable/disable for 110

        if (!isLoggedIn) {
            adminTargetUserField.setText("");
            adminNewNickField.setText("");
            adminNewPassField.setText("");
            adminDeleteMessageIdField.setText("");
        }
    }

    private void handleReceivedMessage(ProtocolMessage message) {
        SwingUtilities.invokeLater(() -> {
            String opCode = message.getOperationCode();
            String messageDetails = message.getMessageContent();

            switch (opCode) {
                case "001": // Login Success
                    currentToken = message.getToken();
                    currentUsername = message.getUser() != null ? message.getUser() : usernameField.getText();
                    connected.set(true);
                    updateGUIState();
                    appendReceivedMessage("Login successful! Welcome, " + currentUsername + ".");
                    mainTabbedPane.setSelectedIndex(0); // Switch to the "Create Topic" tab
                    break;
                case "002": // Login Error
                    appendReceivedMessage("SERVER ERROR (002 - Login Failed): " + messageDetails);
                    connected.set(false);
                    currentToken = null;
                    currentUsername = null;
                    connection.disconnect(); // Socket remains active until app exit
                    updateGUIState();
                    break;
                case "006": // Retrieve User Data Success
                    String retrievedUser = message.getUser();
                    String retrievedNickname = message.getNickname();
                    appendReceivedMessage("\n--- User Data Retrieved (006) ---");
                    appendReceivedMessage("Username: " + retrievedUser);
                    appendReceivedMessage("Nickname: " + retrievedNickname);
                    appendReceivedMessage("----------------------------------\n");
                    break;
                case "007": appendReceivedMessage("SERVER ERROR (007 - Get User Data Failed): " + messageDetails); break;
                case "011": // Register Success
                    appendReceivedMessage("Registration successful! You can now log in. " + messageDetails); // Use msg field
                    usernameField.setText("");
                    passwordField.setText("");
                    nicknameField.setText("");
                    break;
                case "012": appendReceivedMessage("SERVER ERROR (012 - Registration Failed): " + messageDetails); break;
                case "021": // Logout Success
                    currentToken = null;
                    currentUsername = null;
                    connected.set(false);
                    // connection.disconnect(); // Socket remains active until app exit, as per protocol
                    updateGUIState();
                    appendReceivedMessage("Logged out successfully. " + messageDetails); // Use msg field
                    mainTabbedPane.setSelectedIndex(0);
                    break;
                case "022": appendReceivedMessage("SERVER ERROR (022 - Logout Failed): " + messageDetails); break;
                case "031": // Update Profile Success
                    appendReceivedMessage("Profile updated successfully! " + messageDetails); // Use msg field
                    newPasswordField.setText("");
                    nicknameField.setText("");
                    break;
                case "032": appendReceivedMessage("SERVER ERROR (032 - Profile Update Failed): " + messageDetails); break;
                case "041": // Delete Account Success
                    currentToken = null;
                    currentUsername = null;
                    connected.set(false);
                    // connection.disconnect(); // Socket remains active until app exit, as per protocol
                    updateGUIState();
                    appendReceivedMessage("Account deleted successfully. " + messageDetails); // Use msg field
                    usernameField.setText("");
                    passwordField.setText("");
                    mainTabbedPane.setSelectedIndex(0);
                    break;
                case "042": appendReceivedMessage("SERVER ERROR (042 - Account Deletion Failed): " + messageDetails); break;
                case "051": // Create Topic Success
                    appendReceivedMessage("Forum topic created successfully! " + messageDetails); // Use msg field
                    topicTitleField.setText("");
                    topicSubjectField.setText("");
                    topicMessageArea.setText("");
                    break;
                case "052": appendReceivedMessage("SERVER ERROR (052 - Topic Creation Failed): " + messageDetails); break;
                case "055": // Forum Topic Broadcast (from server to all clients)
                    appendReceivedMessage("\n--- NEW FORUM TOPIC (055) ---");
                    appendReceivedMessage("ID: " + message.getTopicId());
                    appendReceivedMessage("Title: " + message.getTopicTitle());
                    appendReceivedMessage("Subject: " + message.getTopicSubject());
                    appendReceivedMessage("Author: " + message.getTopicAuthor());
                    appendReceivedMessage("Content:\n" + message.getTopicContent());
                    appendReceivedMessage("---------------------------\n");
                    break;
                case "061": // Send Reply Success
                    appendReceivedMessage("Reply sent successfully! " + messageDetails); // Use msg field
                    replyTopicIdField.setText("");
                    replyMessageArea.setText("");
                    break;
                case "062": appendReceivedMessage("SERVER ERROR (062 - Send Reply Failed): " + messageDetails); break;
                case "071": // Get Replies Response
                    List<Map<String, String>> replies = message.getMessageList();
                    if (replies != null && !replies.isEmpty()) {
                        appendReceivedMessage("\n--- TOPIC REPLIES (071) ---");
                        for (Map<String, String> reply : replies) {
                            appendReceivedMessage(
                                    "ID: " + reply.get("id") +
                                            ", Author: " + reply.get("nick") +
                                            ", Content: " + reply.get("msg")
                            );
                        }
                        appendReceivedMessage("-----------------------------------\n");
                    } else {
                        appendReceivedMessage("\n--- No replies found for the topic. ---");
                    }
                    getRepliesTopicIdField.setText("");
                    break;
                case "072": appendReceivedMessage("SERVER ERROR (072 - Get Replies Failed): " + messageDetails); break;
                case "076": // Get Topics Response (075)
                    List<Map<String, String>> topics = message.getMessageList();
                    if (topics != null && !topics.isEmpty()) {
                        appendReceivedMessage("\n--- FORUM TOPICS (076) ---");
                        for (Map<String, String> topic : topics) {
                            appendReceivedMessage(
                                    "ID: " + topic.get("id") +
                                            ", Title: " + topic.get("title") +
                                            ", Subject: " + topic.get("subject") +
                                            ", Author: " + topic.get("author")
                            );
                            appendReceivedMessage("   Content: " + topic.get("msg"));
                        }
                        appendReceivedMessage("-------------------------------\n");
                    } else {
                        appendReceivedMessage("\n--- No topics found. ---");
                    }
                    break;
                case "077": appendReceivedMessage("SERVER ERROR (077 - Get Topics Failed): " + messageDetails); break;
                case "081": // Admin Update User Success
                    appendReceivedMessage("User profile updated by admin successfully! " + messageDetails); // Use msg field
                    adminTargetUserField.setText("");
                    adminNewNickField.setText("");
                    adminNewPassField.setText("");
                    break;
                case "082": appendReceivedMessage("SERVER ERROR (082 - Admin Update User Failed): " + messageDetails); break;
                case "091": // Admin Delete User Success
                    appendReceivedMessage("User account deleted by admin successfully! " + messageDetails); // Use msg field
                    adminTargetUserField.setText("");
                    break;
                case "092": appendReceivedMessage("SERVER ERROR (092 - Admin Delete User Failed): " + messageDetails); break;
                case "101": // Admin Delete Message Success
                    appendReceivedMessage("Message/Topic deleted by admin successfully! " + messageDetails); // Use msg field
                    adminDeleteMessageIdField.setText("");
                    break;
                case "102": appendReceivedMessage("SERVER ERROR (102 - Admin Delete Message Failed): " + messageDetails); break;
                case "111": // List All Users Response (Admin)
                    List<String> userList = message.getUserList(); // Use getUserList
                    if (userList != null && !userList.isEmpty()) {
                        appendReceivedMessage("\n--- ALL USERS (111) ---");
                        for (String userEntry : userList) {
                            appendReceivedMessage("- " + userEntry);
                        }
                        appendReceivedMessage("---------------------------\n");
                    } else {
                        appendReceivedMessage("\n--- No users found. ---");
                    }
                    break;
                case "112": appendReceivedMessage("SERVER ERROR (112 - List All Users Failed): " + messageDetails); break;
                case "999": appendReceivedMessage("SERVER ERROR (999 - Unknown Operation): " + messageDetails); break;
                default: appendReceivedMessage("Unknown server response: " + opCode + " - " + messageDetails);
            }
        });
    }

    // --- Request Sending Methods ---

    private void sendLoginRequest() {
        try {
            String host = serverHostField.getText().trim();
            int port = Integer.parseInt(serverPortField.getText().trim());

            if (!connection.isConnected()) {
                connection.connect(host, port);
            }

            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();

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
            appendReceivedMessage("Input error: Invalid port number.");
            connected.set(false);
            updateGUIState();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Connection setup error: " + e.getMessage());
            connected.set(false);
            updateGUIState();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending login request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending login request: " + e.getMessage());
        }
    }

    private void sendRegisterRequest() {
        try {
            String host = serverHostField.getText().trim();
            int port = Integer.parseInt(serverPortField.getText().trim());

            if (!connection.isConnected()) {
                connection.connect(host, port);
            }

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
            connection.sendMessage(registerMsg);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Input error: Invalid port number.");
        } catch (IOException e) {
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
            connection.sendMessage(logoutMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending logout request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending logout request: " + e.getMessage());
        }
    }

    private void sendUpdateProfileRequest() {
        if (!connected.get() || currentUsername == null || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to update your profile.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();
            String newNick = nicknameField.getText().trim();
            String newPass = new String(newPasswordField.getPassword()).trim();

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
            connection.sendMessage(deleteMsg);
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
            connection.sendMessage(topicMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending topic creation request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending topic creation request: " + e.getMessage());
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
            retrieveUserMsg.setUser(userId);
            connection.sendMessage(retrieveUserMsg);
            appendReceivedMessage("Sending retrieve user data request (005) for user: " + userId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending retrieve user data request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending retrieve user data request: " + e.getMessage());
        }
    }

    private void sendReplyMessageRequest() {
        if (!connected.get() || currentToken == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to reply to a topic.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String topicId = replyTopicIdField.getText().trim();
            String msgContent = replyMessageArea.getText().trim();

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
            appendReceivedMessage("Error sending reply request: " + e.getMessage());
        }
    }

    private void sendGetRepliesRequest() {
        if (!connection.isConnected()) {
            JOptionPane.showMessageDialog(this, "You must be connected to get replies.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String topicId = getRepliesTopicIdField.getText().trim();

            if (topicId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Topic ID cannot be empty to get replies.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage getRepliesMsg = new ProtocolMessage("070");
            getRepliesMsg.setId(topicId);
            connection.sendMessage(getRepliesMsg);
            appendReceivedMessage("Sending get replies request (070) for topic: " + topicId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending get replies request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending get replies request: " + e.getMessage());
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
            appendReceivedMessage("Sending get all topics request (075).");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending get all topics request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending get all topics request: " + e.getMessage());
        }
    }

    private void sendAdminUpdateUserRequest() {
        if (!connected.get() || currentToken == null || !currentToken.startsWith("a")) {
            JOptionPane.showMessageDialog(this, "You must be logged in as an administrator to perform this action.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String targetUser = adminTargetUserField.getText().trim();
            String newNick = adminNewNickField.getText().trim();
            String newPass = new String(adminNewPassField.getPassword()).trim();

            if (targetUser.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Target username cannot be empty for admin update.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (newNick.isEmpty() && newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a new nickname or a new password for admin update.", "Input Error", JOptionPane.WARNING_MESSAGE);
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
            appendReceivedMessage("Error sending admin update user request: " + e.getMessage());
        }
    }

    private void sendAdminDeleteUserRequest() {
        if (!connected.get() || currentToken == null || !currentToken.startsWith("a")) {
            JOptionPane.showMessageDialog(this, "You must be logged in as an administrator to perform this action.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user account? This action cannot be undone.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String targetUser = adminTargetUserField.getText().trim();

            if (targetUser.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Target username cannot be empty for admin delete.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage adminDeleteUserMsg = new ProtocolMessage("090");
            adminDeleteUserMsg.setToken(currentToken);
            adminDeleteUserMsg.setUser(targetUser);
            connection.sendMessage(adminDeleteUserMsg);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending admin delete user request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending admin delete user request: " + e.getMessage());
        }
    }

    private void sendAdminDeleteMessageRequest() {
        if (!connected.get() || currentToken == null || !currentToken.startsWith("a")) {
            JOptionPane.showMessageDialog(this, "You must be logged in as an administrator to perform this action.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this message/topic? It will be replaced by 'Mensagem Apagada'.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String messageId = adminDeleteMessageIdField.getText().trim();

            if (messageId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Message/Topic ID cannot be empty for admin delete.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage adminDeleteMessageMsg = new ProtocolMessage("100");
            adminDeleteMessageMsg.setToken(currentToken);
            adminDeleteMessageMsg.setId(messageId);
            connection.sendMessage(adminDeleteMessageMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending admin delete message request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending admin delete message request: " + e.getMessage());
        }
    }

    private void sendAdminListAllUsersRequest() { // New method for opcode 110
        if (!connected.get() || currentToken == null || !currentToken.startsWith("a")) {
            JOptionPane.showMessageDialog(this, "You must be logged in as an administrator to list all users.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            ProtocolMessage listUsersMsg = new ProtocolMessage("110");
            listUsersMsg.setToken(currentToken);
            connection.sendMessage(listUsersMsg);
            appendReceivedMessage("Sending list all users request (110).");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error sending list all users request: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            appendReceivedMessage("Error sending list all users request: " + e.getMessage());
        }
    }

    private void appendReceivedMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            receivedMessagesArea.append("[" + new java.util.Date() + "] " + message + "\n");
            receivedMessagesArea.setCaretPosition(receivedMessagesArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientApp());
    }
}