package client;

import common.Message;
import common.ClientInfo;
import common.MessageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.*;
import java.util.List;

public class UDPClient extends JFrame {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private String clientName;
    private boolean connected = false;
    private boolean running = false;

    // GUI components
    private JTextField serverHostField;
    private JTextField serverPortField;
    private JTextField clientNameField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JComboBox<String> clientComboBox;
    private JCheckBox broadcastCheckBox;
    private JTextArea messageArea;
    private JTextField messageField;
    private JButton sendButton;
    private JTextArea receivedMessagesArea;

    public UDPClient() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("UDP Client");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Connection panel
        JPanel connectionPanel = createConnectionPanel();
        mainPanel.add(connectionPanel, BorderLayout.NORTH);

        // common.Message panel
        JPanel messagePanel = createMessagePanel();
        mainPanel.add(messagePanel, BorderLayout.CENTER);

        add(mainPanel);

        // Window close event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connected) {
                    disconnect();
                }
                System.exit(0);
            }
        });

        setVisible(true);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Server Connection"));
        GridBagConstraints gbc = new GridBagConstraints();

        // Server host
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel("Server Host:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        serverHostField = new JTextField("localhost", 15);
        panel.add(serverHostField, gbc);

        // Server port
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 3;
        serverPortField = new JTextField("12345", 8);
        panel.add(serverPortField, gbc);

        // Client name
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Your Name:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        clientNameField = new JTextField(15);
        panel.add(clientNameField, gbc);

        // Connect button
        gbc.gridx = 2; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connect());
        panel.add(connectButton, gbc);

        // Disconnect button
        gbc.gridy = 2;
        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> disconnect());
        disconnectButton.setEnabled(false);
        panel.add(disconnectButton, gbc);

        return panel;
    }

    private JPanel createMessagePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Target selection panel
        JPanel targetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetPanel.setBorder(BorderFactory.createTitledBorder("common.Message Target"));

        targetPanel.add(new JLabel("Send to:"));
        clientComboBox = new JComboBox<>();
        clientComboBox.setPreferredSize(new Dimension(200, 25));
        clientComboBox.setEnabled(false);
        targetPanel.add(clientComboBox);

        broadcastCheckBox = new JCheckBox("Broadcast to all");
        broadcastCheckBox.setEnabled(false);
        broadcastCheckBox.addActionListener(e -> clientComboBox.setEnabled(!broadcastCheckBox.isSelected()));
        targetPanel.add(broadcastCheckBox);

        panel.add(targetPanel, BorderLayout.NORTH);

        // common.Message composition panel
        JPanel composePanel = new JPanel(new BorderLayout());
        composePanel.setBorder(BorderFactory.createTitledBorder("Send common.Message"));

        messageField = new JTextField();
        messageField.setEnabled(false);
        messageField.addActionListener(e -> sendMessage());
        composePanel.add(messageField, BorderLayout.CENTER);

        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());
        composePanel.add(sendButton, BorderLayout.EAST);

        panel.add(composePanel, BorderLayout.CENTER);

        // Received messages panel
        JPanel receivedPanel = new JPanel(new BorderLayout());
        receivedPanel.setBorder(BorderFactory.createTitledBorder("Received Messages"));

        receivedMessagesArea = new JTextArea(10, 0);
        receivedMessagesArea.setEditable(false);
        receivedMessagesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(receivedMessagesArea);
        receivedPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(receivedPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void connect() {
        try {
            // Validate input
            String host = serverHostField.getText().trim();
            String portStr = serverPortField.getText().trim();
            clientName = clientNameField.getText().trim();

            if (host.isEmpty() || portStr.isEmpty() || clientName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            serverPort = Integer.parseInt(portStr);
            serverAddress = InetAddress.getByName(host);

            // Create socket
            socket = new DatagramSocket();

            // Send connection message
            Message connectMsg = new Message(Message.Type.CLIENT_CONNECT, clientName);
            sendMessageToServer(connectMsg);

            // Start listening thread
            running = true;
            Thread listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            // Update GUI
            connected = true;
            updateGUIState();
            appendReceivedMessage("Connected to server at " + host + ":" + serverPort);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnect() {
        if (connected) {
            try {
                // Send disconnect message
                Message disconnectMsg = new Message(Message.Type.CLIENT_DISCONNECT, clientName);
                sendMessageToServer(disconnectMsg);
            } catch (Exception e) {
                System.err.println("Error sending disconnect message: " + e.getMessage());
            }
        }

        // Clean up
        running = false;
        connected = false;

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        // Update GUI
        updateGUIState();
        clientComboBox.removeAllItems();
        appendReceivedMessage("Disconnected from server");
    }

    private void sendMessage() {
        if (!connected) return;

        String messageText = messageField.getText().trim();
        if (messageText.isEmpty()) return;

        try {
            Message message = new Message(Message.Type.SEND_MESSAGE, clientName);
            message.setContent(messageText);

            if (broadcastCheckBox.isSelected()) {
                message.setBroadcast(true);
                appendReceivedMessage("You (broadcast): " + messageText);
            } else {
                String targetClient = (String) clientComboBox.getSelectedItem();
                if (targetClient == null) {
                    JOptionPane.showMessageDialog(this, "Please select a target client", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                message.setTargetClient(targetClient);
                message.setBroadcast(false);
                appendReceivedMessage("You to " + targetClient + ": " + messageText);
            }

            sendMessageToServer(message);
            messageField.setText("");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to send message: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessageToServer(Message message) throws IOException {
        byte[] data = MessageUtils.serializeMessage(message);
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
        socket.send(packet);
    }

    private void listenForMessages() {
        byte[] buffer = new byte[8192];

        while (running && connected) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Message message = MessageUtils.deserializeMessage(packet);
                handleReceivedMessage(message);

            } catch (IOException | ClassNotFoundException e) {
                if (running && connected) {
                    SwingUtilities.invokeLater(() ->
                            appendReceivedMessage("Error receiving message: " + e.getMessage()));
                }
            }
        }
    }

    private void handleReceivedMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case CLIENT_LIST_UPDATE:
                    updateClientList(message.getClientList());
                    break;
                case MESSAGE_RECEIVED:
                    appendReceivedMessage(message.getClientName() + ": " + message.getContent());
                    break;
                default:
                    appendReceivedMessage("Unknown message type: " + message.getType());
            }
        });
    }

    private void updateClientList(List<ClientInfo> clients) {
        clientComboBox.removeAllItems();

        for (ClientInfo client : clients) {
            if (!client.getName().equals(clientName)) {
                clientComboBox.addItem(client.getName());
            }
        }

        appendReceivedMessage("Client list updated. " + clients.size() + " clients connected.");
    }

    private void appendReceivedMessage(String message) {
        receivedMessagesArea.append("[" + new java.util.Date() + "] " + message + "\n");
        receivedMessagesArea.setCaretPosition(receivedMessagesArea.getDocument().getLength());
    }

    private void updateGUIState() {
        // Connection controls
        serverHostField.setEnabled(!connected);
        serverPortField.setEnabled(!connected);
        clientNameField.setEnabled(!connected);
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);

        // common.Message controls
        clientComboBox.setEnabled(connected && !broadcastCheckBox.isSelected());
        broadcastCheckBox.setEnabled(connected);
        messageField.setEnabled(connected);
        sendButton.setEnabled(connected);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UDPClient());
    }
}