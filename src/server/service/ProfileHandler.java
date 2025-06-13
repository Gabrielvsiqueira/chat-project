package server.service;

import common.ClientInfo;
import common.ProtocolMessage;
import server.model.User;
import server.repository.UserRepository;

import java.util.function.Consumer;

/**
 * Lida com as operações de alteração de perfil e exclusão de conta.
 */
public class ProfileHandler {
    private final UserRepository userRepository;
    private final AuthHandler authHandler; // Para validar tokens
    private final Consumer<String> logConsumer;
    private final Consumer<ClientInfo> clientListUpdater; // Para atualizar a GUI do servidor

    public ProfileHandler(UserRepository userRepository, AuthHandler authHandler, Consumer<String> logConsumer, Consumer<ClientInfo> clientListUpdater) {
        this.userRepository = userRepository;
        this.authHandler = authHandler;
        this.logConsumer = logConsumer;
        this.clientListUpdater = clientListUpdater;
    }

    public ProtocolMessage handleChangeProfile(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String pass = request.getPassword();
        String newNick = request.getNewNickname();
        String newPass = request.getNewPassword();
        String token = request.getToken();

        logConsumer.accept("Attempting profile change for user: '" + user + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort() + ". New Nick: " + newNick + ", New Pass Provided: " + (newPass != null && !newPass.isEmpty()));

        // Valida token e usuário
        ClientInfo authenticatedClient = authHandler.getAuthenticatedClientInfo(token);
        if (authenticatedClient == null || !authenticatedClient.getUserId().equals(user)) {
            logConsumer.accept("Profile change failed: Invalid or mismatched token for user '" + user + "'.");
            return ProtocolMessage.createErrorMessage("032", "Invalid or expired token.");
        }

        User storedUser = userRepository.findByUsername(user);
        if (storedUser == null || !storedUser.getPassword().equals(pass)) {
            logConsumer.accept("Profile change failed: Incorrect current password or user does not exist.");
            return ProtocolMessage.createErrorMessage("032", "Incorrect current password or user does not exist.");
        }

        boolean changed = false;
        String oldNick = storedUser.getNickname();
        if (newNick != null && !newNick.isEmpty()) {
            if (newNick.length() < 6 || newNick.length() > 16 || !newNick.matches("[a-zA-Z0-9]+")) {
                logConsumer.accept("Profile change failed: New nickname must be 6-16 alphanumeric characters.");
                return ProtocolMessage.createErrorMessage("032", "New nickname must be 6-16 alphanumeric characters.");
            }
            if (!oldNick.equals(newNick)) {
                storedUser.setNickname(newNick);
                clientInfo.setName(newNick); // Atualiza o nome de exibição no ClientInfo
                changed = true;
                logConsumer.accept("User '" + user + "' changed nickname from '" + oldNick + "' to '" + newNick + "'.");
            }
        }
        if (newPass != null && !newPass.isEmpty()) {
            if (newPass.length() < 6 || newPass.length() > 32 || !newPass.matches("[a-zA-Z0-9]+")) {
                logConsumer.accept("Profile change failed: New password must be 6-32 alphanumeric characters.");
                return ProtocolMessage.createErrorMessage("032", "New password must be 6-32 alphanumeric characters.");
            }
            if (!storedUser.getPassword().equals(newPass)) {
                storedUser.setPassword(newPass);
                changed = true;
                logConsumer.accept("User '" + user + "' changed password.");
            }
        }

        if (changed) {
            logConsumer.accept("User '" + user + "' profile updated successfully.");
            clientListUpdater.accept(clientInfo); // Força a atualização visual da lista
        } else {
            logConsumer.accept("User '" + user + "' sent profile update request but no changes were made.");
        }

        return new ProtocolMessage("031");
    }

    public ProtocolMessage handleDeleteAccount(ProtocolMessage request, ClientInfo clientInfo) {
        String user = request.getUser();
        String token = request.getToken();
        String pass = request.getPassword();

        logConsumer.accept("Attempting account deletion for user: '" + user + "' with token: '" + token + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        ClientInfo authenticatedClient = authHandler.getAuthenticatedClientInfo(token);
        if (authenticatedClient == null || !authenticatedClient.getUserId().equals(user)) {
            logConsumer.accept("Account deletion failed: Invalid token or token does not match user.");
            return ProtocolMessage.createErrorMessage("042", "Invalid token or token does not match user.");
        }

        User storedUser = userRepository.findByUsername(user);
        if (storedUser == null || !storedUser.getPassword().equals(pass)) {
            logConsumer.accept("Account deletion failed: Incorrect password or user does not exist.");
            return ProtocolMessage.createErrorMessage("042", "Incorrect password or user does not exist.");
        }

        userRepository.deleteByUsername(user);
        authenticatedClient.setToken(null);
        authenticatedClient.setUserId(null);
        authenticatedClient.setName("Guest");
        // Remove do mapa de autenticados (feito pelo AuthHandler)
        // authHandler.authenticatedUsers.remove(token); // Isso é tratado no AuthHandler
        clientListUpdater.accept(clientInfo); // Atualiza a GUI para remover o cliente

        logConsumer.accept("User account '" + user + "' deleted from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());
        return new ProtocolMessage("041");
    }
}