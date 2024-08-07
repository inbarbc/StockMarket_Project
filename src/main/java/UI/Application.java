package UI;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

import java.net.ServerSocket;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication
@Theme(value = "flowcrmtutorial")
@ComponentScan(basePackages = { "UI" })
public class Application implements AppShellConfigurator, WebMvcConfigurer {

    // @Autowired
    // private WebSocketClient webSocketClient;

    @SuppressWarnings("unused")
    private static int port;
    // Static variable to store the token
    public static String token;

    public static void main(String[] args) {
        port = findAvailablePort();
        // System.setProperty("server.port", "8081");
        System.setProperty("server.port", String.valueOf(port));
        // System.out.println("Server port: " + port);
        SpringApplication app = new SpringApplication(Application.class);
        app.run(args);
    }

    /**
     * Finds an available port on the local machine.
     *
     * @return an available port number
     */
    private static int findAvailablePort() {
        // Try-with-resources to ensure the ServerSocket is closed automatically
        try (ServerSocket socket = new ServerSocket(0)) {
            // The port number assigned by the operating system
            return socket.getLocalPort();
        } catch (Exception e) {
            // Throw a RuntimeException if an error occurs while finding an available port
            throw new RuntimeException("Failed to find an available port", e);
        }
    }

}
