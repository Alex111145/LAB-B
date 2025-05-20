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
 * Classe principale dell'applicazione client di Book Recommender.
 * Gestisce l'avvio dell'interfaccia utente, la connessione al server remoto tramite ngrok,
 * e il monitoraggio continuo dello stato della connessione al server.
 */
public class Client extends Application {

    // Costanti per il dimensionamento dell'interfaccia
    /** Larghezza iniziale della finestra dell'applicazione */
    public static final double INITIAL_WIDTH = 1000.0;
    /** Altezza iniziale della finestra dell'applicazione */
    public static final double INITIAL_HEIGHT = 700.0;
    /** Larghezza minima della finestra dell'applicazione */
    public static final double MIN_WIDTH = 1000.0;
    /** Altezza minima della finestra dell'applicazione */
    public static final double MIN_HEIGHT = 700.0;

    /** Identificatore univoco per questa istanza del client, utilizzato per il tracciamento */
    private final String clientId = UUID.randomUUID().toString();
    /** Gestore del database per le operazioni sul database remoto */
    private DatabaseManager dbManager;

    /** Socket per la connessione al server */
    private Socket serverSocket;
    /** Flag che indica se è stato rilevato lo spegnimento del server */
    private boolean serverShutdownDetected = false;

    // Parametri di connessione al database
    /** URL JDBC per la connessione al database */
    private String dbUrl;
    /** Nome utente predefinito per la connessione al database */
    private String dbUser = "book_admin_8530"; // Credenziali fisse
    /** Password predefinita per la connessione al database */
    private String dbPassword = "CPuc#@r-zbKY"; // Credenziali fisse

    /** Flag che indica se utilizzare ngrok per la connessione remota (sempre true) */
    private boolean useNgrok = true;

    /** Riferimento allo Stage principale dell'applicazione JavaFX */
    private Stage primaryStage;


