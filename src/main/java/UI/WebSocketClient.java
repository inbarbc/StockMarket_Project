package UI;

import org.springframework.stereotype.Component;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import java.net.URI;

@Component
@ClientEndpoint
public class WebSocketClient {

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected to server start");
        this.session = session;
        System.out.println("Connected to server end");

    }

    /**
     * Sends an alert message to a specified user via the WebSocket server.
     * Converts the Alert object to a string message before sending.
     *
     * @param targetUsername The username of the recipient.
     * @param alert          The Alert object containing the message to be sent.
     */
    @OnClose
    public void onClose() {
        System.out.println("Disconnected from server");
        this.session = null;
    }

    /**
     * Handles incoming messages from the server.
     * This method is called when a message is received from the server.
     *
     * @param message The message received from the server.
     */
    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received from server: " + message);
    }

    /**
     * Sends a message to the server.
     * This method sends a message to the server if the session is open.
     *
     * @param message The message to be sent to the server.
     */
    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        }
    }

    /**
     * Sends a message to a specific client through the server.
     * This method formats the message with the target client's ID before sending.
     *
     * @param targetClientId The ID of the target client.
     * @param message        The message to be sent.
     */
    public void sendMessage(String targetClientId, String message) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(targetClientId + ":" + message);
        }
    }

    /**
     * Initiates a connection to the WebSocket server with a specified token.
     * This method attempts to connect to the server using the provided token for
     * authentication.
     *
     * @param token The authentication token to use for the connection.
     */
    public void connect(String token) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI uri = new URI("ws://localhost:8080/websocket?token=" + token); // Ensure the server is running on this
                                                                               // endpoint
            container.connectToServer(this, uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}