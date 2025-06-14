package server.service;

import common.ClientInfo;
import common.ProtocolMessage;
import server.model.MessageReply;
import server.model.Topic;
import server.model.User;
import server.repository.ReplyRepository;
import server.repository.TopicRepository;
import server.repository.UserRepository;

import java.util.function.Consumer;

/**
 * Lida com as operações de administração (Alterar Cadastro de Outro, Apagar Cadastro de Outro, Apagar Mensagem).
 */
public class AdminHandler {
    private final UserRepository userRepository; // AdminHandler tem seu próprio userRepository
    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final AuthHandler authHandler; // Para validar tokens e obter dados de usuário de forma controlada
    private final Consumer<String> logConsumer;
    private final Consumer<ClientInfo> clientListUpdater;

    public AdminHandler(UserRepository userRepository, TopicRepository topicRepository, ReplyRepository replyRepository, AuthHandler authHandler, Consumer<String> logConsumer, Consumer<ClientInfo> clientListUpdater) {
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.replyRepository = replyRepository;
        this.authHandler = authHandler; // A injeção de AuthHandler é crucial
        this.logConsumer = logConsumer;
        this.clientListUpdater = clientListUpdater;
    }

    // Helper para verificar se o token pertence a um administrador
    private boolean isAdminToken(String token) {
        ClientInfo client = authHandler.getAuthenticatedClientInfo(token);
        if (client == null) {
            return false;
        }
        // Usar o método público do AuthHandler para obter o User
        User user = authHandler.getUserByUsername(client.getUserId());
        return user != null && "admin".equals(user.getRole());
    }

    // --- Operação 080: Alterar Cadastro (admin) ---
    public ProtocolMessage handleChangeUserByAdmin(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String targetUser = request.getUser();
        String newNick = request.getNewNickname();
        String newPass = request.getNewPassword();

        logConsumer.accept("Admin '" + clientInfo.getUserId() + "' attempting to change profile for user: '" + targetUser + "'. New Nick: " + newNick + ", New Pass Provided: " + (newPass != null && !newPass.isEmpty()));

        if (token == null || token.isEmpty() || !isAdminToken(token)) {
            logConsumer.accept("Admin change profile failed: Invalid or non-admin token.");
            return ProtocolMessage.createErrorMessage("082", "Invalid or non-admin token.");
        }
        if (targetUser == null || targetUser.isEmpty()) {
            logConsumer.accept("Admin change profile failed: Target user cannot be null/empty.");
            return ProtocolMessage.createErrorMessage("082", "Target user cannot be null/empty.");
        }

        // Use o userRepository DO PRÓPRIO ADMINHANDLER, pois ele tem acesso direto
        User userToChange = userRepository.findByUsername(targetUser);
        if (userToChange == null) {
            logConsumer.accept("Admin change profile failed: Target user '" + targetUser + "' not found.");
            return ProtocolMessage.createErrorMessage("082", "Target user not found.");
        }

        boolean changed = false;
        String oldNick = userToChange.getNickname();

        if (newNick != null && !newNick.isEmpty()) {
            if (newNick.length() < 6 || newNick.length() > 16 || !newNick.matches("[a-zA-Z0-9]+")) {
                logConsumer.accept("Admin change profile failed: New nickname must be 6-16 alphanumeric characters.");
                return ProtocolMessage.createErrorMessage("082", "New nickname must be 6-16 alphanumeric characters.");
            }
            if (!oldNick.equals(newNick)) {
                userToChange.setNickname(newNick);
                changed = true;
                logConsumer.accept("User '" + targetUser + "' nickname changed by admin from '" + oldNick + "' to '" + newNick + "'.");
            }
        }
        if (newPass != null && !newPass.isEmpty()) {
            if (newPass.length() < 6 || newPass.length() > 32 || !newPass.matches("[a-zA-Z0-9]+")) {
                logConsumer.accept("Admin change profile failed: New password must be 6-32 alphanumeric characters.");
                return ProtocolMessage.createErrorMessage("082", "New password must be 6-32 alphanumeric characters.");
            }
            if (!userToChange.getPassword().equals(newPass)) {
                userToChange.setPassword(newPass);
                changed = true;
                logConsumer.accept("User '" + targetUser + "' password changed by admin.");
            }
        }

        if (changed) {
            logConsumer.accept("User '" + targetUser + "' profile updated by admin successfully.");
        } else {
            logConsumer.accept("Admin attempted to update user '" + targetUser + "' but no changes were made.");
        }

        return new ProtocolMessage("081");
    }

