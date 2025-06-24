package server.service;

import common.ClientInfo;
import common.ProtocolMessage;
import server.model.MessageReply;
import server.model.Topic;
import server.model.User;
import server.repository.ReplyRepository;
import server.repository.TopicRepository;
import server.repository.UserRepository;

import java.util.List; // Import for List
import java.util.function.Consumer;

/**
 * Handles administration operations (Change Other User's Profile, Delete Other User's Account, Delete Message).
 */
public class AdminHandler {
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final AuthHandler authHandler; // To validate tokens and get user data
    private final Consumer<String> logConsumer;
    private final Consumer<ClientInfo> clientListUpdater;

    public AdminHandler(UserRepository userRepository, TopicRepository topicRepository, ReplyRepository replyRepository, AuthHandler authHandler, Consumer<String> logConsumer, Consumer<ClientInfo> clientListUpdater) {
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.replyRepository = replyRepository;
        this.authHandler = authHandler;
        this.logConsumer = logConsumer;
        this.clientListUpdater = clientListUpdater;
    }

    // Helper to check if the token belongs to an administrator
    private boolean isAdminToken(String token) {
        ClientInfo client = authHandler.getAuthenticatedClientInfo(token);
        if (client == null) {
            return false;
        }
        User user = authHandler.getUserByUsername(client.getUserId());
        return user != null && "admin".equals(user.getRole());
    }

    // --- Operation 080: Alterar Cadastro (admin) ---
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

        // Restriction: Admin user cannot alter their own profile via 080
        if (targetUser.equals(clientInfo.getUserId()) && "admin".equals(userRepository.findByUsername(clientInfo.getUserId()).getRole())) {
            logConsumer.accept("Admin change profile failed: Admin cannot alter their own account via this operation (080).");
            return ProtocolMessage.createErrorMessage("082", "Admin cannot alter their own account via this operation.");
        }
        // Restriction: Admin user cannot be altered by another admin via 080
        if ("admin".equals(userRepository.findByUsername(targetUser).getRole())) {
            logConsumer.accept("Admin change profile failed: Cannot alter another admin account.");
            return ProtocolMessage.createErrorMessage("082", "Cannot alter another admin account.");
        }


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
            return new ProtocolMessage("081", "User profile updated successfully.");
        } else {
            logConsumer.accept("Admin attempted to update user '" + targetUser + "' but no changes were made.");
            return ProtocolMessage.createErrorMessage("082", "No changes made to profile."); // Or a specific success without change msg
        }
    }

    // --- Operation 090: Apagar Cadastro (admin) ---
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

        // Restriction: Admin user cannot delete their own account via 090
        if (targetUser.equals(clientInfo.getUserId()) && "admin".equals(userRepository.findByUsername(clientInfo.getUserId()).getRole())) {
            logConsumer.accept("Admin delete user failed: Admin cannot delete their own account via this operation (090).");
            return ProtocolMessage.createErrorMessage("092", "Admin cannot delete their own account via this operation.");
        }
        // Restriction: Cannot delete another admin account
        if ("admin".equals(userRepository.findByUsername(targetUser).getRole())) {
            logConsumer.accept("Admin delete user failed: Cannot delete another admin account.");
            return ProtocolMessage.createErrorMessage("092", "Cannot delete another admin account.");
        }

        User userToDelete = userRepository.findByUsername(targetUser);
        if (userToDelete == null) {
            logConsumer.accept("Admin delete user failed: Target user '" + targetUser + "' not found.");
            return ProtocolMessage.createErrorMessage("092", "Target user not found.");
        }

        userRepository.deleteByUsername(targetUser);
        logConsumer.accept("User account '" + targetUser + "' deleted by admin '" + clientInfo.getUserId() + "'.");

        // Note: Forcing logout of the deleted user is a separate concern, might need explicit call to AuthHandler
        // if user is currently logged in.

        return new ProtocolMessage("091", "User account deleted successfully.");
    }

    // --- Operation 100: Apagar Mensagem (admin) ---
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

        // Try to find as a topic
        Topic topicToDelete = topicRepository.findById(messageId);
        if (topicToDelete != null) {
            topicToDelete.markAsDeleted();
            logConsumer.accept("Topic '" + messageId + "' marked as deleted by admin '" + clientInfo.getUserId() + "'.");
            return new ProtocolMessage("101", "Topic deleted successfully.");
        }

        // If not a topic, try to find as a reply
        MessageReply replyToDelete = null;
        String parentTopicId = null;
        // Iterate through all topics to find the nested reply (less efficient but functional for in-memory DB)
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
            return new ProtocolMessage("101", "Reply deleted successfully.");
        }

        logConsumer.accept("Admin delete message failed: Message/Topic with ID '" + messageId + "' not found.");
        return ProtocolMessage.createErrorMessage("102", "Message/Topic not found.");
    }

    // --- Operation 110: Retornar TODOS Usuários (admin) ---
    public ProtocolMessage handleListAllUsers(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();

        logConsumer.accept("Admin '" + clientInfo.getUserId() + "' attempting to list all users.");

        if (token == null || token.isEmpty() || !isAdminToken(token)) {
            logConsumer.accept("List all users failed: Invalid or non-admin token.");
            return ProtocolMessage.createErrorMessage("112", "Invalid or non-admin token.");
        }

        List<String> allUsernames = userRepository.listAllUsernames();
        ProtocolMessage response = new ProtocolMessage("111");
        response.setUserList(allUsernames); // Set the user_list field
        logConsumer.accept("Sent 111 response with " + allUsernames.size() + " users.");
        return response;
    }
}