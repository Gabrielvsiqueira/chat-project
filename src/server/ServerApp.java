package server;

import common.ClientInfo;
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
import java.io.PrintWriter; // Importa a classe correta
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

    // Repositórios
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;

    // Gerenciamento de Clientes
    private final Map<String, ClientInfo> authenticatedUsers;
    private final List<ClientHandler> connectedClientHandlers;
    // *** CORREÇÃO APLICADA AQUI ***
    // O mapa agora armazena PrintWriter, não ObjectOutputStream
    private final Map<String, PrintWriter> activeClientOutputs;

    // Handlers de Serviço
    private final AuthHandler authHandler;
    private final ProfileHandler profileHandler;
    private final TopicHandler topicHandler;
    private final UserDataHandler userDataHandler;
    private final AdminHandler adminHandler;

    // Componentes da GUI
    private DefaultListModel<ClientInfo> listModel;
    private JList<ClientInfo> clientList;
    private JTextArea logArea;

    public ServerApp() {
        // Inicializa Repositórios
        userRepository = new UserRepository();
        topicRepository = new TopicRepository();
        replyRepository = new ReplyRepository();

        // Inicializa Estruturas de Gerenciamento de Clientes
        authenticatedUsers = new ConcurrentHashMap<>();
        connectedClientHandlers = new CopyOnWriteArrayList<>();
        // *** CORREÇÃO APLICADA AQUI ***
        activeClientOutputs = new ConcurrentHashMap<>(); // Inicializa o mapa com o tipo correto

        // Inicializa Handlers de Serviço com suas dependências e callbacks
        // *** CORREÇÃO APLICADA AQUI ***
        // Passa o mapa 'activeClientOutputs' (que agora é do tipo PrintWriter) para os handlers
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
                // *** CORREÇÃO APLICADA AQUI ***
                // O construtor do ClientHandler recebe o mapa com o tipo correto
                ClientHandler clientHandler = new ClientHandler(
                        clientSocket,
                        this::logMessage,
                        this::updateClientListGUI,
                        this::removeClientHandler,
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
                if(running) logMessage("Error accepting client connection: " + e.getMessage());
            } catch (Exception e) {
                if(running) {
                    logMessage("Unexpected error in accept loop: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateClientListGUI(ClientInfo clientInfo) {
        SwingUtilities.invokeLater(() -> {
            boolean found = false;
            for (int i = 0; i < listModel.getSize(); i++) {
                if (listModel.getElementAt(i).equals(clientInfo)) {
                    listModel.setElementAt(clientInfo, i);
                    found = true;
                    break;
                }
            }
            if (!found && clientInfo.getUserId() != null) {
                listModel.addElement(clientInfo);
            }
            clientList.repaint();
        });
    }

    private void removeClientHandler(ClientHandler handler) {
        connectedClientHandlers.remove(handler);
        SwingUtilities.invokeLater(() -> {
            listModel.removeElement(handler.getClientInfo());
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
        SwingUtilities.invokeLater(ServerApp::new);
    }
}