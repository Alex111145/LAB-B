package book_recommender.lab_b;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.UUID;

/**
 * Classe principale del client dell'applicazione Book Recommender.
 * Questa classe avvia l'interfaccia grafica JavaFX.
 */
public class Client extends Application {

    // Dimensioni iniziali per la finestra dell'applicazione
    public static final double INITIAL_WIDTH = 800.0;
    public static final double INITIAL_HEIGHT = 600.0;

    // Dimensioni minime per la finestra dell'applicazione
    public static final double MIN_WIDTH = 600.0;
    public static final double MIN_HEIGHT = 400.0;

    // ID univoco per questo client
    private String clientId = UUID.randomUUID().toString();
    private DatabaseManager dbManager;

    // Socket connection to server
    private Socket serverSocket;
    private boolean serverShutdownDetected = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Try to connect to the server first
        try {
            // Try to connect to the server socket
            connectToServer();

            // Then establish database connection
            dbManager = DatabaseManager.getInstance();

            // Register client connection
            registerClientConnection(true);

        } catch (Exception e) {
            System.err.println("ERRORE: Impossibile connettersi al server. Assicurarsi che il server sia in esecuzione.");
            System.err.println("Dettagli: " + e.getMessage());
            showServerErrorAlert(primaryStage, "Errore di connessione",
                    "Server spento",
                    "Il server è spento. L'applicazione verrà chiusa. Riavviare il server prima di riaprire il client.");
            return;
        }

        // Load main page
        Parent root = FXMLLoader.load(getClass().getResource("/book_recommender/lab_b/homepage.fxml"));

        // Set title and scene with initial dimensions
        primaryStage.setTitle("Book Recommender - Client");
        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
        primaryStage.setScene(scene);

        // Set minimum window dimensions
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);

        // Allow window resizing
        primaryStage.setResizable(true);

        // Show window
        primaryStage.show();

        // Start a server listener thread to detect server shutdown
        if (serverSocket != null && !serverSocket.isClosed()) {
            startServerListener(primaryStage);
        }
    }

    /**
     * Connect to the server socket
     */
    private void connectToServer() throws IOException {
        // Get the address of the server
        // This will work both on the same machine and across networks
        String serverAddress = "localhost"; // Default to localhost

        try {
            serverSocket = new Socket(serverAddress, 8888);
        } catch (IOException e) {
            // If local connection fails, try asking the user for the server address using JavaFX dialog

            // Create a TextInputDialog
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("192.168.1.1");
            dialog.setTitle("Server Connection");
            dialog.setHeaderText("Server not found on localhost");
            dialog.setContentText("Enter the IP address of the server:");

            // Traditional JavaFX way to get the response value
            java.util.Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                serverAddress = result.get();
                if (serverAddress != null && !serverAddress.trim().isEmpty()) {
                    serverSocket = new Socket(serverAddress, 8888);
                } else {
                    throw new IOException("No server address provided");
                }
            } else {
                throw new IOException("No server address provided");
            }
        }
    }
    /**
     * Start a thread to listen for server shutdown messages
     */
    private void startServerListener(Stage primaryStage) {
        Thread listener = new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                String message;

                while (!serverShutdownDetected && (message = in.readLine()) != null) {
                    if ("SERVER_SHUTDOWN".equals(message)) {
                        serverShutdownDetected = true;

                        // Show alert on JavaFX thread
                        Platform.runLater(() -> {
                            showServerShutdownAlert(primaryStage);
                        });

                        break;
                    }
                }
            } catch (IOException e) {
                // If there's a connection error, also show the server shutdown alert
                if (!serverShutdownDetected) {
                    serverShutdownDetected = true;
                    Platform.runLater(() -> {
                        showServerShutdownAlert(primaryStage);
                    });
                }
            }
        });

        listener.setDaemon(true);
        listener.start();
    }

    /**
     * Show an alert when the server shuts down
     */
    private void showServerShutdownAlert(Stage primaryStage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore di Connessione");
        alert.setHeaderText("Server Spento");
        alert.setContentText("Il server è stato spento. L'applicazione verrà chiusa. Riavviare il server prima di riaprire il client.");

        // Wait for alert to be closed before exiting
        alert.showAndWait().ifPresent(response -> {
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Show a generic server error alert
     */
    private void showServerErrorAlert(Stage primaryStage, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Wait for alert to be closed before exiting
        alert.showAndWait().ifPresent(response -> {
            Platform.exit();
            System.exit(1);
        });
    }

    /**
     * Register client connection or disconnection in the database
     * @param isConnecting true if client is connecting, false if disconnecting
     */
    private void registerClientConnection(boolean isConnecting) {
        try {
            // Create shorter client ID (just UUID, without hostname and IP)
            String clientIdShort = clientId.substring(0, 32); // Take only UUID

            // Update active_clients table in database
            dbManager.updateClientConnection(clientIdShort, isConnecting);

        } catch (Exception e) {
            System.err.println("Errore durante la registrazione del client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Remove client from count when application terminates
        try {
            if (dbManager != null && !serverShutdownDetected) {
                // Register client disconnection
                registerClientConnection(false);
            }

            // Close socket connection
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            System.err.println("Errore durante la chiusura della connessione: " + e.getMessage());
        }
    }

    /**
     * Main method for client application
     * @param args command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Book Recommender Client");
        launch(args);
    }
}