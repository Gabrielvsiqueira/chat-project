package server.service;

import common.ClientInfo;
import common.ProtocolMessage;
import server.model.User;
import server.repository.UserRepository;

import java.util.function.Consumer;

/**
 * Lida com as operações de recuperação de dados de usuário (005).
 */
public class UserDataHandler {
    private final UserRepository userRepository;
    private final AuthHandler authHandler; // Para validar tokens
    private final Consumer<String> logConsumer;

    public UserDataHandler(UserRepository userRepository, AuthHandler authHandler, Consumer<String> logConsumer) {
        this.userRepository = userRepository;
        this.authHandler = authHandler;
        this.logConsumer = logConsumer;
    }

    public ProtocolMessage handleRetrieveUserData(ProtocolMessage request, ClientInfo clientInfo) {
        String requestedUser = request.getUser();
        String token = request.getToken();

        logConsumer.accept("Attempting to retrieve data for user: '" + requestedUser + "' by client token '" + token + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        if (token == null || token.isEmpty() || requestedUser == null || requestedUser.isEmpty()) {
            logConsumer.accept("User data retrieval failed: User or token cannot be null/empty.");
            return ProtocolMessage.createErrorMessage("007", "User or token cannot be null/empty for data retrieval.");
        }

        ClientInfo authenticatingClient = authHandler.getAuthenticatedClientInfo(token);
        if (authenticatingClient == null || !authenticatingClient.getUserId().equals(clientInfo.getUserId())) {
            logConsumer.accept("User data retrieval failed: Invalid token or token does not match requesting client.");
            return ProtocolMessage.createErrorMessage("007", "Invalid token or token does not match requesting client for data retrieval.");
        }

        User storedUser = userRepository.findByUsername(requestedUser);
        if (storedUser == null) {
            logConsumer.accept("User data retrieval failed: User '" + requestedUser + "' not found.");
            return ProtocolMessage.createErrorMessage("007", "User '" + requestedUser + "' not found.");
        }

        ProtocolMessage response = new ProtocolMessage("006");
        response.setUser(storedUser.getUsername());
        response.setNickname(storedUser.getNickname());
        logConsumer.accept("Sent 006 response with data for user '" + requestedUser + "' (Nickname: " + storedUser.getNickname() + ") to " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());
        return response;
    }
}