    /**
     * Metodo principale per l'avvio dell'applicazione JavaFX.
     * Gestisce la configurazione iniziale, richiede i parametri di connessione all'utente,
     * stabilisce la connessione al database remoto e carica l'interfaccia utente.
     *
     * @param primaryStage Stage principale dell'applicazione JavaFX
     * @throws Exception Se si verifica un errore durante l'inizializzazione
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Salva il riferimento allo stage principale per uso futuro
        this.primaryStage = primaryStage;

        // Tenta la connessione al server
        try {
            // Imposta il flag ngrok (sempre attivo in questa versione)
            useNgrok = true;

            // Loop per richiedere i parametri di connessione finché non sono validi
            boolean parametersProvided = false;
            while (!parametersProvided) {
                parametersProvided = getDatabaseConnectionParameters();
                if (!parametersProvided) {
                    // Se l'utente annulla invece di riprovare, chiude l'applicazione
                    if (!retryConnectionDialog()) {
                        Platform.exit();
                        return;
                    }
                    // Se l'utente vuole riprovare, il ciclo continua
                }
            }

            // Crea e visualizza una schermata di caricamento mentre si tenta la connessione
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

            // Esegue la connessione in un thread separato per mantenere reattiva l'interfaccia utente
            Task<Boolean> connectionTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    try {
                        // Stabilisce la connessione al database remoto
                        dbManager = DatabaseManager.createRemoteInstance(dbUrl, dbUser, dbPassword);

                        // Registra la connessione del client nella tabella active_clients
                        registerClientConnection(true);
                        return true;
                    } catch (Exception e) {
                        // In caso di errore, restituisce false
                        return false;
                    }
                }
            };

            // Configura il comportamento in caso di successo della connessione
            connectionTask.setOnSucceeded(event -> {
                Boolean success = connectionTask.getValue();
                if (success) {
                    try {
                        // Carica la pagina principale (homepage)
                        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/book_recommender/lab_b/homepage.fxml")));

                        // Configura la finestra principale
                        primaryStage.setTitle("Book Recommender - Client");
                        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
                        primaryStage.setScene(scene);

                        // Imposta le dimensioni minime della finestra
                        primaryStage.setMinWidth(MIN_WIDTH);
                        primaryStage.setMinHeight(MIN_HEIGHT);

                        // Permette il ridimensionamento della finestra
                        primaryStage.setResizable(true);

                        // Avvia il thread di monitoraggio per rilevare disconnessioni dal server
                        startServerMonitoring();

                    } catch (Exception e) {
                        // Mostra un avviso in caso di errore nel caricamento dell'interfaccia
                        showServerErrorAlert(primaryStage, "Errore applicazione",
                                "Errore durante il caricamento dell'interfaccia",
                                "Si è verificato un errore durante il caricamento dell'interfaccia: " + e.getMessage());
                    }
                } else {
                    // In caso di errore di connessione, riavvia il processo di connessione
                    boolean retry = retryConnectionDialog();
                    if (retry) {
                        try {
                            // Riavvia il processo di connessione
                            start(primaryStage);
                        } catch (Exception e) {
                            showServerErrorAlert(primaryStage, "Errore fatale",
                                    "Errore durante il riavvio dell'applicazione",
                                    "Impossibile riavviare il processo di connessione: " + e.getMessage());
                        }
                    } else {
                        // L'utente ha scelto di non riprovare, chiude l'applicazione
                        Platform.exit();
                    }
                }
            });

            // Configura il comportamento in caso di errore durante la connessione
            connectionTask.setOnFailed(event -> {
                Throwable exception = connectionTask.getException();
                boolean retry = retryConnectionDialog();
                if (retry) {
                    try {
                        // Riavvia il processo di connessione
                        start(primaryStage);
                    } catch (Exception e) {
                        showServerErrorAlert(primaryStage, "Errore fatale",
                                "Errore durante il riavvio dell'applicazione",
                                "Impossibile riavviare il processo di connessione: " + e.getMessage());
                    }
                } else {
                    // L'utente ha scelto di non riprovare, chiude l'applicazione
                    Platform.exit();
                }
            });

            // Avvia il task di connessione in un thread separato
            new Thread(connectionTask).start();

        } catch (Exception e) {
            // Gestisce eventuali eccezioni non catturate
            boolean retry = retryConnectionDialog();
            if (retry) {
                try {
                    // Riavvia il processo di connessione
                    start(primaryStage);
                } catch (Exception ex) {
                    showServerErrorAlert(primaryStage, "Errore fatale",
                            "Errore durante il riavvio dell'applicazione",
                            "Impossibile riavviare il processo di connessione: " + ex.getMessage());
                }
            } else {
                // L'utente ha scelto di non riprovare, chiude l'applicazione
                Platform.exit();
            }
        }
    }

    /**
     * Richiede all'utente di inserire i parametri di connessione al database tramite un dialog.
     * Verifica che i parametri inseriti siano validi effettuando una connessione di prova.
     *
     * @return true se i parametri sono stati forniti e sono validi, false altrimenti
     */
    private boolean getDatabaseConnectionParameters() {
        // Crea un dialog personalizzato per i parametri di connessione
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Connessione al Database");
        dialog.setHeaderText("Inserisci i parametri di connessione via ngrok");

        // Aggiunge i pulsanti OK e Cancel al dialog
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Crea una griglia per i campi di input
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Crea i campi di input per host e porta ngrok
        TextField hostField = new TextField();
        hostField.setPromptText("Hostname ngrok");

        TextField portField = new TextField();
        portField.setPromptText("Porta ngrok");

        // Aggiunge i campi alla griglia
        grid.add(new Label("Host ngrok:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Porta ngrok:"), 0, 1);
        grid.add(portField, 1, 1);

        // Imposta la griglia come contenuto del dialog
        dialog.getDialogPane().setContent(grid);

        // Mostra il dialog e attende la risposta dell'utente
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // L'utente ha premuto OK, procede con la validazione dei parametri
            String host = hostField.getText().trim();
            String port = portField.getText().trim();

            // Verifica che i campi non siano vuoti
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

            // Costruisce l'URL di connessione JDBC
            dbUrl = "jdbc:postgresql://" + host + ":" + port + "/book_recommender";

            // Tenta una connessione di prova per verificare i parametri
            try {
                Connection testConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                testConnection.close();
                return true; // Connessione riuscita
            } catch (SQLException e) {
                showConnectionParametersError();
                return false; // Connessione fallita
            }
        }

