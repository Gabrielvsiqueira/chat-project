package server.service;

import common.ClientInfo;
import common.ProtocolMessage;
import server.model.User;
import server.repository.UserRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Handles authentication operations (Login, Register, Logout).
 */
public class AuthHandler {
    private final UserRepository userRepository;
    private final Map<String, ClientInfo> authenticatedUsers; // token -> ClientInfo
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

    /**
     * Handles Login operation (000).
     * @param request The login request message.
     * @param clientInfo The ClientInfo associated with the client's endpoint.
     * @return ProtocolMessage for success (001) or error (002).
     */
    public ProtocolMessage handleLogin(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String pass = request.getPassword();

        logConsumer.accept("Attempting login for user: '" + user + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            logConsumer.accept("Login failed: User or password cannot be null/empty.");
            return ProtocolMessage.createErrorMessage("002", "User or password cannot be null/empty.");
        }
        User storedUser = userRepository.findByUsername(user);
        if (storedUser == null) {
            logConsumer.accept("Login failed: User '" + user + "' does not exist.");
            return ProtocolMessage.createErrorMessage("002", "User '" + user + "' does not exist.");
        }
        if (!storedUser.getPassword().equals(pass)) {
            logConsumer.accept("Login failed: Incorrect password for user '" + user + "'.");
            return ProtocolMessage.createErrorMessage("002", "Incorrect password for user '" + user + "'.");
        }

        String token = storedUser.getRole().substring(0, 1) + String.format("%04d", nextTokenId.getAndIncrement());
        clientInfo.setUserId(user);
        clientInfo.setName(storedUser.getNickname());
        clientInfo.setToken(token);
        authenticatedUsers.put(token, clientInfo);

        clientListUpdater.accept(clientInfo);
        logConsumer.accept("Client '" + user + "' logged in with token: " + token + " from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort() + ". Nickname: " + storedUser.getNickname());

        ProtocolMessage response = new ProtocolMessage("001");
        response.setToken(token);
        response.setUser(user);
        return response;
    }

    /**
     * Handles Register operation (010).
     * @param request The registration request message.
     * @param clientInfo The ClientInfo associated with the client's endpoint.
     * @return ProtocolMessage for success (011) or error (012).
     */
    public ProtocolMessage handleRegister(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String nick = request.getNickname();
        String pass = request.getPassword();

        logConsumer.accept("Attempting registration for user: '" + user + "', nickname: '" + nick + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        if (user == null || user.isEmpty() || nick == null || nick.isEmpty() || pass == null || pass.isEmpty()) {
            logConsumer.accept("Registration failed: User, nickname, or password cannot be null/empty.");
            return ProtocolMessage.createErrorMessage("012", "User, nickname, or password cannot be null/empty.");
        }
        if (userRepository.existsByUsername(user)) {
            logConsumer.accept("Registration failed: User '" + user + "' already exists.");
            return ProtocolMessage.createErrorMessage("012", "User '" + user + "' already exists.");
        }

        if (user.length() < 6 || user.length() > 16 || !user.matches("[a-zA-Z0-9]+")) {
            logConsumer.accept("Registration failed: Username must be 6-16 alphanumeric characters.");
            return ProtocolMessage.createErrorMessage("012", "Username must be 6-16 alphanumeric characters.");
        }
        if (pass.length() < 6 || pass.length() > 32 || !pass.matches("[a-zA-Z0-9]+")) {
            logConsumer.accept("Registration failed: Password must be 6-32 alphanumeric characters.");
            return ProtocolMessage.createErrorMessage("012", "Password must be 6-32 alphanumeric characters.");
        }
        if (nick.length() < 6 || nick.length() > 16 || !nick.matches("[a-zA-Z0-9]+")) {
            logConsumer.accept("Registration failed: Nickname must be 6-16 alphanumeric characters.");
            return ProtocolMessage.createErrorMessage("012", "Nickname must be 6-16 alphanumeric characters.");
        }

        User newUser = new User(user, pass, nick, "common");
        userRepository.save(newUser);

        logConsumer.accept("New user registered: '" + user + "' (" + nick + ") from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        return new ProtocolMessage("011", "Registration successful!"); // Added success message
    }

    /**
     * Handles Logout operation (020).
     * @param request The logout request message.
     * @param clientInfo The ClientInfo associated with the client's endpoint.
     * @return ProtocolMessage for success (021) or error (022).
     */
    public ProtocolMessage handleLogout(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String token = request.getToken();

        logConsumer.accept("Attempting logout for user: '" + user + "' with token: '" + token + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        if (token == null || token.isEmpty() || !authenticatedUsers.containsKey(token) || !authenticatedUsers.get(token).getUserId().equals(user)) {
            logConsumer.accept("Logout failed: Invalid token or token does not match user.");
            return ProtocolMessage.createErrorMessage("022", "Invalid token or token does not match user.");
        }

        authenticatedUsers.remove(token);

        // Keep clientInfo details for logging on the server side until ClientHandler is removed.
        // clientInfo.setUserId(null); // Protocol says token is removed from memory, not necessarily clear ClientInfo
        // clientInfo.setToken(null);
        // clientInfo.setName("Guest"); // ClientInfo might still be needed by ClientHandler until connection closes.

        clientListUpdater.accept(clientInfo); // Update GUI to reflect logout
        logConsumer.accept("Client '" + user + "' logged out. Token: " + token + " from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        return new ProtocolMessage("021", "Logged out successfully."); // Added success message
    }

    // Helper method for other handlers to get authenticated ClientInfo
    public ClientInfo getAuthenticatedClientInfo(String token) {
        return authenticatedUsers.get(token);
    }

    // Helper method for other handlers to get User (for nicknames, roles, etc.)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}