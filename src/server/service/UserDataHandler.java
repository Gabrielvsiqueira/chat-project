package server.service;

import common.ClientInfo;
import common.ProtocolMessage;
import server.model.User;
import server.repository.UserRepository;

import java.util.function.Consumer;

public class UserDataHandler {
    private final UserRepository userRepository;
    private final AuthHandler authHandler;
    private final Consumer<String> logConsumer;

    public UserDataHandler(UserRepository userRepository, AuthHandler authHandler, Consumer<String> logConsumer) {
        this.userRepository = userRepository;
        this.authHandler = authHandler;
        this.logConsumer = logConsumer;
    }

    public ProtocolMessage handleRetrieveUserData(ProtocolMessage request, ClientInfo clientInfo) {
        // --- CORREÇÃO (Protocolo 005) ---
        // O campo 'user' agora é tratado como o NOME DE USUÁRIO do alvo da busca.
        String targetUsername = request.getUser();
        String token = request.getToken();

        logConsumer.accept("Attempting to retrieve data for user: '" + targetUsername + "' by token '" + token + "'");

        if (token == null || token.isEmpty() || targetUsername == null || targetUsername.isEmpty()) {
            return ProtocolMessage.createErrorMessage("007", "Usuario ou token nulos.");
        }

        ClientInfo authenticatingClient = authHandler.getAuthenticatedClientInfo(token);
        if (authenticatingClient == null) {
            return ProtocolMessage.createErrorMessage("007", "Token invalido.");
        }

        // --- CORREÇÃO (Protocolo 005) ---
        // Impõe a regra: o usuário só pode consultar seus próprios dados.
        // O 'targetUsername' deve ser o mesmo usuário do 'authenticatingClient'.
        if (!authenticatingClient.getUserId().equals(targetUsername)) {
            logConsumer.accept("Data retrieval failed: User '" + authenticatingClient.getUserId() + "' cannot retrieve data for another user '" + targetUsername + "'.");
            return ProtocolMessage.createErrorMessage("007", "Nao e possivel retornar dados de outros usuarios.");
        }

        User storedUser = userRepository.findByUsername(targetUsername);
        if (storedUser == null) {
            logConsumer.accept("Data retrieval failed: User '" + targetUsername + "' not found.");
            return ProtocolMessage.createErrorMessage("007", "Usuario nao existe.");
        }

        ProtocolMessage response = new ProtocolMessage("006");
        response.setUser(storedUser.getUsername());
        response.setNickname(storedUser.getNickname());
        logConsumer.accept("Sent 006 response with data for user '" + storedUser.getUsername() + "'");
        return response;
    }
}