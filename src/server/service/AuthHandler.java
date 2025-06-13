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
 * Lida com as operações de autenticação (Login, Registro, Logout).
 */
public class AuthHandler {
    private final UserRepository userRepository;
    private final Map<String, ClientInfo> authenticatedUsers; // token -> ClientInfo
    private final AtomicInteger nextTokenId;
    private final Consumer<String> logConsumer;
    private final Consumer<ClientInfo> clientListUpdater; // Para atualizar a GUI do servidor

    public AuthHandler(UserRepository userRepository, Map<String, ClientInfo> authenticatedUsers, Consumer<String> logConsumer, Consumer<ClientInfo> clientListUpdater) {
        this.userRepository = userRepository;
        this.authenticatedUsers = authenticatedUsers;
        this.nextTokenId = new AtomicInteger(1);
        this.logConsumer = logConsumer;
        this.clientListUpdater = clientListUpdater;
    }

    /**
     * Manipula a operação de Login (000).
     * Valida credenciais, gera token e autentica o cliente.
     *
     * @param request    A mensagem de requisição de login.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     * @return ProtocolMessage de sucesso (001) ou erro (002).
     */
    public ProtocolMessage handleLogin(ProtocolMessage request, ClientInfo clientInfo) { // <--- ESTE MÉTODO
        String user = request.getUser();
        String pass = request.getPassword();

        logConsumer.accept("Attempting login for user: '" + user + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        // Validação de entrada
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

        // Gera token e autentica o cliente
        String token = storedUser.getRole().substring(0, 1) + String.format("%04d", nextTokenId.getAndIncrement());
        clientInfo.setUserId(user);
        clientInfo.setName(storedUser.getNickname());
        clientInfo.setToken(token);
        authenticatedUsers.put(token, clientInfo);

        clientListUpdater.accept(clientInfo); // Atualiza a GUI
        logConsumer.accept("Client '" + user + "' logged in with token: " + token + " from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort() + ". Nickname: " + storedUser.getNickname());

        ProtocolMessage response = new ProtocolMessage("001");
        response.setToken(token);
        response.setUser(user);
        return response;
    }

    /**
     * Manipula a operação de Cadastro (010).
     *
     * @param request    A mensagem de requisição de cadastro.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     * @return ProtocolMessage de sucesso (011) ou erro (012).
     */
    public ProtocolMessage handleRegister(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String nick = request.getNickname();
        String pass = request.getPassword();

        logConsumer.accept("Attempting registration for user: '" + user + "', nickname: '" + nick + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        // Validação de entrada
        if (user == null || user.isEmpty() || nick == null || nick.isEmpty() || pass == null || pass.isEmpty()) {
            logConsumer.accept("Registration failed: User, nickname, or password cannot be null/empty.");
            return ProtocolMessage.createErrorMessage("012", "User, nickname, or password cannot be null/empty.");
        }
        if (userRepository.existsByUsername(user)) {
            logConsumer.accept("Registration failed: User '" + user + "' already exists.");
            return ProtocolMessage.createErrorMessage("012", "User '" + user + "' already exists.");
        }

        // Validação de formato (conforme o protocolo)
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

        User newUser = new User(user, pass, nick, "common"); // Novo usuário com papel "common"
        userRepository.save(newUser);

        logConsumer.accept("New user registered: '" + user + "' (" + nick + ") from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        return new ProtocolMessage("011");
    }

    /**
     * Manipula a operação de Logout (020).
     *
     * @param request    A mensagem de requisição de logout.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     * @return ProtocolMessage de sucesso (021) ou erro (022).
     */
    public ProtocolMessage handleLogout(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String token = request.getToken();

        logConsumer.accept("Attempting logout for user: '" + user + "' with token: '" + token + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        if (token == null || token.isEmpty() || !authenticatedUsers.containsKey(token) || !authenticatedUsers.get(token).getUserId().equals(user)) {
            logConsumer.accept("Logout failed: Invalid token or token does not match user.");
            return ProtocolMessage.createErrorMessage("022", "Invalid token or token does not match user.");
        }

        // Remova o token apenas se ele pertence ao usuário que está fazendo a requisição.
        // Já verificamos isso acima, então podemos remover com segurança.
        authenticatedUsers.remove(token);

        clientInfo.setUserId(null); // Limpa as informações de autenticação do ClientInfo
        clientInfo.setToken(null);
        clientInfo.setName("Guest"); // Reseta o nome de exibição

        clientListUpdater.accept(clientInfo); // Atualiza a GUI para remover o cliente
        logConsumer.accept("Client '" + user + "' logged out. Token: " + token + " from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        return new ProtocolMessage("021");
    }

    // Método auxiliar para outros handlers obterem ClientInfo autenticado
    public ClientInfo getAuthenticatedClientInfo(String token) {
        return authenticatedUsers.get(token);
    }

    // Método auxiliar para outros handlers obterem User (para nicknames, roles, etc.)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}