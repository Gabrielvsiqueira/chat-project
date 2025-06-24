package server;

import common.ClientInfo;
import server.model.Topic;
import server.model.User;
import server.repository.ReplyRepository;
import server.repository.TopicRepository;
import server.repository.UserRepository;
import server.service.AdminHandler;
import server.service.AuthHandler;
import server.service.ProfileHandler;
import server.service.TopicHandler;
import server.service.UserDataHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerApp extends JFrame {
    private ServerSocket serverSocket;
    private volatile boolean running;
    private int port;

    // Repositories
    private UserRepository userRepository;
    private TopicRepository topicRepository;
    private ReplyRepository replyRepository;

    // Client Management Maps
    private Map<String, ClientInfo> authenticatedUsers; // token -> ClientInfo
    private List<ClientHandler> connectedClientHandlers; // All active client handlers
    private Map<String, ObjectOutputStream> activeClientOutputs; // token -> ObjectOutputStream for broadcast

    // Service Handlers
    private AuthHandler authHandler;
    private ProfileHandler profileHandler;
    private TopicHandler topicHandler;
    private UserDataHandler userDataHandler;
    private AdminHandler adminHandler;

    private DefaultListModel<ClientInfo> listModel;
    private JList<ClientInfo> clientList;
    private JTextArea logArea;

    public ServerApp() {
        // Initialize Repositories
        userRepository = new UserRepository();
        topicRepository = new TopicRepository();
        replyRepository = new ReplyRepository();

        // Initialize Client Management Structures
        authenticatedUsers = new ConcurrentHashMap<>();
        connectedClientHandlers = new CopyOnWriteArrayList<>();
        activeClientOutputs = new ConcurrentHashMap<>();

        // Initialize Service Handlers with their dependencies and callbacks
        authHandler = new AuthHandler(userRepository, authenticatedUsers, this::logMessage, this::updateClientListGUI);
        topicHandler = new TopicHandler(topicRepository, replyRepository, authHandler, this::logMessage, activeClientOutputs);
        profileHandler = new ProfileHandler(userRepository, authHandler, this::logMessage, this::updateClientListGUI);
        userDataHandler = new UserDataHandler(userRepository, authHandler, this::logMessage);
        adminHandler = new AdminHandler(userRepository, topicRepository, replyRepository, authHandler, this::logMessage, this::updateClientListGUI);

        initializeGUI();
        askForPort();
        startServer();
    }

    private void initializeGUI() {
        setTitle("TCP Server (Forum)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(700, 500);
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
                stopServer();
                System.exit(0);
            }
        });

        setVisible(true);
    }

    private void askForPort() {
        String portStr = JOptionPane.showInputDialog(this, "Enter server port:", "Server Port", JOptionPane.QUESTION_MESSAGE, null, null, "12345").toString();
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
            serverSocket = new ServerSocket(port);
            running = true;
            logMessage("Server started on port " + port);

            Thread acceptThread = new Thread(this::acceptClientsLoop);
            acceptThread.setDaemon(true);
            acceptThread.start();

        } catch (IOException e) {
            logMessage("Error starting server: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error starting server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void acceptClientsLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(
                        clientSocket,
                        this::logMessage,
                        this::updateClientListGUI, // Callback to update GUI list
                        this::removeClientHandler, // Callback to remove handler on disconnect
                        authHandler,
                        profileHandler,
                        topicHandler,
                        userDataHandler,
                        adminHandler,
                        activeClientOutputs
                );
                connectedClientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            } catch (SocketException e) {
                if (running) {
                    logMessage("Server socket closed unexpectedly: " + e.getMessage());
                }
            } catch (IOException e) {
                logMessage("Error accepting client connection: " + e.getMessage());
            } catch (Exception e) {
                logMessage("Unexpected error in accept loop: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Callback to update the GUI client list
    private void updateClientListGUI(ClientInfo clientInfo) {
        SwingUtilities.invokeLater(() -> {
            // Remove the client from the list if it's no longer authenticated or its token changed,
            // then re-add or just update. This logic ensures accurate representation.
            if (!listModel.contains(clientInfo)) {
                // Check if an existing entry needs updating
                boolean found = false;
                for (int i = 0; i < listModel.getSize(); i++) {
                    if (listModel.getElementAt(i).equals(clientInfo)) { // Compares by IP:Port
                        listModel.setElementAt(clientInfo, i); // Update existing element
                        found = true;
                        break;
                    }
                }
                if (!found) { // Add if not found
                    listModel.addElement(clientInfo);
                }
            } else { // If already in list, just repaint to ensure name changes are reflected
                clientList.repaint();
            }

            // Also, remove guests from list if they become authenticated and are no longer listed as guest by IP:Port
            // This is handled by the ClientHandler's key management in activeClientOutputs.
            // The `clientInfo.setUserId(null); clientInfo.setToken(null);` on logout/delete will make equals/hashCode
            // effectively remove the old entry if it's not authenticated anymore by ID, but `equals` is still by IP/Port.
            // A more robust solution might involve specific removal and re-addition if the "identity" changes,
            // or clearing and repopulating the list based on `authenticatedUsers` map.
            // For now, `repaint()` and `removeElement` in `removeClientHandler` help.
        });
    }

    // Callback to remove a ClientHandler from the list when it disconnects
    private void removeClientHandler(ClientHandler handler) {
        connectedClientHandlers.remove(handler);
        SwingUtilities.invokeLater(() -> {
            listModel.removeElement(handler.getClientInfo()); // Remove the client from GUI list
            clientList.repaint();
        });
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
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler handler : connectedClientHandlers) {
                handler.stop();
            }
            connectedClientHandlers.clear();
            authenticatedUsers.clear();
            activeClientOutputs.clear();

            logMessage("Server stopped.");
        } catch (IOException e) {
            logMessage("Error stopping server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerApp());
    }
}