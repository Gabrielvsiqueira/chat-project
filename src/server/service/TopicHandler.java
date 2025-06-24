package server.service;

import common.ClientInfo;
import common.ProtocolMessage;
import common.SerializationHelper;
import server.model.MessageReply;
import server.model.Topic;
import server.model.User; // Importar a classe User para obter nicknames
import server.repository.ReplyRepository;
import server.repository.TopicRepository;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Lida com as operações relacionadas a tópicos do fórum (criar, listar, broadcast)
 * e também com respostas a tópicos.
 */
public class TopicHandler {
    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final AuthHandler authHandler; // Usado para validar tokens e obter User objects
    private final Consumer<String> logConsumer;
    private final Map<String, ObjectOutputStream> activeClientOutputs; // token -> ObjectOutputStream para broadcast

    public TopicHandler(TopicRepository topicRepository, ReplyRepository replyRepository, AuthHandler authHandler, Consumer<String> logConsumer, Map<String, ObjectOutputStream> activeClientOutputs) {
        this.topicRepository = topicRepository;
        this.replyRepository = replyRepository;
        this.authHandler = authHandler;
        this.logConsumer = logConsumer;
        this.activeClientOutputs = activeClientOutputs;
    }

    /**
     * Manipula a operação 050 (Criar Tópico).
     * Cria um novo tópico no fórum e o transmite para todos os clientes autenticados.
     *
     * @param request    A mensagem de requisição de criação de tópico.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     * @return ProtocolMessage de sucesso (051) ou erro (052).
     */
    public ProtocolMessage handleCreateTopic(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String title = request.getTitle();
        String subject = request.getSubject();
        String msgContent = request.getMessageContent();

        logConsumer.accept("Attempting to create topic by client: '" + clientInfo.getName() + "' (ID: " + clientInfo.getUserId() + ") from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort() + ". Title: '" + title + "'");

        // Validação de entrada obrigatória
        if (token == null || token.isEmpty() || title == null || title.isEmpty() ||
                subject == null || subject.isEmpty() || msgContent == null || msgContent.isEmpty()) {
            logConsumer.accept("Topic creation failed: Missing token, title, subject, or message content.");
            return ProtocolMessage.createErrorMessage("052", "Token, title, subject, or message cannot be null/empty.");
        }

        // Validação de autenticação/token
        ClientInfo authClient = authHandler.getAuthenticatedClientInfo(token);
        if (authClient == null || !authClient.getUserId().equals(clientInfo.getUserId())) {
            logConsumer.accept("Topic creation failed: Invalid or expired token for user '" + clientInfo.getUserId() + "'.");
            return ProtocolMessage.createErrorMessage("052", "Invalid or expired token.");
        }

        // Validações de comprimento conforme protocolo: [6:???] para title, subject, msg
        // Assumindo ??? = 255 para título e assunto, e 1024 para mensagem. Ajuste se souber os valores exatos.
        if (title.length() < 6 || title.length() > 255) {
            logConsumer.accept("Topic creation failed: Title length out of range [6-255].");
            return ProtocolMessage.createErrorMessage("052", "Title must be 6-255 characters.");
        }
        if (subject.length() < 6 || subject.length() > 255) {
            logConsumer.accept("Topic creation failed: Subject length out of range [6-255].");
            return ProtocolMessage.createErrorMessage("052", "Subject must be 6-255 characters.");
        }
        if (msgContent.length() < 6 || msgContent.length() > 8192) { // 8192 para caber no buffer, ajuste se necessário
            logConsumer.accept("Topic creation failed: Message content length out of range [6-8192].");
            return ProtocolMessage.createErrorMessage("052", "Message content must be 6-8192 characters.");
        }


        String topicId = topicRepository.getNextTopicId();
        Topic newTopic = new Topic(topicId, title, subject, msgContent, authClient.getUserId());
        topicRepository.save(newTopic);

        logConsumer.accept("New topic created by " + authClient.getUserId() + ": '" + title + "' (ID: " + topicId + ") from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        broadcastTopic(newTopic); // Transmite o novo tópico para todos os clientes

        return new ProtocolMessage("051");
    }

    /**
     * Manipula a operação 060 (Enviar Mensagem - Responder mensagem).
     * Cria uma nova resposta para um tópico existente.
     *
     * @param request    A mensagem de requisição de resposta.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     * @return ProtocolMessage de sucesso (061) ou erro (062).
     */
    public ProtocolMessage handleReplyMessage(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String topicId = request.getId(); // 'id' se refere ao ID do tópico para o qual a resposta é
        String msgContent = request.getMessageContent();

        logConsumer.accept("Attempting to reply to topic '" + topicId + "' by client: '" + clientInfo.getName() + "' (ID: " + clientInfo.getUserId() + ") from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        // Validação de entrada obrigatória
        if (token == null || token.isEmpty() || topicId == null || topicId.isEmpty() || msgContent == null || msgContent.isEmpty()) {
            logConsumer.accept("Reply failed: Missing token, topic ID, or message content.");
            return ProtocolMessage.createErrorMessage("062", "Token, Topic ID, or message cannot be null/empty.");
        }

        // Validação de autenticação/token
        ClientInfo authClient = authHandler.getAuthenticatedClientInfo(token);
        if (authClient == null || !authClient.getUserId().equals(clientInfo.getUserId())) {
            logConsumer.accept("Reply failed: Invalid or expired token for user '" + clientInfo.getUserId() + "'.");
            return ProtocolMessage.createErrorMessage("062", "Invalid or expired token.");
        }

        // Verificar se o tópico existe
        Topic existingTopic = topicRepository.findById(topicId);
        if (existingTopic == null) {
            logConsumer.accept("Reply failed: Topic with ID '" + topicId + "' not found.");
            return ProtocolMessage.createErrorMessage("062", "Topic not found.");
        }

        // Validação de comprimento do conteúdo da mensagem de resposta: [6:???]
        if (msgContent.length() < 6 || msgContent.length() > 8192) { // Usando 8192 como limite, ajuste se necessário
            logConsumer.accept("Reply failed: Message content length out of range [6-8192].");
            return ProtocolMessage.createErrorMessage("062", "Message content must be 6-8192 characters.");
        }

        String replyId = replyRepository.getNextReplyId(); // Gera um ID único para a resposta
        MessageReply newReply = new MessageReply(replyId, topicId, authClient.getUserId(), msgContent);
        replyRepository.save(newReply);

        logConsumer.accept("New reply created by " + authClient.getUserId() + " to topic '" + topicId + "' (Reply ID: " + replyId + ")");

        // Opcional: Você pode querer transmitir a nova resposta para todos os clientes também.
        // Se decidir implementar isso, crie um novo opcode (ex: "065" para Broadcast de Resposta)
        // e um método broadcastReply(newReply) similar ao broadcastTopic.

        return new ProtocolMessage("061"); // Sucesso
    }

    /**
     * Manipula a operação 070 (Receber Respostas).
     * Retorna todas as respostas para um tópico específico.
     *
     * @param request    A mensagem de requisição.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     * @return ProtocolMessage com a lista de respostas (071) ou erro (072).
     */
    public ProtocolMessage handleGetReplies(ProtocolMessage request, ClientInfo clientInfo) {
        String topicId = request.getId();

        logConsumer.accept("Attempting to get replies for topic '" + topicId + "' by client: '" + clientInfo.getName() + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        if (topicId == null || topicId.isEmpty()) {
            logConsumer.accept("Get replies failed: Topic ID cannot be null/empty.");
            return ProtocolMessage.createErrorMessage("072", "Topic ID cannot be null/empty.");
        }

        // Pelo protocolo, 070 C->S não exige token. Assumimos que qualquer cliente conectado pode ver as respostas.
        // Se você quiser exigir autenticação, adicione a verificação de token aqui.
        /*
        String token = request.getToken();
        ClientInfo authClient = authHandler.getAuthenticatedClientInfo(token);
        if (authClient == null || !authClient.getUserId().equals(clientInfo.getUserId())) {
            logConsumer.accept("Get replies failed: Invalid or expired token.");
            return ProtocolMessage.createErrorMessage("072", "Invalid or expired token.");
        }
        */

        Topic existingTopic = topicRepository.findById(topicId);
        if (existingTopic == null) {
            logConsumer.accept("Get replies failed: Topic with ID '" + topicId + "' not found.");
            return ProtocolMessage.createErrorMessage("072", "Topic not found.");
        }

        List<MessageReply> replies = replyRepository.findByTopicId(topicId);
        // Ordenar as respostas por timestamp para exibi-las em ordem cronológica
        replies.sort(Comparator.comparingLong(MessageReply::getTimestamp));

        List<Map<String, String>> replyData = replies.stream()
                .map(reply -> {
                    Map<String, String> replyMap = new ConcurrentHashMap<>();
                    replyMap.put("id", reply.getId());
                    User authorUser = authHandler.getUserByUsername(reply.getAuthorUserId()); // Obtém o User para pegar o nickname
                    replyMap.put("nick", authorUser != null ? authorUser.getNickname() : "Unknown"); // Nickname do autor
                    replyMap.put("msg", reply.getContent()); // Conteúdo da resposta
                    return replyMap;
                })
                .collect(Collectors.toList());

        ProtocolMessage response = new ProtocolMessage("071");
        response.setMessageList(replyData); // Usa setMessageList para msg_list
        logConsumer.accept("Sent 071 response with " + replyData.size() + " replies for topic '" + topicId + "'.");
        return response;
    }

    /**
     * Manipula a operação 075 (Receber Tópicos).
     * Retorna todos os tópicos existentes no fórum.
     *
     * @param request    A mensagem de requisição.
     * @param clientInfo O ClientInfo associado ao endpoint do cliente.
     * @return ProtocolMessage com a lista de tópicos (076) ou erro (077).
     */
    public ProtocolMessage handleGetTopics(ProtocolMessage request, ClientInfo clientInfo) {
        logConsumer.accept("Attempting to get all topics by client: '" + clientInfo.getName() + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        // Pelo protocolo, 075 C->S não exige token. Assumimos que qualquer cliente conectado pode ver os tópicos.
        // Se você quiser exigir autenticação, adicione a verificação de token aqui.
        /*
        String token = request.getToken();
        ClientInfo authClient = authHandler.getAuthenticatedClientInfo(token);
        if (authClient == null || !authClient.getUserId().equals(clientInfo.getUserId())) {
            logConsumer.accept("Get topics failed: Invalid or expired token.");
            return ProtocolMessage.createErrorMessage("077", "Invalid or expired token.");
        }
        */

        List<Map<String, String>> topicsData = topicRepository.findAll().stream()
                .map(topic -> {
                    Map<String, String> topicMap = new ConcurrentHashMap<>();
                    topicMap.put("id", topic.getId());
                    topicMap.put("title", topic.getTitle());
                    topicMap.put("subject", topic.getSubject());
                    User authorUser = authHandler.getUserByUsername(topic.getAuthorUserId()); // Obtém o User para pegar o nickname
                    topicMap.put("nick", authorUser != null ? authorUser.getNickname() : "Unknown"); // Nickname do autor
                    topicMap.put("msg", topic.getContent()); // Conteúdo completo do tópico
                    return topicMap;
                })
                .collect(Collectors.toList());

        ProtocolMessage response = new ProtocolMessage("076");
        response.setMessageList(topicsData); // Usa setMessageList para msg_list
        logConsumer.accept("Sent 076 response with " + topicsData.size() + " topics.");
        return response;
    }


    /**
     * Transmite um novo tópico (ou tópico atualizado, ex: após exclusão) para todos os clientes autenticados.
     * Isso usa o opcode 055.
     * @param topic O objeto Topic a ser transmitido.
     */
    private void broadcastTopic(Topic topic) {
        ProtocolMessage broadcastTopicMsg = new ProtocolMessage("055");
        broadcastTopicMsg.setTopicId(topic.getId());
        broadcastTopicMsg.setTopicTitle(topic.getTitle());
        broadcastTopicMsg.setTopicSubject(topic.getSubject());
        broadcastTopicMsg.setTopicContent(topic.getContent());

        User authorUser = authHandler.getUserByUsername(topic.getAuthorUserId());
        broadcastTopicMsg.setTopicAuthor(authorUser != null ? authorUser.getNickname() : "Unknown");

        logConsumer.accept("Broadcasting new topic '" + topic.getTitle() + "' (ID: " + topic.getId() + ") to all authenticated clients.");

        for (Map.Entry<String, ObjectOutputStream> entry : new ConcurrentHashMap<>(activeClientOutputs).entrySet()) {
            try {
                SerializationHelper.writeMessage(broadcastTopicMsg, entry.getValue());
                logConsumer.accept("  - Broadcasted (attempted) to client with token: " + entry.getKey());
            } catch (IOException e) {
                logConsumer.accept("Error broadcasting to client " + entry.getKey() + ": " + e.getMessage() + ". Removing client output.");
                activeClientOutputs.remove(entry.getKey());

            }
        }
    }
}