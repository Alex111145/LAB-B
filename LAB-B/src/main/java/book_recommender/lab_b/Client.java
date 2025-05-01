package book_recommender.lab_b;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;
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
    private final String clientId = UUID.randomUUID().toString();
    private DatabaseManager dbManager;

    // Socket connection to server
    private Socket serverSocket;
    private boolean serverShutdownDetected = false;

    // Connessione remota
    private String dbUrl;
    private String dbUser = "book_admin_8530"; // Credenziali fisse
    private String dbPassword = "CPuc#@r-zbKY"; // Credenziali fisse

    // Flag per usare ngrok - sempre true
    private boolean useNgrok = true;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Try to connect to the server first
        try {
            // Ngrok è sempre attivo
            useNgrok = true;

            // Ask for database connection parameters (solo ngrok host e porta)
            if (!getDatabaseConnectionParameters()) {
                showServerErrorAlert(primaryStage, "Errore di connessione",
                        "Parametri di connessione mancanti",
                        "È necessario fornire i parametri di connessione al database. L'applicazione verrà chiusa.");
                return;
            }

            // Then establish a database connection
            dbManager = DatabaseManager.createRemoteInstance(dbUrl, dbUser, dbPassword);

            // Register client connection
            registerClientConnection(true);

        } catch (Exception e) {
            System.err.println("ERRORE: Impossibile connettersi al server. Assicurarsi che il server sia in esecuzione.");
            System.err.println("Dettagli: " + e.getMessage());
            showServerErrorAlert(primaryStage, "Errore di connessione",
                    "Connessione al database fallita",
                    "Impossibile connettersi al database: " + e.getMessage() +
                            "\nL'applicazione verrà chiusa. Verificare i parametri di connessione.");
            return;
        }

        // Load the main page
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/book_recommender/lab_b/homepage.fxml")));

        // Set the title and scene with initial dimensions
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
    }

    /**
     * Chiedi all'utente i parametri di connessione al database (solo ngrok host e porta)
     * @return true se i parametri sono stati forniti, false altrimenti
     */
    private boolean getDatabaseConnectionParameters() {
        // Creiamo un dialog personalizzato per i parametri di connessione
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Connessione al Database");
        dialog.setHeaderText("Inserisci i parametri di connessione via ngrok");

        // Pulsanti
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Griglia per i campi
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Campi di ingresso - solo host e porta ngrok
        TextField hostField = new TextField();
        hostField.setPromptText("Hostname ngrok (senza tcp://)");

        TextField portField = new TextField();
        portField.setPromptText("Porta ngrok");

        // Mostra le credenziali fisse (solo informativo)
        Label dbNameLabel = new Label("book_recommender");
        dbNameLabel.setStyle("-fx-font-weight: bold;");

        Label userLabel = new Label(dbUser);
        userLabel.setStyle("-fx-font-weight: bold;");

        Label passwordLabel = new Label(dbPassword);
        passwordLabel.setStyle("-fx-font-weight: bold;");

        // Aggiungi i campi alla griglia
        grid.add(new Label("Host ngrok:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Porta ngrok:"), 0, 1);
        grid.add(portField, 1, 1);
        grid.add(new Label("Database:"), 0, 2);
        grid.add(dbNameLabel, 1, 2);
        grid.add(new Label("Username:"), 0, 3);
        grid.add(userLabel, 1, 3);
        grid.add(new Label("Password:"), 0, 4);
        grid.add(passwordLabel, 1, 4);

        // Aggiunge la griglia al dialog
        dialog.getDialogPane().setContent(grid);

        // Opzionale: preselezione il primo campo
        Platform.runLater(hostField::requestFocus);

        // Mostra il dialog e aspetta che l'utente faccia una scelta
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // L'utente ha confermato, procedi con i parametri forniti
            String host = hostField.getText().trim();
            String port = portField.getText().trim();

            // Verifica che i parametri non siano vuoti
            if (host.isEmpty() || port.isEmpty()) {
                return false;
            }

            // Costruisci URL di connessione JDBC
            dbUrl = "jdbc:postgresql://" + host + ":" + port + "/book_recommender";

            return true;
        }

        // L'utente ha annullato
        return false;
    }

    /**
     * Show an alert when the server shuts down
     */
    private void showServerShutdownAlert(Stage primaryStage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore di Connessione");
        alert.setHeaderText("Server Spento");
        alert.setContentText("Il server è stato spento. L'applicazione verrà chiusa. Riavviare il server prima di riaprire il client.");

        // Wait for the alert to be closed before exiting
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

        // Wait for the alert to be closed before exiting
        alert.showAndWait().ifPresent(response -> {
            Platform.exit();
            System.exit(1);
        });
    }

    /**
     * Register client connection or disconnection in the database
     */
    private void registerClientConnection(boolean isConnecting) {
        try {
            // Create a shorter client ID (just UUID, without hostname and IP)
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
        // Remove the client from count when the application terminates
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
        launch(args);
    }
}