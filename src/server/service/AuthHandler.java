package server.service;

import common.ClientInfo;
import common.ProtocolMessage;
import server.model.User;
import server.repository.UserRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AuthHandler {
    private final UserRepository userRepository;
    private final Map<String, ClientInfo> authenticatedUsers;
    private final AtomicInteger nextTokenId;
    private final Consumer<String> logConsumer;
    private final Consumer<ClientInfo> clientListUpdater;

    public AuthHandler(UserRepository userRepository, Map<String, ClientInfo> authenticatedUsers, Consumer<String> logConsumer, Consumer<ClientInfo> clientListUpdater) {
        this.userRepository = userRepository;
        this.authenticatedUsers = authenticatedUsers;
        this.nextTokenId = new AtomicInteger(1);
        this.logConsumer = logConsumer;
        this.clientListUpdater = clientListUpdater;
    }

    public ProtocolMessage handleLogin(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String pass = request.getPassword();

        logConsumer.accept("Attempting login for user: '" + user + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        if (user == null || user.isEmpty() || !user.matches("[a-zA-Z0-9]{6,16}") ||
                pass == null || pass.isEmpty() || !pass.matches("[a-zA-Z0-9]{6,32}")) {
            logConsumer.accept("Login failed: Invalid format for user or password.");
            return ProtocolMessage.createErrorMessage("002", "Formato de Usuario ou Senha errados.");
        }

        User storedUser = userRepository.findByUsername(user);
        if (storedUser == null) {
            logConsumer.accept("Login failed: User '" + user + "' does not exist.");
            return ProtocolMessage.createErrorMessage("002", "Usuario nao existe.");
        }
        if (!storedUser.getPassword().equals(pass)) {
            logConsumer.accept("Login failed: Incorrect password for user '" + user + "'.");
            return ProtocolMessage.createErrorMessage("002", "Senha errada.");
        }

        String rolePrefix = "common".equals(storedUser.getRole()) ? "c" : "a";
        String token = rolePrefix + String.format("%05d", nextTokenId.getAndIncrement());

        clientInfo.setUserId(user);
        clientInfo.setName(storedUser.getNickname());
        clientInfo.setToken(token);
        authenticatedUsers.put(token, clientInfo);

        clientListUpdater.accept(clientInfo);
        logConsumer.accept("Client '" + user + "' (ID: " + storedUser.getId() + ") logged in with token: " + token);

        ProtocolMessage response = new ProtocolMessage("001");
        response.setToken(token);
        return response;
    }

    public ProtocolMessage handleRegister(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String nick = request.getNickname();
        String pass = request.getPassword();

        logConsumer.accept("Attempting registration for user: '" + user + "', nickname: '" + nick + "'");

        if (user == null || user.isEmpty() || nick == null || nick.isEmpty() || pass == null || pass.isEmpty()) {
            return ProtocolMessage.createErrorMessage("012", "Usuario, Nick ou Senha nulos.");
        }
        if (userRepository.existsByUsername(user)) {
            return ProtocolMessage.createErrorMessage("012", "Usuario ja existe.");
        }
        if (!user.matches("[a-zA-Z0-9]{6,16}") || !nick.matches("[a-zA-Z0-9]{6,16}") || !pass.matches("[a-zA-Z0-9]{6,32}")) {
            return ProtocolMessage.createErrorMessage("012", "Formato de Usuario, Nick ou Senha errados.");
        }

        User newUser = new User(user, pass, nick, "common");
        userRepository.save(newUser);

        logConsumer.accept("New user registered: '" + user + "' (ID: " + newUser.getId() + ")");

        return new ProtocolMessage("011", "Cadastro realizado com sucesso.");
    }

    public ProtocolMessage handleLogout(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String token = request.getToken();

        logConsumer.accept("Attempting logout for user: '" + user + "' with token: '" + token + "'");

        ClientInfo authClient = authenticatedUsers.get(token);
        if (authClient == null || !authClient.getUserId().equals(user)) {
            return ProtocolMessage.createErrorMessage("022", "Token pertence a outro usuario ou nao existe.");
        }

        authenticatedUsers.remove(token);
        clientListUpdater.accept(clientInfo);
        logConsumer.accept("Client '" + user + "' logged out. Token: " + token + " removed.");

        return new ProtocolMessage("021", "Logout realizado com sucesso.");
    }

    public ClientInfo getAuthenticatedClientInfo(String token) {
        return authenticatedUsers.get(token);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}