package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import common.Message;
import common.ClientInfo;
import common.MessageUtils;

public class UDPServer extends JFrame {
    private DatagramSocket socket;
    private boolean running;
    private int port;
    private List<ClientInfo> connectedClients;
    private DefaultListModel<ClientInfo> listModel;
    private JList<ClientInfo> clientList;
    private JTextArea logArea;

    public UDPServer() {
        connectedClients = new CopyOnWriteArrayList<>();
        initializeGUI();
        askForPort();
        startServer();
    }

    private void initializeGUI() {
        setTitle("UDP Server");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Client list
        listModel = new DefaultListModel<>();
        clientList = new JList<>(listModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        clientScrollPane.setBorder(BorderFactory.createTitledBorder("Connected Clients"));
        clientScrollPane.setPreferredSize(new Dimension(250, 0));

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Server Log"));

        mainPanel.add(clientScrollPane, BorderLayout.WEST);
        mainPanel.add(logScrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // Window close event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
                System.exit(0);
            }
        });

        setVisible(true);
    }

    private void askForPort() {
        String portStr = JOptionPane.showInputDialog(this, "Enter server port:", "Server Port", JOptionPane.QUESTION_MESSAGE);
        try {
            port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number. Using default port 12345.", "Warning", JOptionPane.WARNING_MESSAGE);
            port = 12345;
        }
    }

    private void startServer() {
        try {
            socket = new DatagramSocket(port);
            running = true;
            logMessage("Server started on port " + port);

            Thread serverThread = new Thread(this::serverLoop);
            serverThread.setDaemon(true);
            serverThread.start();

        } catch (SocketException e) {
            logMessage("Error starting server: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error starting server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void serverLoop() {
        byte[] buffer = new byte[8192];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Message message = MessageUtils.deserializeMessage(packet);
                handleMessage(message, packet.getAddress(), packet.getPort());

            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    logMessage("Error receiving message: " + e.getMessage());
                }
            }
        }
    }

    private void handleMessage(Message message, InetAddress clientAddress, int clientPort) {
        switch (message.getType()) {
            case CLIENT_CONNECT:
                handleClientConnect(message, clientAddress, clientPort);
                break;
            case SEND_MESSAGE:
                handleSendMessage(message, clientAddress, clientPort);
                break;
            case CLIENT_DISCONNECT:
                handleClientDisconnect(message, clientAddress, clientPort);
                break;
            default:
                logMessage("Unknown message type received: " + message.getType());
        }
    }

    private void handleClientConnect(Message message, InetAddress clientAddress, int clientPort) {
        ClientInfo clientInfo = new ClientInfo(message.getClientName(), clientAddress, clientPort);

        if (!connectedClients.contains(clientInfo)) {
            connectedClients.add(clientInfo);
            SwingUtilities.invokeLater(() -> {
                listModel.addElement(clientInfo);
                logMessage("Client connected: " + clientInfo);
            });

            // Send updated client list to all clients
            broadcastClientList();
        }
    }

    private void handleSendMessage(Message message, InetAddress senderAddress, int senderPort) {
        ClientInfo sender = findClient(senderAddress, senderPort);

        if (sender == null) {
            sendErrorMessage("You are not connected to the server", senderAddress, senderPort);
            return;
        }

        if (message.isBroadcast()) {
            // Broadcast to all clients except sender
            Message broadcastMsg = new Message(Message.Type.MESSAGE_RECEIVED);
            broadcastMsg.setClientName(sender.getName());
            broadcastMsg.setContent(message.getContent());

            for (ClientInfo client : connectedClients) {
                if (!client.equals(sender)) {
                    sendMessage(broadcastMsg, client.getAddress(), client.getPort());
                }
            }
            logMessage("Broadcast message from " + sender.getName() + ": " + message.getContent());
        } else {
            // Send to specific client
            ClientInfo target = findClientByName(message.getTargetClient());
            if (target != null) {
                Message targetMsg = new Message(Message.Type.MESSAGE_RECEIVED);
                targetMsg.setClientName(sender.getName());
                targetMsg.setContent(message.getContent());

                sendMessage(targetMsg, target.getAddress(), target.getPort());
                logMessage("common.Message from " + sender.getName() + " to " + target.getName() + ": " + message.getContent());
            } else {
                sendErrorMessage("Target client not found: " + message.getTargetClient(), senderAddress, senderPort);
            }
        }
    }

    private void handleClientDisconnect(Message message, InetAddress clientAddress, int clientPort) {
        ClientInfo clientToRemove = findClient(clientAddress, clientPort);

        if (clientToRemove != null) {
            connectedClients.remove(clientToRemove);
            SwingUtilities.invokeLater(() -> {
                listModel.removeElement(clientToRemove);
                logMessage("Client disconnected: " + clientToRemove);
            });

            // Send updated client list to remaining clients
            broadcastClientList();
        }
    }

    private void broadcastClientList() {
        Message listMessage = new Message(Message.Type.CLIENT_LIST_UPDATE);
        listMessage.setClientList(new ArrayList<>(connectedClients));

        for (ClientInfo client : connectedClients) {
            sendMessage(listMessage, client.getAddress(), client.getPort());
        }
    }

    private void sendMessage(Message message, InetAddress address, int port) {
        try {
            byte[] data = MessageUtils.serializeMessage(message);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            logMessage("Error sending message: " + e.getMessage());
        }
    }

    private void sendErrorMessage(String errorMsg, InetAddress address, int port) {
        Message message = new Message(Message.Type.MESSAGE_RECEIVED);
        message.setClientName("SERVER");
        message.setContent("ERROR: " + errorMsg);
        sendMessage(message, address, port);
    }

    private ClientInfo findClient(InetAddress address, int port) {
        for (ClientInfo client : connectedClients) {
            if (client.getAddress().equals(address) && client.getPort() == port) {
                return client;
            }
        }
        return null;
    }

    private ClientInfo findClientByName(String name) {
        for (ClientInfo client : connectedClients) {
            if (client.getName().equals(name)) {
                return client;
            }
        }
        return null;
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.util.Date() + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void stopServer() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        logMessage("Server stopped");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UDPServer());
    }
}