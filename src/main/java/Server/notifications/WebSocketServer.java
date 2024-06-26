package Server.notifications;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ServiceLayer.TokenService;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

//Todo: make sure the class is thread safe.
/**
 * This class is responsible for handling WebSocket communication within the
 * application.
 * It extends TextWebSocketHandler, which provides a simple way to handle text
 * messages over WebSocket connections.
 * The WebSocketServer class provides methods for sending messages to specific
 * clients.
 */
@Component
public class WebSocketServer extends TextWebSocketHandler {
    // @Autowired
    private TokenService tokenService;
    // Singleton instance
    private static WebSocketServer instance;
    // assumption messages as aformat of:"targetUsername:message"

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>(); // registered user ->
                                                                                             // <username,session>,
                                                                                             // guest -> <token,session>
    private static final Map<String, Queue<String>> messageQueues = new ConcurrentHashMap<>(); // <username,
                                                                                               // messageQueue>

    // Private constructor to prevent instantiation
    private WebSocketServer() {
        // Initialization code
        this.tokenService = TokenService.getTokenService();
    }

    // Method to get singleton instance
    public static synchronized WebSocketServer getInstance() {
        if (instance == null) {
            instance = new WebSocketServer();
        }
        return instance;
    }

    /**
     * Handles a new WebSocket connection after it has been established.
     * This method is called after the connection is opened.
     *
     * @param session The WebSocket session for the newly opened connection.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String query = uri.getQuery();
        String token = null;

        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    token = param.split("token=")[1];
                }
            }
        }
        if (token == null || !validateToken(token)) {
            session.close(CloseStatus.BAD_DATA);
            System.out.println("Invalid token, connection closed");
            return;
        }

        String username = tokenService.extractUsername(token);
        String clientKey = (username != null) ? username : "guest-" + token;
        // String clientKey = "guest-" + token;

        if (username != null && tokenService.isUserAndLoggedIn(token)) {
            // User is logged in
            sessions.put(clientKey, session);
            System.out.println("Connected: " + clientKey);
        } else {
            // User is a guest
            sessions.put(clientKey, session);
            System.out.println("Connected: " + clientKey);
        }
    }

    // check for any queued message and if exist send them to the client
    public void checkForQueuedMessages(String username) {
        WebSocketSession session = sessions.get(username);
        if (session != null && session.isOpen()) {
            Queue<String> queue = messageQueues.getOrDefault(username, new ConcurrentLinkedQueue<>());
            while (!queue.isEmpty()) {
                String message = queue.poll();
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            messageQueues.remove(username);
        }
    }

    /**
     * Handles the closing of a WebSocket connection.
     * This method is called when the connection with a client is closed.
     *
     * @param session The WebSocket session that was closed.
     * @param status  The status code for the closure.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.values().remove(session);
        System.out.println("Disconnected: " + session.getId());
    }

    /**
     * Handles a text message received from a client.
     * This method is called when a new text message arrives.
     *
     * @param session The WebSocket session from which the message was received.
     * @param message The text message received.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Assuming the message format is "targetUsername:message"
        String[] parts = message.getPayload().split(":", 2);
        if (parts.length == 2) {
            String targetUsername = parts[0];
            String msg = parts[1];

            // Send message to the target client
            WebSocketSession targetSession = sessions.get(targetUsername);
            if (targetSession != null && targetSession.isOpen()) {
                targetSession.sendMessage(new TextMessage(msg));
            } else {
                // Queue message for later delivery
                messageQueues.computeIfAbsent(targetUsername, k -> new ConcurrentLinkedQueue<>()).add(msg);
                System.out.println("Client not found or not open, message queued for : " + targetUsername);
            }
        } else {
            System.out.println("Invalid message format");
        }
    }

    /**
     * Broadcasts a text message to all connected users.
     *
     * @param message The message to be broadcasted.
     * @throws IOException If an I/O error occurs while sending the message.
     */
    public void broadcastMessage(String message) {
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    System.out.println("Broadcasted message to: " + entry.getKey());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sends a text message to a specific user.
     * This method looks up the session for the username and sends the message if
     * the session exists.
     * if it is a registered user but not logged in than the message will be queued.
     * 
     * @param username The username of the recipient.
     * @param message  The message to be sent.
     * @throws IOException If an I/O error occurs while sending the message.
     */
    public void sendMessage(String targetUser, String message) {
        WebSocketSession session = sessions.get(targetUser);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                System.out.println("Sent message to: " + targetUser);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Queue message for later delivery
            messageQueues.computeIfAbsent(targetUser, k -> new ConcurrentLinkedQueue<>()).add(message);
            System.out.println("Client not found or not open, message queued for : " + targetUser);
        }
    }

    private boolean validateToken(String token) {
        // return tokenService.validateToken(token);
        return true;
    }

    /**
     * Replaces a guest user's token with a logged-in user's new token.
     * This method is called when a guest user logs in and their token is replaced
     * with a new token.
     * 
     * @param oldToken The old token of the guest user.
     * @param newToken The new token for the logged-in user.
     * @param username The username of the logged-in user.
     */
    public void replaceGuestTokenToUserToken(String oldToken, String newToken, String username) {
        if (username != null && oldToken != null) {
            String guestKey = "guest-" + oldToken;
            if (sessions.containsKey(guestKey)) {
                WebSocketSession session = sessions.get(guestKey);
                sessions.remove(guestKey);
                if (session != null) {
                    sessions.put(username, session);
                }
            }
        }
        checkForQueuedMessages(username);
    }

    public void changeLoggedInSession(String userName, String guestToken) {
        WebSocketSession session = sessions.get(userName);
        if (session != null) {
            try {
                sessions.remove(userName);
                String guestKey = "guest-" + guestToken;
                sessions.put(guestKey, session);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}