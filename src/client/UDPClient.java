package client;

import common.ProtocolMessage;
import common.ClientInfo; // Ainda pode ser útil para exibir informações de outros clientes, se necessário
import common.MessageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cliente UDP que interage com o servidor de fórum, enviando e recebendo
 * mensagens de protocolo JSON.
 * A GUI permite operações de login, registro, logout, atualização de perfil
 * e criação de tópicos.
 */
public class UDPClient extends JFrame {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private String currentUsername; // O nome de usuário atualmente logado
    private String currentToken; // O token de autenticação recebido do servidor
    private AtomicBoolean connected = new AtomicBoolean(false); // Estado de conexão com o servidor
    private AtomicBoolean running = new AtomicBoolean(false); // Estado do thread de escuta

    // Componentes da GUI
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

    private JTextArea receivedMessagesArea; // Área para exibir mensagens recebidas e logs do cliente

    /**
     * Construtor do cliente UDP. Inicializa a interface gráfica.
     */
    public UDPClient() {
        initializeGUI();
    }

    /**
     * Inicializa os componentes da interface gráfica do cliente.
     */
    private void initializeGUI() {
        setTitle("UDP Client (Forum)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(700, 600); // Tamanho aumentado para melhor visualização
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Painel de Conexão e Autenticação
        JPanel authPanel = createAuthPanel();
        mainPanel.add(authPanel, BorderLayout.NORTH);

        // Painel de Criação de Tópicos
        JPanel topicPanel = createTopicPanel();
        mainPanel.add(topicPanel, BorderLayout.CENTER);

        // Painel de Mensagens Recebidas / Log do Servidor
        JPanel receivedPanel = new JPanel(new BorderLayout());
        receivedPanel.setBorder(BorderFactory.createTitledBorder("Forum Messages / Server Log"));

        receivedMessagesArea = new JTextArea(10, 0);
        receivedMessagesArea.setEditable(false);
        receivedMessagesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(receivedMessagesArea);
        receivedPanel.add(scrollPane, BorderLayout.CENTER); // Centraliza a área de log

        mainPanel.add(receivedPanel, BorderLayout.SOUTH); // Coloca a área de log na parte inferior

        add(mainPanel);

        // Listener para o evento de fechamento da janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connected.get()) {
                    sendLogoutRequest(); // Tenta enviar logout antes de fechar
                }
                System.exit(0);
            }
        });

        updateGUIState(); // Define o estado inicial da GUI
        setVisible(true);
    }

    /**
     * Cria o painel de conexão e autenticação com campos de entrada e botões.
     *
     * @return O JPanel configurado.
     */
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

        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel("Password:"), gbc);
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

        // Botões de Ação
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

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; // Botão de exclusão ocupa 4 colunas
        deleteAccountButton = new JButton("Delete Account (040)");
        deleteAccountButton.addActionListener(e -> sendDeleteAccountRequest());
        panel.add(deleteAccountButton, gbc);

        return panel;
    }

    /**
     * Cria o painel para criação de tópicos de fórum.
     *
     * @return O JPanel configurado.
     */
    private JPanel createTopicPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Create Forum Topic (050)"));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Campo de Título do Tópico
        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        topicTitleField = new JTextField(20); inputPanel.add(topicTitleField, gbc);

        // Campo de Assunto do Tópico
        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        topicSubjectField = new JTextField(20); inputPanel.add(topicSubjectField, gbc);

        // Área de Mensagem do Tópico
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        inputPanel.add(new JLabel("Message Content:"), gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        topicMessageArea = new JTextArea(5, 20);
        topicMessageArea.setLineWrap(true); // Quebra de linha automática
        topicMessageArea.setWrapStyleWord(true); // Quebra de linha por palavra
        JScrollPane messageScrollPane = new JScrollPane(topicMessageArea);
        inputPanel.add(messageScrollPane, gbc);

        panel.add(inputPanel, BorderLayout.CENTER);

        // Botão de Criar Tópico
        createTopicButton = new JButton("Create Topic");
        createTopicButton.addActionListener(e -> sendCreateTopicRequest());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(createTopicButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // --- Gerenciamento de Estado da GUI ---

    /**
     * Atualiza o estado dos componentes da GUI (habilitado/desabilitado)
     * com base no estado de conexão e autenticação do cliente.
     */
    private void updateGUIState() {
        boolean isConnected = connected.get();

        // Campos de Conexão
        serverHostField.setEnabled(!isConnected);
        serverPortField.setEnabled(!isConnected);

        // Campos de Autenticação
        usernameField.setEnabled(!isConnected);
        passwordField.setEnabled(!isConnected);
        nicknameField.setEnabled(!isConnected); // Apelido só para registro
        newPasswordField.setEnabled(isConnected); // Nova senha só para update

        // Botões de Autenticação
        loginButton.setEnabled(!isConnected);
        registerButton.setEnabled(!isConnected);
        logoutButton.setEnabled(isConnected);
        updateProfileButton.setEnabled(isConnected);
        deleteAccountButton.setEnabled(isConnected);

        // Campos e Botão de Criação de Tópico
        topicTitleField.setEnabled(isConnected);
        topicSubjectField.setEnabled(isConnected);
        topicMessageArea.setEnabled(isConnected);
        createTopicButton.setEnabled(isConnected);
    }

    // --- Operações de Rede ---

    /**
     * Tenta estabelecer uma conexão com o servidor UDP.
     * Cria um DatagramSocket se ainda não estiver aberto e inicia o thread de escuta.
     *
     * @throws UnknownHostException Se o host do servidor não puder ser resolvido.
     * @throws SocketException      Se ocorrer um erro ao abrir o socket.
     */
    private void connectToServer() throws UnknownHostException, SocketException {
        String host = serverHostField.getText().trim();
        String portStr = serverPortField.getText().trim();

        if (host.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in server host and port.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        serverPort = Integer.parseInt(portStr);
        serverAddress = InetAddress.getByName(host); // Resolve o endereço IP do servidor

        // Cria o socket UDP do cliente (ele se ligará a uma porta efêmera disponível)
        socket = new DatagramSocket();

        // Inicia o thread de escuta se ainda não estiver rodando
        if (!running.get()) {
            running.set(true);
            Thread listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true); // Define como daemon para que o thread termine com a aplicação
            listenerThread.start();
        }
        appendReceivedMessage("Attempting to connect to server at " + host + ":" + serverPort);
    }

    /**
     * Envia uma mensagem do protocolo para o servidor.
     * Garante que o socket esteja inicializado antes de enviar.
     *
     * @param message O objeto ProtocolMessage a ser enviado.
     */
    private void sendMessageToServer(ProtocolMessage message) {
        try {
            // Garante que o socket esteja inicializado e não fechado
            if (socket == null || socket.isClosed()) {
                connectToServer(); // Tenta reconectar/inicializar o socket
            }

            byte[] data = MessageUtils.serializeMessage(message); // Serializa a mensagem para bytes JSON
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet); // Envia o pacote
        } catch (IOException e) {
            appendReceivedMessage("Error sending message: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to send message: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            appendReceivedMessage("Unexpected error before sending: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Unexpected error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loop principal do cliente para escutar mensagens do servidor.
     * Desserializa as mensagens e as encaminha para o manipulador.
     */
    private void listenForMessages() {
        byte[] buffer = new byte[8192]; // Buffer para receber dados do pacote

        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Bloqueia até que um pacote seja recebido

                ProtocolMessage message = MessageUtils.deserializeMessage(packet); // Desserializa a mensagem
                handleReceivedMessage(message); // Manipula a mensagem recebida

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

    /**
     * Manipula as mensagens de resposta recebidas do servidor.
     * Atualiza a GUI com base no código de operação da resposta.
     *
     * @param message A mensagem do protocolo recebida do servidor.
     */
    private void handleReceivedMessage(ProtocolMessage message) {
        SwingUtilities.invokeLater(() -> { // Garante que as atualizações da GUI sejam na EDT
            String opCode = message.getOperationCode();
            String msgContent = message.getMessageContent();
            String token = message.getToken();

            switch (opCode) {
                case "001": // Login Sucesso
                    currentToken = token;
                    // Usa o usuário da resposta se presente, senão o do campo de entrada
                    currentUsername = message.getUser() != null ? message.getUser() : usernameField.getText();
                    connected.set(true);
                    updateGUIState();
                    // Removido: appendReceivedMessage("Login successful! Welcome, " + currentUsername + ". Token: " + currentToken);
                    break;
                case "002": // Erro de Login
                case "012": // Erro de Registro
                case "022": // Erro de Logout
                case "032": // Erro de Atualização de Perfil
                case "042": // Erro de Exclusão de Conta
                case "052": // Erro de Criação de Tópico
                    appendReceivedMessage("SERVER ERROR (" + opCode + "): " + msgContent);
                    break;
                case "011": // Registro Sucesso
                    // Removido: appendReceivedMessage("Registration successful! You can now log in.");
                    break;
                case "021": // Logout Sucesso
                    currentToken = null;
                    currentUsername = null;
                    connected.set(false);
                    updateGUIState();
                    // Removido: appendReceivedMessage("Logged out successfully.");
                    break;
                case "031": // Atualização de Perfil Sucesso
                    // Removido: appendReceivedMessage("Profile updated successfully!");
                    // Opcional: Atualizar o apelido localmente se ele foi alterado.
                    // Isso pode ser feito relogando ou tendo o servidor enviando o novo apelido.
                    break;
                case "041": // Exclusão de Conta Sucesso
                    currentToken = null;
                    currentUsername = null;
                    connected.set(false);
                    updateGUIState();
                    // Removido: appendReceivedMessage("Account deleted successfully.");
                    break;
                case "051": // Criação de Tópico Sucesso (confirmação para o cliente que criou)
                    // Removido: appendReceivedMessage("Forum topic created successfully!");
                    // Limpa os campos após a criação bem-sucedida
                    topicTitleField.setText("");
                    topicSubjectField.setText("");
                    topicMessageArea.setText("");
                    break;
                case "055": // NOVO: Tópico de Fórum Transmitido (para todos os clientes)
                    String topicId = message.getTopicId();
                    String topicTitle = message.getTopicTitle();
                    String topicSubject = message.getTopicSubject();
                    String topicContent = message.getTopicContent();
                    String topicAuthor = message.getTopicAuthor();

                    // Formata e exibe a mensagem do tópico no log
                    appendReceivedMessage("\n--- NOVO TÓPICO DO FÓRUM ---");
                    appendReceivedMessage("ID: " + topicId);
                    appendReceivedMessage("Título: " + topicTitle);
                    appendReceivedMessage("Assunto: " + topicSubject);
                    appendReceivedMessage("Autor: " + topicAuthor);
                    appendReceivedMessage("Conteúdo:\n" + topicContent);
                    appendReceivedMessage("---------------------------\n");
                    break;
                default:
                    // Mensagens de resposta desconhecidas ou não tratadas
                    appendReceivedMessage("Resposta desconhecida do servidor: " + opCode + " - " + msgContent);
            }
        });
    }

    // --- Métodos de Envio de Requisições do Protocolo ---

    /**
     * Envia uma requisição de Login (000) para o servidor.
     */
    private void sendLoginRequest() {
        try {
            connectToServer(); // Garante que o socket esteja pronto antes de enviar
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nome de usuário e senha não podem estar vazios.", "Erro de Entrada", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage loginMsg = new ProtocolMessage("000");
            loginMsg.setUser(user);
            loginMsg.setPassword(pass);
            sendMessageToServer(loginMsg);
            appendReceivedMessage("Enviando requisição de login para o usuário: " + user);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao enviar requisição de login: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Envia uma requisição de Registro (010) para o servidor.
     */
    private void sendRegisterRequest() {
        try {
            connectToServer(); // Garante que o socket esteja pronto
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();
            String nick = nicknameField.getText().trim();

            if (user.isEmpty() || pass.isEmpty() || nick.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nome de usuário, senha e apelido não podem estar vazios para o registro.", "Erro de Entrada", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage registerMsg = new ProtocolMessage("010");
            registerMsg.setUser(user);
            registerMsg.setPassword(pass);
            registerMsg.setNickname(nick);
            sendMessageToServer(registerMsg);
            appendReceivedMessage("Enviando requisição de registro para o usuário: " + user);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao enviar requisição de registro: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Envia uma requisição de Logout (020) para o servidor.
     */
    private void sendLogoutRequest() {
        if (!connected.get() || currentUsername == null || currentToken == null) {
            appendReceivedMessage("Não está logado.");
            return;
        }
        try {
            ProtocolMessage logoutMsg = new ProtocolMessage("020");
            logoutMsg.setUser(currentUsername);
            logoutMsg.setToken(currentToken);
            sendMessageToServer(logoutMsg);
            appendReceivedMessage("Enviando requisição de logout para o usuário: " + currentUsername);
        }
        // Não é necessário um catch aqui, pois sendMessageToServer já trata exceções.
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao enviar requisição de logout: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Envia uma requisição de Atualização de Perfil (030) para o servidor.
     */
    private void sendUpdateProfileRequest() {
        if (!connected.get() || currentUsername == null) {
            JOptionPane.showMessageDialog(this, "Você deve estar logado para atualizar seu perfil.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String user = usernameField.getText().trim(); // Usa o nome de usuário atual logado
            String pass = new String(passwordField.getPassword()).trim(); // Senha atual para verificação
            String newNick = nicknameField.getText().trim(); // Novo apelido (opcional)
            String newPass = new String(newPasswordField.getPassword()).trim(); // Nova senha (opcional)

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nome de usuário e senha atuais são obrigatórios para atualizar o perfil.", "Erro de Entrada", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage updateMsg = new ProtocolMessage("030");
            updateMsg.setUser(user);
            updateMsg.setPassword(pass);
            if (!newNick.isEmpty()) {
                updateMsg.setNewNickname(newNick);
            }
            if (!newPass.isEmpty()) {
                updateMsg.setNewPassword(newPass);
            }

            if (newNick.isEmpty() && newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Digite um novo apelido ou uma nova senha para atualizar.", "Erro de Entrada", JOptionPane.WARNING_MESSAGE);
                return;
            }

            sendMessageToServer(updateMsg);
            appendReceivedMessage("Enviando requisição de atualização de perfil para o usuário: " + user);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao enviar requisição de atualização de perfil: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Envia uma requisição de Exclusão de Conta (040) para o servidor.
     */
    private void sendDeleteAccountRequest() {
        if (!connected.get() || currentUsername == null || currentToken == null) {
            JOptionPane.showMessageDialog(this, "Você deve estar logado para excluir sua conta.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Confirmação antes de excluir a conta
        int confirm = JOptionPane.showConfirmDialog(this, "Tem certeza de que deseja excluir sua conta? Esta ação não pode ser desfeita.", "Confirmar Exclusão", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nome de usuário e senha são obrigatórios para excluir a conta.", "Erro de Entrada", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage deleteMsg = new ProtocolMessage("040");
            deleteMsg.setUser(user);
            deleteMsg.setToken(currentToken);
            deleteMsg.setPassword(pass);
            sendMessageToServer(deleteMsg);
            appendReceivedMessage("Enviando requisição de exclusão de conta para o usuário: " + user);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao enviar requisição de exclusão de conta: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Envia uma requisição de Criação de Tópico (050) para o servidor.
     */
    private void sendCreateTopicRequest() {
        if (!connected.get() || currentToken == null) {
            JOptionPane.showMessageDialog(this, "Você deve estar logado para criar um tópico.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String title = topicTitleField.getText().trim();
            String subject = topicSubjectField.getText().trim();
            String msgContent = topicMessageArea.getText().trim();

            if (title.isEmpty() || subject.isEmpty() || msgContent.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Título, assunto e conteúdo da mensagem não podem estar vazios.", "Erro de Entrada", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProtocolMessage topicMsg = new ProtocolMessage("050");
            topicMsg.setToken(currentToken);
            topicMsg.setTitle(title);
            topicMsg.setSubject(subject);
            topicMsg.setMessageContent(msgContent);
            sendMessageToServer(topicMsg);
            appendReceivedMessage("Enviando nova requisição de tópico: '" + title + "'");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao enviar requisição de criação de tópico: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Auxiliar de GUI ---

    /**
     * Adiciona uma mensagem à área de mensagens recebidas/log da GUI do cliente.
     * Garante que a atualização da GUI seja feita na Event Dispatch Thread (EDT).
     *
     * @param message A mensagem a ser exibida.
     */
    private void appendReceivedMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            receivedMessagesArea.append("[" + new java.util.Date() + "] " + message + "\n");
            receivedMessagesArea.setCaretPosition(receivedMessagesArea.getDocument().getLength()); // Rola para o final
        });
    }

    /**
     * Método principal para iniciar o cliente.
     * Garante que a inicialização da GUI seja feita na Event Dispatch Thread (EDT).
     *
     * @param args Argumentos da linha de comando (não utilizados).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UDPClient());
    }
}