    // --- Operação 090: Apagar Cadastro (admin) ---
    public ProtocolMessage handleDeleteUserByAdmin(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String targetUser = request.getUser();

        logConsumer.accept("Admin '" + clientInfo.getUserId() + "' attempting to delete user: '" + targetUser + "'.");

        if (token == null || token.isEmpty() || !isAdminToken(token)) {
            logConsumer.accept("Admin delete user failed: Invalid or non-admin token.");
            return ProtocolMessage.createErrorMessage("092", "Invalid or non-admin token.");
        }
        if (targetUser == null || targetUser.isEmpty()) {
            logConsumer.accept("Admin delete user failed: Target user cannot be null/empty.");
            return ProtocolMessage.createErrorMessage("092", "Target user cannot be null/empty.");
        }
        if (targetUser.equals(clientInfo.getUserId())) {
            logConsumer.accept("Admin delete user failed: Admin cannot delete their own account via this operation.");
            return ProtocolMessage.createErrorMessage("092", "Admin cannot delete their own account via this operation.");
        }

        User userToDelete = userRepository.findByUsername(targetUser);
        if (userToDelete == null) {
            logConsumer.accept("Admin delete user failed: Target user '" + targetUser + "' not found.");
            return ProtocolMessage.createErrorMessage("092", "Target user not found.");
        }

        userRepository.deleteByUsername(targetUser);
        logConsumer.accept("User account '" + targetUser + "' deleted by admin '" + clientInfo.getUserId() + "'.");

        // Considerar forçar o logout do usuário se ele estiver ativo.
        // Isso exigiria um método como authHandler.forceLogoutUser(targetUser);
        // O ClientHandler já o removerá do activeClientOutputs e da lista da GUI quando for detectada a desconexão
        // ou na próxima operação do cliente, mas um logout forçado seria mais imediato.

        return new ProtocolMessage("091");
    }

    // --- Operação 100: Apagar Mensagem (admin) ---
    public ProtocolMessage handleDeleteMessage(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String messageId = request.getId();

        logConsumer.accept("Admin '" + clientInfo.getUserId() + "' attempting to delete message/topic with ID: '" + messageId + "'.");

        if (token == null || token.isEmpty() || !isAdminToken(token)) {
            logConsumer.accept("Admin delete message failed: Invalid or non-admin token.");
            return ProtocolMessage.createErrorMessage("102", "Invalid or non-admin token.");
        }
        if (messageId == null || messageId.isEmpty()) {
            logConsumer.accept("Admin delete message failed: Message ID cannot be null/empty.");
            return ProtocolMessage.createErrorMessage("102", "Message ID cannot be null/empty.");
        }

        Topic topicToDelete = topicRepository.findById(messageId);
        if (topicToDelete != null) {
            topicToDelete.markAsDeleted();
            logConsumer.accept("Topic '" + messageId + "' marked as deleted by admin '" + clientInfo.getUserId() + "'.");
            return new ProtocolMessage("101");
        }

        // Se não for um tópico, tenta encontrar como resposta
        // Melhorando a busca de resposta para ser mais clara
        MessageReply replyToDelete = null;
        String parentTopicId = null;
        // Percorre *todos* os tópicos para encontrar a resposta aninhada (menos eficiente, mas funcional)
        for (Topic t : topicRepository.findAll()) {
            MessageReply foundReply = replyRepository.findReplyByIdInTopic(t.getId(), messageId);
            if (foundReply != null) {
                replyToDelete = foundReply;
                parentTopicId = t.getId();
                break;
            }
        }

        if (replyToDelete != null) {
            replyToDelete.markAsDeleted();
            logConsumer.accept("Reply '" + messageId + "' in topic '" + parentTopicId + "' marked as deleted by admin '" + clientInfo.getUserId() + "'.");
            return new ProtocolMessage("101");
        }

        logConsumer.accept("Admin delete message failed: Message/Topic with ID '" + messageId + "' not found.");
        return ProtocolMessage.createErrorMessage("102", "Message/Topic not found.");
    }
}