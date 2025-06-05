package book_recommender.lab_b;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Classe principale del client dell'applicazione Book Recommender.
 * Questa classe avvia l'interfaccia grafica JavaFX.
 */
public class Client extends Application {

    // Dimensioni iniziali per la finestra dell'applicazione
    public static final double INITIAL_WIDTH = 1000.0;
    public static final double INITIAL_HEIGHT = 700.0;

    // Dimensioni minime per la finestra dell'applicazione
    public static final double MIN_WIDTH = 1000.0;
    public static final double MIN_HEIGHT = 700.0;

    // ID univoco per questo client
    private final String clientId = UUID.randomUUID().toString();
    private DatabaseManager dbManager;

    // Socket connection to server
    private Socket serverSocket;
    private boolean serverShutdownDetected = false;

    // Connessione remota
    private String dbUrl;
    private String dbUser = "book_admin_8530"; // Credenziali fisse per vedere il db su pgadmin
    private String dbPassword = "CPuc#@r-zbKY"; // Credenziali fisse per vedere il db su pgadmin

    // Flag per usare ngrok - sempre true
    private boolean useNgrok = true;

    // Riferimento allo Stage principale
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Salva il riferimento allo stage principale
        this.primaryStage = primaryStage;

        // Try to connect to the server first
        try {
            // Ngrok è sempre attivo
            useNgrok = true;

            // Ask for database connection parameters (solo ngrok host e porta)
            boolean parametersProvided = getDatabaseConnectionParameters();
            if (!parametersProvided) {
                Platform.exit();
                return;
            }

            // Creiamo un indicatore di caricamento mentre proviamo a connetterci
            ProgressIndicator progress = new ProgressIndicator();
            progress.setMaxSize(100, 100);

            VBox loadingBox = new VBox(10);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.getChildren().addAll(
                    new Label("Tentativo di connessione al database..."),
                    progress
            );

            Scene loadingScene = new Scene(loadingBox, 300, 200);
            primaryStage.setScene(loadingScene);
            primaryStage.show();

            // Eseguiamo la connessione in un thread separato per non bloccare l'UI
            Task<Boolean> connectionTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    try {
                        // Establish database connection
                        dbManager = DatabaseManager.createRemoteInstance(dbUrl, dbUser, dbPassword);

                        // Register client connection
                        registerClientConnection(true);
                        return true;
                    } catch (Exception e) {
                        System.err.println("ERRORE: Impossibile connettersi al server. Assicurarsi che il server sia in esecuzione.");
                        System.err.println("Dettagli: " + e.getMessage());
                        return false;
                    }
                }
            };

            connectionTask.setOnSucceeded(event -> {
                Boolean success = connectionTask.getValue();
                if (success) {
                    try {
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

                        // Avvia il monitoraggio del server
                        startServerMonitoring();

                    } catch (Exception e) {
                        showServerErrorAlert(primaryStage, "Errore applicazione",
                                "Errore durante il caricamento dell'interfaccia",
                                "Si è verificato un errore durante il caricamento dell'interfaccia: " + e.getMessage());
                    }
                } else {
                    showServerErrorAlert(primaryStage, "Errore di connessione",
                            "Connessione al database fallita",
                            "Impossibile connettersi al database. Verificare che il server sia in esecuzione e che i parametri di connessione siano corretti.");
                }
            });

            connectionTask.setOnFailed(event -> {
                Throwable exception = connectionTask.getException();
                showServerErrorAlert(primaryStage, "Errore di connessione",
                        "Connessione al database fallita",
                        "Impossibile connettersi al database: " + exception.getMessage() +
                                "\nL'applicazione verrà chiusa. Verificare i parametri di connessione.");
            });

            // Avvia il task di connessione
            new Thread(connectionTask).start();

        } catch (Exception e) {
            System.err.println("ERRORE: Impossibile connettersi al server. Assicurarsi che il server sia in esecuzione.");
            System.err.println("Dettagli: " + e.getMessage());
            showServerErrorAlert(primaryStage, "Errore di connessione",
                    "Connessione al database fallita",
                    "Impossibile connettersi al database: " + e.getMessage() +
                            "\nL'applicazione verrà chiusa. Verificare i parametri di connessione.");
        }
    }

    /**
     * Chiedi all'utente i parametri di connessione al database (solo ngrok host e porta)
     * e verifica che siano validi prima di procedere
     * @return true se i parametri sono stati forniti e sono validi, false altrimenti
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
        hostField.setPromptText("Hostname ngrok");

        TextField portField = new TextField();
        portField.setPromptText("Porta ngrok");

        // Aggiungi solo i campi per host e porta alla griglia
        grid.add(new Label("Host ngrok:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Porta ngrok:"), 0, 1);
        grid.add(portField, 1, 1);

        // Aggiunge la griglia al dialog
        dialog.getDialogPane().setContent(grid);

        // Mostra il dialog e aspetta che l'utente faccia una scelta
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // L'utente ha confermato, procedi con i parametri forniti
            String host = hostField.getText().trim();
            String port = portField.getText().trim();

            // Verifica che i parametri non siano vuoti
            if (host.isEmpty() || port.isEmpty()) {
                showConnectionParametersError();
                return false;
            }

            // Verifica che la porta sia un numero valido
            try {
                int portNumber = Integer.parseInt(port);
                if (portNumber <= 0 || portNumber > 65535) {
                    showConnectionParametersError();
                    return false;
                }
            } catch (NumberFormatException e) {
                showConnectionParametersError();
                return false;
            }

            // Costruisci URL di connessione JDBC
            dbUrl = "jdbc:postgresql://" + host + ":" + port + "/book_recommender";

            // Qui possiamo anche verificare preliminarmente se la connessione è possibile
            // prima di procedere con la creazione dell'istanza DatabaseManager
            try {
                // Prova a fare un rapido test di connessione
                Connection testConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                testConnection.close();
                return true;
            } catch (SQLException e) {
                System.err.println("Test di connessione fallito: " + e.getMessage());
                showConnectionParametersError();
                return false;
            }
        }

        // L'utente ha annullato
        return false;
    }

    /**
     * Mostra una finestra di dialogo di errore quando i parametri di connessione sono mancanti o non validi
     */
    private void showConnectionParametersError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore di connessione");
        alert.setHeaderText("Parametri di connessione mancanti o errari ");
        alert.setContentText("È necessario fornire i parametri di connessione correti al database. \nL'applicazione verrà chiusa.");
        alert.showAndWait();
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
            String clientIdShort = clientId.substring(0, 8); // Use a shorter ID for better readability

            // Update active_clients table in database
            if (dbManager != null) {
                dbManager.updateClientConnection(clientIdShort, isConnecting);
                System.out.println("Client " + (isConnecting ? "registered" : "unregistered") +
                        " with ID: " + clientIdShort);
            } else {
                System.err.println("Database manager is null, cannot register client");
            }
        } catch (Exception e) {
            System.err.println("Error registering client connection: " + e.getMessage());
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
        } catch (IOException e) {
            System.err.println("Errore durante la chiusura della connessione: " + e.getMessage());
        }
    }

    /**
     * Avvia il monitoraggio del server per rilevare quando viene spento
     */
    private void startServerMonitoring() {
        Thread monitorThread = new Thread(() -> {
            while (!serverShutdownDetected && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(3000); // Controlla ogni 3 secondi

                    // Prova a controllare la connessione al database
                    if (dbManager != null) {
                        try {
                            Connection conn = dbManager.getConnection();
                            // Se la connessione fallisce, lancerà un'eccezione
                        } catch (SQLException e) {
                            // Connessione persa, segnala la disconnessione
                            Platform.runLater(() -> {
                                showServerDisconnectedScreen();
                            });
                            serverShutdownDetected = true;
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Se c'è un errore, mostra la schermata di disconnessione
                    Platform.runLater(() -> {
                        showServerDisconnectedScreen();
                    });
                    serverShutdownDetected = true;
                    break;
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Mostra la schermata di disconnessione del server
     */
    private void showServerDisconnectedScreen() {
        try {
            // Interrompi eventuale monitoraggio per evitare chiamate multiple
            serverShutdownDetected = true;

            // Carica il layout FXML per la schermata di disconnessione
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/server_disconnected.fxml"));
            Parent root = loader.load();

            // Crea una nuova scena con lo schermo di disconnessione
            Scene scene = new Scene(root, 600, 400);

            // Applica la scena alla finestra primaria
            Platform.runLater(() -> {
                primaryStage.setScene(scene);
                primaryStage.setTitle("Server Disconnesso");
                primaryStage.setResizable(false);
                primaryStage.centerOnScreen();
            });

        } catch (IOException e) {
            System.err.println("Errore nel caricare la schermata di disconnessione: " + e.getMessage());

            // Fallback nel caso in cui non si riesca a caricare il FXML
            Platform.runLater(() -> {
                showServerShutdownAlert(primaryStage);
            });
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