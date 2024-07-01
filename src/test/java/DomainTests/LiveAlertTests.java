package DomainTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import java.time.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LiveAlertTests {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private int testPort; 

    @BeforeEach
    public void setUp() throws Exception {
        // Setup mock server
        serverSocket = new ServerSocket(0);
        testPort = serverSocket.getLocalPort();
        new Thread(() -> {
            try {
                clientSocket = serverSocket.accept();
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Simulate server response for shop close request
                String request = in.readLine();
                if ("CLOSE_SHOP".equals(request)) {
                    out.println("The shop has been closed");
                } else if ("WRONG_MESSAGE".equals(request)) {
                    out.println("Expected error message or behavior");
                } else if ("REQUEST_MESSAGE".equals(request)) {
                    out.println("Expected response from server");
                } else if ("REMOVE_SUBSCRIPTION".equals(request)) {
                    out.println("Subscription successfully removed");
                } else if ("CUSTOMER_BUY".equals(request)) {
                    out.println("Purchase alert: Customer bought an item");
                } else if ("NO_RESPONSE".equals(request)) {
                    // Do nothing
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }

    @Test
    public void testCloseStoreAlert() throws Exception {
        // Simulate sending a close shop request and receiving the alert
        try (Socket testClient = new Socket("localhost", testPort);
                PrintWriter testOut = new PrintWriter(testClient.getOutputStream(), true);
                BufferedReader testIn = new BufferedReader(new InputStreamReader(testClient.getInputStream()))) {

            // Send close shop request
            testOut.println("CLOSE_SHOP");

            // Wait for and read the response
            String response = testIn.readLine();

            // Assert the response is as expected
            assertEquals("The shop has been closed", response, "Expected shop close alert message not received");
        }
    }

    @Test
    public void testCloseStoreAlertWrongMessage() {
        assertTimeout(Duration.ofMillis(5000), () -> {
            // Simulate sending a close shop request and receiving the alert
            try (Socket testClient = new Socket("localhost", testPort);
                    PrintWriter testOut = new PrintWriter(testClient.getOutputStream(), true);
                    BufferedReader testIn = new BufferedReader(new InputStreamReader(testClient.getInputStream()))) {

                // Send close shop request
                testOut.println("WRONG_MESSAGE");

                // Wait for and read the response
                String response = testIn.readLine();

                // Assert the response is as expected
                assertEquals("Expected error message or behavior", response, "Unexpected response for wrong message");
            }
        });

    }

    @Test
    public void testReceiveAlertFromServer() throws Exception {
        // Simulate sending a request to the server and receiving a response
        try (Socket testClient = new Socket("localhost", testPort);
                PrintWriter testOut = new PrintWriter(testClient.getOutputStream(), true);
                BufferedReader testIn = new BufferedReader(new InputStreamReader(testClient.getInputStream()))) {

            // Send a message that is expected to receive a response from the server
            testOut.println("REQUEST_MESSAGE");

            // Wait for and read the response
            String response = testIn.readLine();

            // Assert the response is as expected
            assertEquals("Expected response from server", response, "Unexpected response received from server");
        }

    }

    @Test
    public void testSubscriptionRemovalAlert() throws Exception {
        try (Socket testClient = new Socket("localhost", testPort);
                PrintWriter testOut = new PrintWriter(testClient.getOutputStream(), true);
                BufferedReader testIn = new BufferedReader(new InputStreamReader(testClient.getInputStream()))) {

            // Send a message or command that triggers the removal of a subscription
            testOut.println("REMOVE_SUBSCRIPTION");

            // Wait for and read the response
            String response = testIn.readLine();

            // Assert the response is as expected
            assertEquals("Subscription successfully removed", response, "Unexpected response received from server");
        }

    }

    @Test
    public void testCustomerBuysFromStoreAlert() throws Exception {
        try (Socket testClient = new Socket("localhost", testPort);
                PrintWriter testOut = new PrintWriter(testClient.getOutputStream(), true);
                BufferedReader testIn = new BufferedReader(new InputStreamReader(testClient.getInputStream()))) {

            // Send a message or command that simulates a customer buying from the store
            testOut.println("CUSTOMER_BUY");

            // Wait for and read the response
            String response = testIn.readLine();

            // Assert the response is as expected
            assertEquals("Purchase alert: Customer bought an item", response,
                    "Unexpected response received from server");
        }
    }

    // Q: what more to tes?
    // A: We can test more scenarios such as sending a message that triggers an
    // alert for a customer buying from the store
    // A: We can also test the case where the server does not respond to a message
    // sent by the client
    // A: We can test the case where the server responds with an error message when
    // the client sends a message
    // please implement the all above tests

    // test test the case where the server does not respond to a message sent by the
    // client
    @Test
    public void testServerDoesNotRespond() {
        assertTimeout(Duration.ofMillis(500), () -> {
            // Simulate sending a close shop request and receiving the alert
            try (Socket testClient = new Socket("localhost", testPort)) {
                testClient.setSoTimeout(200); // Set socket read timeout to 200ms
                try (PrintWriter testOut = new PrintWriter(testClient.getOutputStream(), true);
                        BufferedReader testIn = new BufferedReader(
                                new InputStreamReader(testClient.getInputStream()))) {

                    // Send close shop request
                    testOut.println("NO_RESPONSE");

                    // Wait for and read the response
                    String response = null;
                    try {
                        response = testIn.readLine();
                    } catch (SocketTimeoutException e) {
                        // Expected behavior, the server does not respond
                    }

                    // Assert the response is as expected
                    assertEquals(null, response, "Unexpected response for no response from server");
                }
            }
        });
    }

}
