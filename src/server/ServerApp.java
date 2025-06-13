package server;

import common.ClientInfo;
import server.model.Topic;
import server.model.User;
import server.repository.ReplyRepository; // Importar novo repositório
import server.repository.TopicRepository;
import server.repository.UserRepository;
import server.service.AdminHandler; // Importar novo handler
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

    // Repositórios de dados
    private UserRepository userRepository;
    private TopicRepository topicRepository;
    private ReplyRepository replyRepository; // Novo repositório

    // Mapas para gerenciar clientes
    private Map<String, ClientInfo> authenticatedUsers; // token -> ClientInfo
    private List<ClientHandler> connectedClientHandlers;
    private Map<String, ObjectOutputStream> activeClientOutputs; // token -> ObjectOutputStream para broadcast

    // Handlers de serviço
    private AuthHandler authHandler;
    private ProfileHandler profileHandler;
    private TopicHandler topicHandler;
    private UserDataHandler userDataHandler;
    private AdminHandler adminHandler; // Novo handler de admin

    private DefaultListModel<ClientInfo> listModel;
    private JList<ClientInfo> clientList;
    private JTextArea logArea;

    public ServerApp() {
        // Inicializa repositórios
        userRepository = new UserRepository();
        topicRepository = new TopicRepository();
        replyRepository = new ReplyRepository(); // Inicializa novo repositório

        // Inicializa estruturas de dados para clientes
        authenticatedUsers = new ConcurrentHashMap<>();
        connectedClientHandlers = new CopyOnWriteArrayList<>();
        activeClientOutputs = new ConcurrentHashMap<>();

        // Inicializa handlers de serviço com suas dependências e callbacks
        authHandler = new AuthHandler(userRepository, authenticatedUsers, this::logMessage, this::updateClientListGUI);
        topicHandler = new TopicHandler(topicRepository, replyRepository, authHandler, this::logMessage, activeClientOutputs); // Injeta replyRepository
        profileHandler = new ProfileHandler(userRepository, authHandler, this::logMessage, this::updateClientListGUI);
        userDataHandler = new UserDataHandler(userRepository, authHandler, this::logMessage);
        adminHandler = new AdminHandler(userRepository, topicRepository, replyRepository, authHandler, this::logMessage, this::updateClientListGUI); // Inicializa novo handler

        initializeGUI();
        askForPort();
        startServer();
    }

    private void updateClientListGUI(ClientInfo clientInfo) {
    }

    private void logMessage(String message) {
        String logEntry = "[" + new java.util.Date() + "] " + message; // Garante timestamp
        System.out.println(logEntry);
        SwingUtilities.invokeLater(() -> {
            logArea.append(logEntry + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // This method needs to be copied into your ServerApp.java
    private void initializeGUI() {
        setTitle("TCP Server (Forum)"); // Title updated
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
    // Este método precisa ser copiado para dentro da sua classe ServerApp.java
    /**
     * Para o servidor, fechando o ServerSocket e interrompendo todos os ClientHandlers.
     */
    private void stopServer() {
        running = false; // Sinaliza para o loop principal parar
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Fecha o ServerSocket, interrompendo o acceptClientsLoop
            }
            // Interrompe todos os ClientHandlers ativos
            for (ClientHandler handler : connectedClientHandlers) {
                handler.stop(); // Pede para o handler parar seu loop e fechar o socket do cliente
            }
            connectedClientHandlers.clear(); // Limpa a lista de handlers
            authenticatedUsers.clear(); // Limpa os usuários autenticados
            activeClientOutputs.clear(); // Limpa os outputs ativos

            logMessage("Server stopped.");
        } catch (IOException e) {
            logMessage("Error stopping server: " + e.getMessage());
        }
    }
    // This method also needs to be copied into your ServerApp.java
    private void askForPort() {
        String portStr = JOptionPane.showInputDialog(this, "Enter server port:", "Server Port", JOptionPane.QUESTION_MESSAGE, null, null, "12345").toString();
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

    // And this one too
    private void startServer() {
        try {
            serverSocket = new ServerSocket(port); // Cria o socket UDP na porta especificada
            running = true;
            logMessage("Server started on port " + port);

            // Inicia um novo thread para escutar mensagens UDP, para não bloquear a GUI
            Thread serverThread = new Thread(this::acceptClientsLoop); // Changed from serverLoop to acceptClientsLoop
            serverThread.setDaemon(true); // Define como daemon para que o thread termine com a aplicação
            serverThread.start();

        } catch (IOException e) { // Changed from SocketException to IOException as ServerSocket can throw it
            logMessage("Error starting server: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error starting server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    /**
     * Loop principal do servidor para aceitar novas conexões de clientes.
     */
    private void acceptClientsLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(
                        clientSocket,
                        this::logMessage,
                        this::updateClientListGUI,
                        this::removeClientHandler,
                        authHandler,
                        profileHandler,
                        topicHandler,
                        userDataHandler,
                        adminHandler, // Passa o novo handler de admin
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

    private void removeClientHandler(ClientHandler clientHandler) {
    }

    // ... (updateClientListGUI, removeClientHandler, logMessage, stopServer, main permanecem os mesmos)

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerApp());
    }
}