        // L'utente ha annullato il dialog
        return false;
    }

    /**
     * Mostra una finestra di dialogo di errore quando i parametri di connessione
     * sono mancanti o non validi.
     */
    private void showConnectionParametersError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore di connessione");
        alert.setHeaderText("Parametri di connessione mancanti o errati");
        alert.setContentText("È necessario fornire i parametri di connessione corretti al database.");
        alert.showAndWait();
    }

    /**
     * Mostra un dialog per chiedere all'utente se desidera riprovare la connessione.
     *
     * @return true se l'utente vuole riprovare, false altrimenti
     */
    private boolean retryConnectionDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Errore di connessione");
        alert.setHeaderText("Connessione al database fallita");
        alert.setContentText("Vuoi riprovare a inserire i parametri di connessione?");

        ButtonType buttonTypeRetry = new ButtonType("Riprova");
        ButtonType buttonTypeExit = new ButtonType("Esci", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeRetry, buttonTypeExit);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == buttonTypeRetry;
    }
    /**
     * Mostra un avviso quando il server viene spento o diventa irraggiungibile.
     * Terminata l'applicazione dopo la conferma dell'utente.
     *
     * @param primaryStage Stage principale dell'applicazione
     */
    private void showServerShutdownAlert(Stage primaryStage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore di Connessione");
        alert.setHeaderText("Server Spento");
        alert.setContentText("Il server è stato spento. L'applicazione verrà chiusa. Riavviare il server prima di riaprire il client.");

        // Attende che l'avviso venga chiuso prima di terminare l'applicazione
        alert.showAndWait().ifPresent(response -> {
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Mostra un avviso generico di errore del server.
     * Termina l'applicazione dopo la conferma dell'utente.
     *
     * @param primaryStage Stage principale dell'applicazione
     * @param title Titolo dell'avviso
     * @param header Intestazione dell'avviso
     * @param content Contenuto del messaggio di errore
     */
    private void showServerErrorAlert(Stage primaryStage, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Attende che l'avviso venga chiuso prima di terminare l'applicazione
        alert.showAndWait().ifPresent(response -> {
            Platform.exit();
            System.exit(1); // Codice di uscita 1 indica errore
        });
    }

    /**
     * Registra la connessione o disconnessione del client nel database.
     * Aggiorna la tabella active_clients per tenere traccia dei client attivi.
     *
     * @param isConnecting true per registrare una connessione, false per una disconnessione
     */
    private void registerClientConnection(boolean isConnecting) {
        try {
            // Crea un ID client più breve per migliorare la leggibilità
            String clientIdShort = clientId.substring(0, 8);

            // Aggiorna la tabella active_clients nel database
            if (dbManager != null) {
                dbManager.updateClientConnection(clientIdShort, isConnecting);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Metodo chiamato quando l'applicazione viene terminata.
     * Esegue la pulizia delle risorse e registra la disconnessione del client.
     */
    @Override
    public void stop() {
        // Rimuove il client dal conteggio quando l'applicazione termina
        try {
            if (dbManager != null && !serverShutdownDetected) {
                // Registra la disconnessione del client
                registerClientConnection(false);
            }

            // Chiude la connessione socket se esistente
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Gestione silenziosa dell'errore durante la chiusura
        }
    }

    /**
     * Avvia un thread di monitoraggio per rilevare quando il server diventa irraggiungibile.
     * Verifica periodicamente la connessione al database e mostra una schermata di
     * disconnessione quando la connessione viene persa.
     */
    private void startServerMonitoring() {
        Thread monitorThread = new Thread(() -> {
            while (!serverShutdownDetected && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(3000); // Controlla ogni 3 secondi

                    // Verifica la connessione al database
                    if (dbManager != null) {
                        try {
                            Connection conn = dbManager.getConnection();
                            // Se la connessione fallisce, lancerà un'eccezione SQLException
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
                    // Il thread è stato interrotto, termina il ciclo di monitoraggio
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Qualsiasi altro errore è considerato una disconnessione dal server
                    Platform.runLater(() -> {
                        showServerDisconnectedScreen();
                    });
                    serverShutdownDetected = true;
                    break;
                }
            }
        });

        // Imposta il thread come daemon per consentirne la terminazione automatica
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Mostra la schermata di disconnessione quando il server diventa irraggiungibile.
     * Carica il layout FXML dedicato alla disconnessione o mostra un avviso di fallback.
     */
    private void showServerDisconnectedScreen() {
        try {
            // Imposta il flag per evitare chiamate multiple alla schermata di disconnessione
            serverShutdownDetected = true;

            // Carica il layout FXML per la schermata di disconnessione
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/server_disconnected.fxml"));
            Parent root = loader.load();

            // Crea una nuova scena con la schermata di disconnessione
            Scene scene = new Scene(root, 600, 400);

            // Applica la scena allo stage principale
            Platform.runLater(() -> {
                primaryStage.setScene(scene);
                primaryStage.setTitle("Server Disconnesso");
                primaryStage.setResizable(false);
                primaryStage.centerOnScreen();
            });

        } catch (IOException e) {
            // Fallback in caso di errore nel caricamento del file FXML
            Platform.runLater(() -> {
                showServerShutdownAlert(primaryStage);
            });
        }
    }

    /**
     * Metodo principale dell'applicazione client.
     * Punto di ingresso per l'avvio dell'applicazione JavaFX.
     *
     * @param args Argomenti della riga di comando (non utilizzati)
     */
    public static void main(String[] args) {
        launch(args);
    }
}