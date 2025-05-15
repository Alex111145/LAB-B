package book_recommender.lab_b;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import java.util.Arrays;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.io.PrintWriter;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerInterfaceController {

    // Google Drive file IDs
    private static final String VALUTAZIONI_FILE_ID = "1kBmaKQoCb-Z_DrTXGzEXt0htDYxG0ZRd";
    private static final String UTENTI_FILE_ID = "1Yn_pEZa7TpcT1ZIynhBCL31jtAHyRHBj";
    private static final String LIBRI_FILE_ID = "1C7Uz6fc6MRR0zp4tcDeU9D-THXW8n5mk";
    private static final String LIBRERIE_FILE_ID = "1S5G3wYhCq9UXDrhfGuhOZ7JzQ_YCyR4Q";
    private static final String DATA_FILE_ID = "17E35q-wg3YQn3EUsYyKeHDpzXOre8pYU";
    private static final String CONSIGLI_FILE_ID = "1tuUDCljamjaC4VKsu2VBwXSYFV7e8ilC";


    private ScheduledExecutorService clientMonitorScheduler;  // Add at the top of the class with other fields
    private NgrokManager ngrokManager;
    private boolean ngrokEnabled = false;
    private TextField dbUrlField; // Not in FXML anymore, but still needed
    private TextField dbUserField; // Not in FXML anymore, but still needed
    private TextField dbPasswordField; // Not in FXML anymore, but still needed
    private VBox logContainer; // Not in FXML anymore, but still needed for compatibility

    private final List<Socket> connectedClientSockets = new ArrayList<>();
    // Directory temporanea per i file scaricati
    private static final String TEMP_DIR = "temp_data/";




    @FXML
    private Label dbStatusLabel;
    @FXML
    private Label serverStatusLabel;
    @FXML
    private Label clientCountLabel;
    @FXML
    private Label startTimeLabel;
    @FXML
    private Label ngrokStatusLabel;
    @FXML
    private TextField ngrokHostField;

    @FXML
    private TextField ngrokPortField;

    @FXML
    private TextField ngrokUrlField;

    @FXML
    private Button startNgrokButton;

    @FXML
    private Button stopNgrokButton;
    @FXML
    private Label uptimeLabel;
    @FXML
    private ProgressBar initProgressBar;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;



    private ServerSocket serverSocket;
    private Thread serverThread;
    private LocalDateTime serverStartTime;
    private ScheduledExecutorService scheduler;
    private final AtomicInteger connectedClients = new AtomicInteger(0);
    private boolean serverRunning = false;
// Modifica in ServerInterfaceController.java

    @FXML
    public void onCopyNgrokHost(ActionEvent event) {
        // Copia solo l'host negli appunti
        String hostInfo = ngrokManager.getPublicUrl();
        if (hostInfo != null) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(hostInfo);
            clipboard.setContent(content);

            // Fornisci feedback visivo al bottone
            Button sourceButton = (Button) event.getSource();
            showButtonFeedback(sourceButton);
        }
    }
    @FXML
    public void onCopyNgrokPort(ActionEvent event) {
        // Copia solo la porta negli appunti
        int portInfo = ngrokManager.getPublicPort();
        if (portInfo > 0) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(portInfo));
            clipboard.setContent(content);

            // Fornisci feedback visivo al bottone
            Button sourceButton = (Button) event.getSource();
            showButtonFeedback(sourceButton);
        }
    }
    @FXML
    public void initialize() {
        // Create a temp directory if it doesn't exist
        new File(TEMP_DIR);

        // Initialize scheduler for updating uptime
        scheduler = Executors.newScheduledThreadPool(1);

        // Initialize NgrokManager
        ngrokManager = new NgrokManager();
        ngrokEnabled = false;

        // Create fields that were removed from FXML but still needed in code
        dbUrlField = new TextField("jdbc:postgresql://localhost:5432/book_recommender");
        dbUserField = new TextField("book_admin_8530");
        dbPasswordField = new TextField("CPuc#@r-zbKY");
        logContainer = new VBox();

        // Configure UI for ngrok if components exist
        if (ngrokStatusLabel != null) {
            ngrokStatusLabel.setText("Inattivo");
            ngrokStatusLabel.setTextFill(Color.RED);
        }

        if (ngrokHostField != null) {
            ngrokHostField.setEditable(false);
            ngrokHostField.setTooltip(new Tooltip("Host pubblico per la connessione tramite ngrok"));
        }

        if (ngrokPortField != null) {
            ngrokPortField.setEditable(false);
            ngrokPortField.setTooltip(new Tooltip("Porta pubblica per la connessione tramite ngrok"));
        }

        // Hide the start/stop ngrok buttons since it will be automatic
        if (startNgrokButton != null) {
            startNgrokButton.setVisible(false);
            startNgrokButton.setManaged(false);
        }

        if (stopNgrokButton != null) {
            stopNgrokButton.setVisible(false);
            stopNgrokButton.setManaged(false);
        }

        // Add the copy connection info feature
        if (ngrokHostField != null && ngrokPortField != null) {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem copyItem = new MenuItem("Copia informazioni di connessione");
            copyItem.setOnAction(e -> {
              Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
               clipboard.setContent(content);
          });
            contextMenu.getItems().add(copyItem);
            ngrokHostField.setContextMenu(contextMenu);
            ngrokPortField.setContextMenu(contextMenu);
        }

        // Remaining code stays the same...
    }




    @FXML
    public void onStartServer(ActionEvent event) {
        if (serverRunning) return;

        // Disable the button during initialization
        startButton.setDisable(true);


        // Run check in the background thread
        new Thread(() -> {
            // We now need to make sure dbUrlField is initialized with a default value
            String dbUrl = "jdbc:postgresql://localhost:5432/book_recommender";
            if (dbUrlField != null) {
                dbUrl = dbUrlField.getText();
            }

            // Same for username and password
            String dbUser = "book_admin_8530";
            if (dbUserField != null) {
                dbUser = dbUserField.getText();
            }

            String dbPassword = "CPuc#@r-zbKY";
            if (dbPasswordField != null) {
                dbPassword = dbPasswordField.getText();
            }

            try {
                // First, check if the PostgreSQL is installed and running
                updateProgress(0.1, "Checking PostgreSQL status...");
                if (!isPostgresInstalled()) {
                   if (!installPostgresIfNeeded()) {
                        Platform.runLater(() -> {
                            dbStatusLabel.setText("PostgreSQL not installed");
                            dbStatusLabel.setTextFill(Color.RED);
                          startButton.setDisable(false);
                        });
                        return;
                    }
                }

                if (!isPostgresRunning()) {
                 if (!startPostgresIfNeeded()) {
                        Platform.runLater(() -> {
                            dbStatusLabel.setText("PostgreSQL is not running");
                            dbStatusLabel.setTextFill(Color.RED);
                          startButton.setDisable(false);
                        });
                        return;
                    }
                }

                // Check if another server is already running
                updateProgress(0.2, "Checking for existing server...");
                if (checkExistingServer(dbUrl)) {
                    // Connect to an existing server
                    updateProgress(0.3, "Connecting to existing server...");
                    connectToExistingServer(dbUrl, dbUser, dbPassword);
                } else {
                    // Start a new server
                    Platform.runLater(() -> {
                        serverStatusLabel.setText("Starting...");
                        serverStatusLabel.setTextFill(Color.BLUE);
                    });

                    // Imposta lo stato del server come attivo prima di avviare il thread
                    serverRunning = true;
                    updateUIState(true);

                    // Record start time
                    serverStartTime = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    Platform.runLater(() -> {
                        startTimeLabel.setText(serverStartTime.format(formatter));
                    });

                    // Start uptime counter
                    startUptimeCounter();

                    // Start server in background thread
                    serverThread = new Thread(this::startServerProcess);
                    serverThread.setDaemon(true);
                    serverThread.start();
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                   startButton.setDisable(false);

                    // Reset server state in case of error
                    serverRunning = false;
                    updateUIState(false);
                });
            }
        }).start();
    }
    /**
     * Checks if another server is already running
     * @param dbUrl The database URL to check
     * @return true if another server is running, false otherwise
     */
    private boolean checkExistingServer(String dbUrl) {
        try {
            boolean serverRunning = Server.isAnotherServerRunning(dbUrl);

            return serverRunning;
        } catch (Exception e) {
           return false;
        }
    }


    @FXML
    public void onStartNgrok(ActionEvent event) {
        if (ngrokEnabled) return;

        // Disabilita il pulsante durante l'inizializzazione
        startNgrokButton.setDisable(true);

        // Ottieni la porta PostgreSQL
        int postgresPort = 5432; // Porta di default
        try {
            postgresPort = Integer.parseInt(DatabaseManager.getDefaultPort());
        } catch (NumberFormatException e) {
            // Usa la porta di default
        }

        // Avvia ngrok in un thread separato
        int finalPostgresPort = postgresPort;
        new Thread(() -> {
            boolean success = ngrokManager.startNgrokTunnel(finalPostgresPort);

            Platform.runLater(() -> {
                if (success) {
                    ngrokEnabled = true;
                    updateNgrokUIState(true);

                    // Aggiorna i campi UI con l'host e la porta pubblici separatamente
                    String publicUrl = ngrokManager.getPublicUrl();
                    int publicPort = ngrokManager.getPublicPort();

                    ngrokHostField.setText(publicUrl);
                    ngrokPortField.setText(String.valueOf(publicPort));

                    ngrokStatusLabel.setText("Attivo");
                    ngrokStatusLabel.setTextFill(Color.GREEN);

                } else {
                    ngrokStatusLabel.setText("Errore");
                    ngrokStatusLabel.setTextFill(Color.RED);
                    startNgrokButton.setDisable(false);

                }
            });
        }).start();
    }

    @FXML
    public void onStopNgrok(ActionEvent event) {
        if (!ngrokEnabled) return;

        ngrokManager.stopTunnel();
        ngrokEnabled = false;
        updateNgrokUIState(false);

    }
    private void updateNgrokUIState(boolean running) {
        Platform.runLater(() -> {
            startNgrokButton.setDisable(running);
            stopNgrokButton.setDisable(!running);

            if (!running) {
                ngrokStatusLabel.setText("Inattivo");
                ngrokStatusLabel.setTextFill(Color.RED);
                ngrokHostField.setText("");
                ngrokPortField.setText("");
            }
        });

    }
    /**
     * Connects to an existing server and updates the UI
     * @param dbUrl The database URL
     * @param dbUser The database username
     * @param dbPassword The database password
     */
    private void connectToExistingServer(String dbUrl, String dbUser, String dbPassword) {
        try {
            // Connect to database
            updateProgress(0.4, "Connecting to database...");
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);


            // Update UI to reflect we're connected to the existing server
            serverRunning = true;

            // Get the current server start time from a database if available
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT server_start_time FROM server_info LIMIT 1");
                if (rs.next()) {
                    Timestamp startTimestamp = rs.getTimestamp(1);
                    if (startTimestamp != null) {
                        serverStartTime = startTimestamp.toLocalDateTime();
                    } else {
                        serverStartTime = LocalDateTime.now(); // Fallback
                    }
                } else {
                    serverStartTime = LocalDateTime.now(); // Fallback
                }
            } catch (SQLException e) {
                // If a table doesn't exist, just use the current time
                serverStartTime = LocalDateTime.now();
            }

            // Update UI with server status
            updateProgress(1.0, "Connected to existing server");
            Platform.runLater(() -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                startTimeLabel.setText(serverStartTime.format(formatter));
                serverStatusLabel.setText("Connected");
                serverStatusLabel.setTextFill(Color.GREEN);
                dbStatusLabel.setText("Connected to existing database");
                dbStatusLabel.setTextFill(Color.GREEN);
                startButton.setDisable(true);
                stopButton.setDisable(false);

                // Disable input fields
                dbUrlField.setDisable(true);
                dbUserField.setDisable(true);
                dbPasswordField.setDisable(true);
            });

            // Start uptime counter
            startUptimeCounter();

            // Start client count monitoring
            startClientCountMonitoring();

        } catch (SQLException e) {
            Platform.runLater(() -> {
              startButton.setDisable(false);
            });
        }
    }

    /**
     * Starts a thread to periodically update the connected client count
     */
    private void startClientCountMonitoring() {
        // Prima interrompi qualsiasi scheduler di monitoraggio esistente
        stopClientCountMonitoring();

        // Crea un nuovo scheduler
        clientMonitorScheduler = Executors.newScheduledThreadPool(1);

        clientMonitorScheduler.scheduleAtFixedRate(() -> {
            // Controlla se il server è ancora in esecuzione prima di eseguire l'aggiornamento
            if (!serverRunning) {
                stopClientCountMonitoring();
                return;
            }

            try {
                // Connect to database
                DatabaseManager dbManager = DatabaseManager.getInstance();
                int count = dbManager.getConnectedClientCount();

                // Update UI
                Platform.runLater(() -> {
                    clientCountLabel.setText(String.valueOf(count));
               });
            } catch (SQLException e) {

            }
        }, 0, 2, TimeUnit.SECONDS);

    }
    /**
     * Stops the client count monitoring thread
     */
    private void stopClientCountMonitoring() {
        if (clientMonitorScheduler != null && !clientMonitorScheduler.isShutdown()) {
            try {
                clientMonitorScheduler.shutdown();
                if (!clientMonitorScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    clientMonitorScheduler.shutdownNow();
                }
           } catch (InterruptedException e) {
                clientMonitorScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    @FXML
    private void onStopServer(ActionEvent event) {
        if (!serverRunning) return;

     
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Spegni Server");
            alert.setHeaderText("Spegnimento del Server in corso...");
            alert.setContentText("Sei sicuro di voler arrestare il Server? \nTutti gli utenti collegati verranno immediatamente scollegati.");

            // Customize the buttons
            ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("OK");
            ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Annulla");

            // Handle the result
            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    // User confirmed, proceed with shutdown
                    cleanupDatabaseAndShutdown();
                }
                // If cancel, do nothing
            });
        });
    }
    @FXML
    private void onClearLog() {
        logContainer.getChildren().clear();
    }

    private void startServerProcess() {
        // Make sure we have a default value if dbUrlField is null
        final String dbUrl;
        if (dbUrlField != null) {
            dbUrl = dbUrlField.getText();
        } else {
            dbUrl = "jdbc:postgresql://localhost:5432/book_recommender";
        }

        try {
            // Step 1: Initialize progress
            updateProgress(0.0, "Initializing server...");

            // Start a socket server early
            updateProgress(0.1, "Starting socket server...");
            startSocketServer();

            // Step 1.5: Verify and start PostgreSQL if necessary
            updateProgress(0.15, "Checking PostgreSQL status...");
            if (!isPostgresInstalled()) {
                boolean installed = installPostgresIfNeeded();
                if (!installed) {
                }
            }

            if (!isPostgresRunning()) {
                boolean started = startPostgresIfNeeded();
                if (!started) {
                }
            }

            // Step 2: Initialize the database connection using DatabaseManager
            updateProgress(0.2, "Initializing database connection...");
            try {
                final DatabaseManager dbManager = DatabaseManager.getInstance();
                // Get credentials from DatabaseManager for UI display
                final String finalDbUser = dbManager.getDbUser();
                final String finalDbPassword = dbManager.getDbPassword();

                // Update UI fields if they exist - using final variables
                Platform.runLater(() -> {
                    if (dbUserField != null) dbUserField.setText(finalDbUser);
                    if (dbPasswordField != null) dbPasswordField.setText(finalDbPassword);
                });

                // Step 3: Download files
                updateProgress(0.3, "Downloading data files...");
                downloadAllFiles();

                // Step 4: Initialize database tables
                updateProgress(0.5, "Creating database tables...");
                initializeDatabase(dbUrl, finalDbUser, finalDbPassword);

                // Step 5: Import data
                updateProgress(0.7, "Importing data...");
                populateDatabase(dbUrl, finalDbUser, finalDbPassword);

                // Step 6: Create active_clients table for client tracking (already done in initializeDatabase)
                updateProgress(0.8, "Setting up client tracking...");

                // Step 7: Start client count monitoring
                updateProgress(0.85, "Starting client monitoring...");
                startClientCountMonitoring();

                // Step 8: Start Ngrok automatically
                updateProgress(0.9, "Starting ngrok tunnel...");
                startNgrokAutomatically();

                // Complete
                updateProgress(1.0, "Server started successfully!");
                Platform.runLater(() -> {
                    serverStatusLabel.setText("Running");
                    serverStatusLabel.setTextFill(Color.GREEN);
                });
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Database initialization failed", e);
            }

        } catch (Exception e) {
            String errorMsg = "Server initialization failed: " + e.getMessage();
            e.printStackTrace();

            // Update UI on error
            Platform.runLater(() -> {
                serverStatusLabel.setText("Error");
                serverStatusLabel.setTextFill(Color.RED);
                updateUIState(false);
                serverRunning = false;
            });
        }
    }

    // Metodo di utilità per dare feedback visivo al bottone dopo il clic
    private void showButtonFeedback(Button button) {
        // Salva il colore originale
        String originalStyle = button.getStyle();

        // Imposta il colore verde
        button.setStyle("-fx-background-color: #00C853; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 5px;");

        // Dopo un breve ritardo, ripristina il colore originale
        new Thread(() -> {
            try {
                Thread.sleep(500); // Attesa di 500ms
                Platform.runLater(() -> {
                    button.setStyle(originalStyle);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void startNgrokAutomatically() {
        // Get the PostgreSQL port
        int postgresPort = 5432; // Default port
        try {
            postgresPort = Integer.parseInt(DatabaseManager.getDefaultPort());
        } catch (NumberFormatException e) {
            // Use the default port
        }

        // Start ngrok in a separate thread
        int finalPostgresPort = postgresPort;
        new Thread(() -> {
            boolean success = ngrokManager.startNgrokTunnel(finalPostgresPort);

            Platform.runLater(() -> {
                if (success) {
                    ngrokEnabled = true;

                    // Update UI fields with the host and port separately
                    String publicUrl = ngrokManager.getPublicUrl();
                    int publicPort = ngrokManager.getPublicPort();

                    // Set the separate fields
                    ngrokHostField.setText(publicUrl);
                    ngrokPortField.setText(String.valueOf(publicPort));

                    ngrokStatusLabel.setText("Attivo");
                    ngrokStatusLabel.setTextFill(Color.GREEN);

                } else {
                    ngrokStatusLabel.setText("Errore");
                    ngrokStatusLabel.setTextFill(Color.RED);
               }
            });
        }).start();
    }
    /**
     * Notifies all clients of a shutdown, cleans up a database and shuts down the server
     */
    public void cleanupDatabaseAndShutdown() {
        if (!serverRunning) return;
        stopClientCountMonitoring();
        // Arresta il tunnel ngrok se attivo
        if (ngrokManager != null) {
            try {
              ngrokManager.stopTunnel();

                // Aggiorna UI per ngrok
                Platform.runLater(() -> {
                    if (ngrokStatusLabel != null) {
                        ngrokStatusLabel.setText("Inattivo");
                        ngrokStatusLabel.setTextFill(Color.RED);
                    }
                    if (ngrokHostField != null) {
                        ngrokHostField.setText("");
                    }
                    if (ngrokPortField != null) {
                        ngrokPortField.setText("");
                    }
                    if (startNgrokButton != null) {
                        startNgrokButton.setDisable(false); // Modifica: abilita il pulsante di avvio di ngrok
                    }
                    if (stopNgrokButton != null) {
                        stopNgrokButton.setDisable(true);
                    }
                });
 } catch (Exception e) {
            }
        }

        // Clean up del database
        cleanDatabase();

        // Elimina tutti i file scaricati
        deleteTemporaryFiles();

        // Ora procedi con il normale arresto
        serverRunning = false;

        // Chiudi il socket del server se esiste
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }

        // Arresta lo scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            try {
                scheduler.shutdown();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
         } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
          }
        }

        // Reimpostazione delle variabili di stato del server - AGGIUNGI QUESTA SEZIONE
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }

        // Reset UI state
        Platform.runLater(() -> {
            updateUIState(false);

            // Reset client count
            connectedClients.set(0);
            if (clientCountLabel != null) {
                clientCountLabel.setText("0");
            }

            if (serverStatusLabel != null) {
                serverStatusLabel.setText("Stopped");
                serverStatusLabel.setTextFill(Color.RED);
            }

            if (startTimeLabel != null) {
                startTimeLabel.setText("-");
            }

            if (uptimeLabel != null) {
                uptimeLabel.setText("-");
            }

            // Riabilita i pulsanti di avvio
            if (startButton != null) {
                startButton.setDisable(false);
            }

            if (startNgrokButton != null) {
                boolean postgresRunning = isPostgresRunning();
                startNgrokButton.setDisable(!postgresRunning);
            }
        });

    }
    /**
     * Delete all files in the temporary directory
     * Metodo migliorato per la cancellazione efficace dei file temporanei
     */
    private void deleteTemporaryFiles() {

            File tempDir = new File(TEMP_DIR);
            if (tempDir.exists() && tempDir.isDirectory()) {
                // First, delete all files in the directory
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            boolean deleted = file.delete();
                            if (!deleted) {
                             file.deleteOnExit();
                            }
                        } else if (file.isDirectory()) {
                            // Handle subdirectories recursively
                            deleteDirectoryRecursively(file);
                        }
                    }
                }

                // Now try to delete the directory itself
                boolean dirDeleted = tempDir.delete();
                if (!dirDeleted) {

                    // Try force garbage collection to release locks
                    System.gc();
                    try {
                        Thread.sleep(200); // Give a little time for GC
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }

                    dirDeleted = tempDir.delete();

                    if (!dirDeleted) {
                        // If still can't delete, schedule for deletion on JVM exit
                        tempDir.deleteOnExit();
                  }
                }
            }
        }

    /**
     * Helper method to delete a directory and all its contents recursively
     * Nuovo metodo helper per la cancellazione ricorsiva di directory
     */
    private boolean deleteDirectoryRecursively(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectoryRecursively(file);
                    } else {
                        boolean deleted = file.delete();
                        if (!deleted) {
                         file.deleteOnExit();
                        }
                    }
                }
            }
        }
        boolean deleted = directory.delete();
        if (!deleted) {
           directory.deleteOnExit();
        }
        return deleted;
    }
    /**
     * Cleans up the database by dropping all tables
     */
    private void cleanDatabase() {

        String dbUrl = dbUrlField.getText();
        String dbUser = dbUserField.getText();
        String dbPassword = dbPasswordField.getText();

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {

            // Drop all tables in the correct order to handle dependencies
            String[] dropStatements = {
                    "DROP TABLE IF EXISTS active_clients CASCADE",
                    "DROP TABLE IF EXISTS book_recommendations CASCADE",
                    "DROP TABLE IF EXISTS book_ratings CASCADE",
                    "DROP TABLE IF EXISTS library_books CASCADE",
                    "DROP TABLE IF EXISTS libraries CASCADE",
                    "DROP TABLE IF EXISTS books CASCADE",
                    "DROP TABLE IF EXISTS users CASCADE"
            };

            for (String sql : dropStatements) {
                stmt.execute(sql);
            }

        } catch (SQLException e) {
        }
    }

    private void downloadAllFiles() throws IOException {

        // Make sure the temp directory exists
        File tempDir = new File(TEMP_DIR);
        if (!tempDir.exists()) {
            boolean created = tempDir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create directory: " + TEMP_DIR);
            }
        }

        try {
            // Download all required files
            downloadFromGoogleDrive(VALUTAZIONI_FILE_ID, "ValutazioniLibri.csv");
            downloadFromGoogleDrive(UTENTI_FILE_ID, "UtentiRegistrati.csv");
            downloadFromGoogleDrive(LIBRI_FILE_ID, "Libri.csv");
            downloadFromGoogleDrive(LIBRERIE_FILE_ID, "Librerie.dati.csv");
            downloadFromGoogleDrive(DATA_FILE_ID, "Data.csv");
            downloadFromGoogleDrive(CONSIGLI_FILE_ID, "ConsigliLibri.csv");

      } catch (IOException e) {
           throw e;
        }
    }

    private void downloadFromGoogleDrive(String fileId, String fileName) throws IOException {
       
        String urlString = "https://drive.google.com/uc?id=" + fileId + "&export=download";
        File outputFile = new File(TEMP_DIR + fileName);

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(60000);    // 60 seconds

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            // Get file size for logging
            int fileSize = connection.getContentLength();
         
            // Create parent directories if needed
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            // Download file
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Log progress for large files
                    if (fileSize > 1000000 && totalBytesRead % 500000 == 0) { // 500KB increments for files > 1MB
                  }
                }

           }

            // Check if file was actually downloaded
            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("File downloaded but is empty or not found: " + outputFile.getAbsolutePath());
            }

        } catch (Exception e) {
            throw new IOException("Failed to download file: " + fileName, e);
        }
    }

    private void initializeDatabase(String dbUrl, String dbUser, String dbPassword) throws SQLException {
      
        try {
            // Use the DatabaseManager instance instead of direct connection
            DatabaseManager dbManager = DatabaseManager.getInstance();
            Connection conn = dbManager.getConnection();

            try (Statement stmt = conn.createStatement()) {
                // Drop existing tables if any (in reverse order to handle foreign keys)
                String[] dropStatements = {
                        "DROP TABLE IF EXISTS book_recommendations CASCADE",
                        "DROP TABLE IF EXISTS book_ratings CASCADE",
                        "DROP TABLE IF EXISTS library_books CASCADE",
                        "DROP TABLE IF EXISTS libraries CASCADE",
                        "DROP TABLE IF EXISTS books CASCADE",
                        "DROP TABLE IF EXISTS users CASCADE",
                        "DROP TABLE IF EXISTS active_clients CASCADE"
                };

                for (String sql : dropStatements) {
                    stmt.execute(sql);
              }

                // Create tables in proper order
                String[] createTableStatements = {
                        // Users table
                        "CREATE TABLE IF NOT EXISTS users (" +
                                "user_id VARCHAR(8) PRIMARY KEY UNIQUE," +
                                "full_name VARCHAR(100) NOT NULL," +
                                "fiscal_code VARCHAR(16) NOT NULL," +
                                "email VARCHAR(100) NOT NULL," +
                                "password VARCHAR(100) NOT NULL" +
                                ")",

                        // Books table
                        "CREATE TABLE IF NOT EXISTS books (" +
                                "id SERIAL PRIMARY KEY," +
                                "title TEXT NOT NULL," +
                                "authors TEXT NOT NULL," +
                                "category TEXT," +
                                "publisher TEXT," +
                                "publish_year INTEGER," +
                                "UNIQUE(title, authors)" +
                                ")",

                        // Libraries table
                        "CREATE TABLE IF NOT EXISTS libraries (" +
                                "id SERIAL PRIMARY KEY," +
                                "user_id VARCHAR(8) REFERENCES users(user_id) ON DELETE CASCADE," +
                                "library_name VARCHAR(100) NOT NULL," +
                                "UNIQUE(user_id, library_name)" +
                                ")",

                        // Library_Books table (many-to-many relationship)
                        "CREATE TABLE IF NOT EXISTS library_books (" +
                                "library_id INTEGER REFERENCES libraries(id) ON DELETE CASCADE," +
                                "book_id INTEGER REFERENCES books(id) ON DELETE CASCADE," +
                                "PRIMARY KEY (library_id, book_id)" +
                                ")",

                        // Book_Ratings table
                        "CREATE TABLE IF NOT EXISTS book_ratings (" +
                                "id SERIAL PRIMARY KEY," +
                                "user_id VARCHAR(8) REFERENCES users(user_id) ON DELETE CASCADE," +
                                "book_id INTEGER REFERENCES books(id) ON DELETE CASCADE," +
                                "style_rating INTEGER CHECK (style_rating >= 1 AND style_rating <= 5)," +
                                "content_rating INTEGER CHECK (content_rating >= 1 AND content_rating <= 5)," +
                                "pleasantness_rating INTEGER CHECK (pleasantness_rating >= 1 AND pleasantness_rating <= 5)," +
                                "originality_rating INTEGER CHECK (originality_rating >= 1 AND originality_rating <= 5)," +
                                "edition_rating INTEGER CHECK (edition_rating >= 1 AND edition_rating <= 5)," +
                                "average_rating FLOAT," +
                                "general_comment TEXT," +         // Commento generale
                                "style_comment TEXT," +           // Commento sullo stile
                                "content_comment TEXT," +         // Commento sul contenuto
                                "pleasantness_comment TEXT," +    // Commento sulla gradevolezza
                                "originality_comment TEXT," +     // Commento sull'originalità
                                "edition_comment TEXT," +         // Commento sull'edizione
                                "UNIQUE(user_id, book_id)" +
                                ")",

                        // Book_Recommendations table
                        "CREATE TABLE IF NOT EXISTS book_recommendations (" +
                                "id SERIAL PRIMARY KEY," +
                                "user_id VARCHAR(8) REFERENCES users(user_id) ON DELETE CASCADE," +
                                "source_book_id INTEGER REFERENCES books(id) ON DELETE CASCADE," +
                                "recommended_book_id INTEGER REFERENCES books(id) ON DELETE CASCADE," +
                                "UNIQUE(user_id, source_book_id, recommended_book_id)" +
                                ")",

                        // Active_Clients table
                        "CREATE TABLE IF NOT EXISTS active_clients (" +
                                "client_id VARCHAR(50) PRIMARY KEY," +
                                "connect_time TIMESTAMP NOT NULL" +
                                ")"
                };

                for (String sql : createTableStatements) {
                    stmt.execute(sql);
                }

                // Create indexes for better performance
                String[] indexStatements = {
                        "CREATE INDEX IF NOT EXISTS idx_books_title ON books(title)",
                        "CREATE INDEX IF NOT EXISTS idx_books_authors ON books(authors)",
                        "CREATE INDEX IF NOT EXISTS idx_book_ratings_user_id ON book_ratings(user_id)",
                        "CREATE INDEX IF NOT EXISTS idx_book_ratings_book_id ON book_ratings(book_id)",
                        "CREATE INDEX IF NOT EXISTS idx_library_books_book_id ON library_books(book_id)",
                        "CREATE INDEX IF NOT EXISTS idx_library_books_library_id ON library_books(library_id)"
                };

                for (String sql : indexStatements) {
                    stmt.execute(sql);
              }
            }

        } catch (SQLException e) {
            throw e;
        }
    }

    private void populateDatabase(String dbUrl, String dbUser, String dbPassword) throws SQLException, IOException {
    
        try {
            // Use the DatabaseManager instance instead of direct connection
            DatabaseManager dbManager = DatabaseManager.getInstance();
            Connection conn = dbManager.getConnection();

            // Disable auto-commit for better performance
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                // Import books from Data.csv first (this contains all book metadata)
             importBooks(conn, TEMP_DIR + "Data.csv");
                conn.commit();
            
                // Import users from UtentiRegistrati.csv
                importUsers(conn);
                conn.commit();
             
                // Import libraries from Librerie.dati.csv
             importLibraries(conn);
                conn.commit();
             
                // Import ratings from ValutazioniLibri.csv
               importRatings(conn);
                conn.commit();
              
                // Import recommendations from ConsigliLibri.dati.csv or ConsigliLibri.csv
              importRecommendations(conn);
                conn.commit();
             
                // Verify database content
                verifyDatabaseContent(conn);

            } catch (Exception e) {
                // Rollback on error
                e.printStackTrace();
                conn.rollback();
                throw e;
            } finally {
                // Restore original auto-commit setting
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw e;
        }
 }
    private void verifyDatabaseContent(Connection conn) throws SQLException {
       
        try (Statement stmt = conn.createStatement()) {
            // Check users table
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next()) {
             }
            }

            // Check books table
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books")) {
                if (rs.next()) {
                }
            }

            // Check libraries table
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM libraries")) {
                if (rs.next()) {
               }
            }

            // Check library_books table
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM library_books")) {
                if (rs.next()) {
               }
            }

            // Check book_ratings table
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM book_ratings")) {
                if (rs.next()) {
               }
            }

            // Check book_recommendations table
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM book_recommendations")) {
                if (rs.next()) {
              }
            }

            // Sample data check
       
            // Sample users
            try (ResultSet rs = stmt.executeQuery("SELECT user_id, full_name, email FROM users LIMIT 3")) {
              while (rs.next()) {
               }
            }

            // Sample books
            try (ResultSet rs = stmt.executeQuery("SELECT id, title, authors, category FROM books LIMIT 3")) {
               while (rs.next()) {
               }
            }

            // Sample ratings
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT br.user_id, b.title, br.average_rating " +
                            "FROM book_ratings br " +
                            "JOIN books b ON br.book_id = b.id " +
                            "LIMIT 3")) {
               while (rs.next()) {
               }
            }
        }

    }

    private void importRecommendations(Connection conn) throws SQLException, IOException {
      
        // Try both possible filenames
        File consigliFile = new File(TEMP_DIR + "ConsigliLibri.csv");
        if (!consigliFile.exists()) {
            consigliFile = new File(TEMP_DIR + "ConsigliLibri.dati.csv");
            if (!consigliFile.exists()) {
               return;
            }
        }

      
        String findBookByTitleSql = "SELECT id FROM books WHERE title = ?";
        String findBookByTitleLikeSql = "SELECT id FROM books WHERE title ILIKE ?";

        String insertRecommendationSql =
                "INSERT INTO book_recommendations (user_id, source_book_id, recommended_book_id) " +
                        "VALUES (?, ?, ?) ON CONFLICT (user_id, source_book_id, recommended_book_id) DO NOTHING";

        int recommendationCount = 0;
        int errorCount = 0;

        try (PreparedStatement pstmtFindBook = conn.prepareStatement(findBookByTitleSql);
             PreparedStatement pstmtFindBookLike = conn.prepareStatement(findBookByTitleLikeSql);
             PreparedStatement pstmtInsert = conn.prepareStatement(insertRecommendationSql);
             BufferedReader reader = new BufferedReader(new FileReader(consigliFile))) {

            String line = reader.readLine(); // Skip header
        int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    String[] fields = parseCsvLine(line);

                    if (fields.length < 3) {
                      continue;
                    }

                    String userId = fields[0].trim();
                    String sourceBookTitle = fields[1].trim();

                    // Skip if required fields are empty
                    if (userId.isEmpty() || sourceBookTitle.isEmpty()) {
                     continue;
                    }

                    // Check if user exists
                    String checkUserSql = "SELECT 1 FROM users WHERE user_id = ?";
                    try (PreparedStatement pstmtCheckUser = conn.prepareStatement(checkUserSql)) {
                        pstmtCheckUser.setString(1, userId);
                        ResultSet userRs = pstmtCheckUser.executeQuery();

                        if (!userRs.next()) {
                         continue;
                        }
                    }

                    // Find source book ID - try exact match first
                    Integer sourceBookId = null;
                    pstmtFindBook.setString(1, sourceBookTitle);
                    ResultSet sourceBookRs = pstmtFindBook.executeQuery();

                    if (sourceBookRs.next()) {
                        sourceBookId = sourceBookRs.getInt(1);
                    } else {
                        // Try LIKE search
                        pstmtFindBookLike.setString(1, "%" + sourceBookTitle + "%");
                        sourceBookRs = pstmtFindBookLike.executeQuery();

                        if (sourceBookRs.next()) {
                            sourceBookId = sourceBookRs.getInt(1);
                        }
                    }

                    if (sourceBookId == null) {
             
                        // Add the book to the database
                        String insertBookSql = "INSERT INTO books (title, authors, category, publisher) " +
                                "VALUES (?, 'Unknown', 'Unknown', 'Unknown') RETURNING id";
                        try (PreparedStatement pstmtInsertBook = conn.prepareStatement(insertBookSql)) {
                            pstmtInsertBook.setString(1, sourceBookTitle);
                            ResultSet newBookRs = pstmtInsertBook.executeQuery();

                            if (newBookRs.next()) {
                                sourceBookId = newBookRs.getInt(1);
                          } else {
                                continue; // Skip if failed to add book
                            }
                        }
                    }

                    // Process recommended books
                    for (int i = 2; i < fields.length; i++) {
                        String recBookTitle = fields[i].trim();
                        if (recBookTitle.isEmpty() || recBookTitle.equalsIgnoreCase("null")) {
                            continue;
                        }

                        // Find recommended book ID
                        Integer recBookId = null;
                        pstmtFindBook.setString(1, recBookTitle);
                        ResultSet recBookRs = pstmtFindBook.executeQuery();

                        if (recBookRs.next()) {
                            recBookId = recBookRs.getInt(1);
                        } else {
                            // Try LIKE search
                            pstmtFindBookLike.setString(1, "%" + recBookTitle + "%");
                            recBookRs = pstmtFindBookLike.executeQuery();

                            if (recBookRs.next()) {
                                recBookId = recBookRs.getInt(1);
                            }
                        }

                        if (recBookId == null) {
                            // Add the book to the database
                            String insertBookSql = "INSERT INTO books (title, authors, category, publisher) " +
                                    "VALUES (?, 'Unknown', 'Unknown', 'Unknown') RETURNING id";
                            try (PreparedStatement pstmtInsertBook = conn.prepareStatement(insertBookSql)) {
                                pstmtInsertBook.setString(1, recBookTitle);
                                ResultSet newBookRs = pstmtInsertBook.executeQuery();

                                if (newBookRs.next()) {
                                    recBookId = newBookRs.getInt(1);
                                } else {
                                    continue; // Skip if failed to add book
                                }
                            }
                        }

                        // Insert recommendation
                        pstmtInsert.setString(1, userId);
                        pstmtInsert.setInt(2, sourceBookId);
                        pstmtInsert.setInt(3, recBookId);

                        try {
                            int rowsAffected = pstmtInsert.executeUpdate();
                            if (rowsAffected > 0) {
                                recommendationCount++;
                            }
                        } catch (SQLException e) {
                            if (!e.getMessage().contains("duplicate key")) {
                                throw e; // Re-throw if not a duplicate key error
                            }
                        }
                    }

                } catch (Exception e) {
                    errorCount++;
             }
            }
        }

    }

    private void importBooks(Connection conn, String filePath) throws SQLException, IOException {
       
        String sql = "INSERT INTO books (title, authors, category, publisher, publish_year) " +
                "VALUES (?, ?, ?, ?, ?) ON CONFLICT (title, authors) DO NOTHING";

        int successCount = 0;
        int errorCount = 0;

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line = reader.readLine(); // Skip header
            
            // Process each line
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    String[] fields = parseCsvLine(line);

                    if (fields.length < 5) {
                      continue;
                    }

                    String title = fields[0].trim();
                    String authors = fields[1].trim();
                    String category = fields[2].trim();
                    String publisher = fields[3].trim();

                    // Skip if title or author is empty
                    if (title.isEmpty() || authors.isEmpty()) {
                     continue;
                    }

                    pstmt.setString(1, title);
                    pstmt.setString(2, authors);
                    pstmt.setString(3, category);
                    pstmt.setString(4, publisher);

                    // Handle publish year
                    try {
                        float yearFloat = Float.parseFloat(fields[4].trim());
                        pstmt.setInt(5, (int) yearFloat);
                    } catch (NumberFormatException e) {
                        pstmt.setNull(5, Types.INTEGER);
                   }

                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        successCount++;
                        if (successCount % 100 == 0) {
                      }
                    }
                } catch (Exception e) {
                    errorCount++;
                }
            }
        }
}
    /**
     * Helper method to parse integers with default value if parsing fails
     */
    private int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            // Ensure the rating is between 1 and 5
            if (parsed < 1) parsed = 1;
            if (parsed > 5) parsed = 5;
            return parsed;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    /**
     * Verifica se PostgreSQL è installato sul sistema
     * @return true se PostgreSQL è installato, false altrimenti
     */
    private boolean isPostgresInstalled() {
        try {
            // Comando per verificare se PostgreSQL è installato
            String checkCommand;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Windows
                checkCommand = "where psql";
            } else {
                // macOS o Linux
                checkCommand = "which psql";
            }

            Process process = Runtime.getRuntime().exec(checkCommand);
            int exitCode = process.waitFor();

            return exitCode == 0;
        } catch (Exception e) {
        return false;
        }
    }

    /**
     * Installa PostgreSQL se non è già installato
     * @return true se l'installazione ha avuto successo o PostgreSQL è già installato, false altrimenti
     */
    private boolean installPostgresIfNeeded() {
        if (isPostgresInstalled()) {
           return true;
        }


        try {
            // Il comando di installazione dipende dal sistema operativo

            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("win")) {
               Runtime.getRuntime().exec("cmd /c start https://www.postgresql.org/download/windows/");

                // Mostra istruzioni all'utente
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Installazione PostgreSQL");
                    alert.setHeaderText("PostgreSQL non è installato");
                    alert.setContentText("Si prega di installare PostgreSQL dal browser che si aprirà.\n" +
                            "Dopo l'installazione, riavvia l'applicazione.");
                    alert.showAndWait();
                });

                return false;
            } else if (osName.contains("mac")) {

                // Verifica se Homebrew è installato
                Process brewCheck = Runtime.getRuntime().exec("which brew");
                if (brewCheck.waitFor() == 0) {
                    Process process = Runtime.getRuntime().exec("brew install postgresql");
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                      return true;
                    }
                } else {
                    // Chiedi all'utente di installare Homebrew o PostgreSQL manualmente
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Installazione PostgreSQL");
                        alert.setHeaderText("È necessario installare PostgreSQL");
                        alert.setContentText("Utilizza il comando 'brew install postgresql' nel terminale\n" +
                                "o scarica PostgreSQL dal sito ufficiale.");
                        alert.showAndWait();
                    });

                    Runtime.getRuntime().exec("open https://www.postgresql.org/download/macosx/");
                    return false;
                }
            } else {

                // Chiedi conferma all'utente
                AtomicBoolean proceed = new AtomicBoolean(false);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Installazione PostgreSQL");
                    alert.setHeaderText("È necessario installare PostgreSQL");
                    alert.setContentText("L'applicazione tenterà di installare PostgreSQL.\n" +
                            "Questo richiede i privilegi di amministratore.");
                    alert.showAndWait().ifPresent(result -> {
                        if (result == ButtonType.OK) {
                            proceed.set(true);
                        }
                    });
                });

                // Attendi che l'utente risponda
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (proceed.get()) {
                    Process process = Runtime.getRuntime().exec("pk exec apt-get -y install postgresql postgresql-contrib");
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                      return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean startPostgresIfNeeded() {
        if (isPostgresRunning()) {
            return true;
        }


        try {
            String osName = System.getProperty("os.name").toLowerCase();
            boolean success = false;

            if (osName.contains("mac")) {
                // Comandi specifici per macOS
                String[] macCommands;
                boolean isAppleSilicon = false;

                // Verifica se è un Mac con Apple Silicon (M1/M2/M3)
                try {
                    Process archProcess = Runtime.getRuntime().exec("uname -m");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(archProcess.getInputStream()));
                    String arch = reader.readLine();
                    if (arch != null && arch.equals("arm64")) {
                        isAppleSilicon = true;
                   } else {
                   }
                } catch (Exception e) {
                }

                if (isAppleSilicon) {
                    // Comandi specifici per Apple Silicon (M1/M2/M3)
                    macCommands = new String[] {
                            "pg_ctl -D /opt/homebrew/var/postgresql@14 start",
                            "/opt/homebrew/bin/pg_ctl -D /opt/homebrew/var/postgresql@14 start",
                            "pg_ctl -D /opt/homebrew/var/postgres start",
                            "/opt/homebrew/bin/pg_ctl -D /opt/homebrew/var/postgres start",
                            "brew services start postgresql@14",
                            "brew services start postgresql"
                    };
                } else {
                    // Comandi specifici per Mac Intel
                    macCommands = new String[] {
                            "pg_ctl -D /usr/local/var/postgresql@14 start",
                            "/usr/local/bin/pg_ctl -D /usr/local/var/postgresql@14 start",
                            "pg_ctl -D /usr/local/var/postgres start",
                            "/usr/local/bin/pg_ctl -D /usr/local/var/postgres start",
                            "brew services start postgresql@14",
                            "brew services start postgresql"
                    };
                }

                // Aggiungi i comandi comuni per entrambe le architetture
                String[] commonMacCommands = {
                        // Installer ufficiale PostgreSQL
                        "/Library/PostgreSQL/14/bin/pg_ctl start -D /Library/PostgreSQL/14/data",
                        "/Library/PostgreSQL/15/bin/pg_ctl start -D /Library/PostgreSQL/15/data",
                        "/Library/PostgreSQL/16/bin/pg_ctl start -D /Library/PostgreSQL/16/data",

                        // Comandi specifici per PostgreSQL.app
                        "/Applications/Postgres.app/Contents/Versions/14/bin/pg_ctl -D /Applications/Postgres.app/Contents/Versions/14/data start",
                        "/Applications/Postgres.app/Contents/Versions/latest/bin/pg_ctl -D /Applications/Postgres.app/Contents/Versions/latest/data start"
                };

                // Combina i comandi specifici con quelli comuni
                String[] allMacCommands = new String[macCommands.length + commonMacCommands.length];
                System.arraycopy(macCommands, 0, allMacCommands, 0, macCommands.length);
                System.arraycopy(commonMacCommands, 0, allMacCommands, macCommands.length, commonMacCommands.length);

                // Prova tutti i comandi
                for (String command : allMacCommands) {
                  try {
                        Process process = Runtime.getRuntime().exec(command);
                        process.waitFor(5, TimeUnit.SECONDS);

                        // Attendi e verifica se PostgreSQL è partito
                        Thread.sleep(3000);
                        if (isPostgresRunning()) {
                           success = true;
                            break;
                        }
                    } catch (Exception e) {
                    }
                }

                // Se tutti i comandi falliscono, prova un approccio alternativo
                if (!success) {
                  try {
                        // Comando specifico basato sul percorso dove hai installato PostgreSQL
                        Process process = Runtime.getRuntime().exec("pg_ctl -D /opt/homebrew/var/postgresql@14 start");
                        process.waitFor(10, TimeUnit.SECONDS);

                        // Leggi l'uscita del processo
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;


                        // Attendi un po' più a lungo
                        Thread.sleep(5000);
                        if (isPostgresRunning()) {
                          success = true;
                        }
                    } catch (Exception e) {
                    }
                }
            } else if (osName.contains("win")) {
                // Comandi per Windows
                String[] windowsCommands = {
                        "net start postgresql",
                        "net start postgresql-x64-14",
                        "net start postgresql-14",
                        "sc start postgresql",
                        "sc start postgresql-x64-14",
                        "sc start postgresql-14",
                        "\"C:\\Program Files\\PostgreSQL\\14\\bin\\pg_ctl.exe\" -D \"C:\\Program Files\\PostgreSQL\\14\\data\" start",
                        "\"C:\\Program Files\\PostgreSQL\\15\\bin\\pg_ctl.exe\" -D \"C:\\Program Files\\PostgreSQL\\15\\data\" start",
                        "\"C:\\Program Files\\PostgreSQL\\16\\bin\\pg_ctl.exe\" -D \"C:\\Program Files\\PostgreSQL\\16\\data\" start",
                        "\"C:\\Program Files (x86)\\PostgreSQL\\14\\bin\\pg_ctl.exe\" -D \"C:\\Program Files (x86)\\PostgreSQL\\14\\data\" start"
                };

                for (String command : windowsCommands) {
                    try {
                        Process process;
                        if (command.startsWith("\"")) {
                            // Per comandi con percorsi che contengono spazi
                            process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", command});
                        } else {
                            process = Runtime.getRuntime().exec(command);
                        }

                        process.waitFor(5, TimeUnit.SECONDS);

                        // Attendi e verifica
                        Thread.sleep(3000);
                        if (isPostgresRunning()) {
                           success = true;
                            break;
                        }
                    } catch (Exception e) {
                    }
                }
            } else {
                // Linux
                String[] linuxCommands = {
                        "sudo systemctl start postgresql",
                        "sudo service postgresql start",
                        "sudo /etc/init.d/postgresql start",
                        "sudo pg_cluster 14 main start",
                        "sudo pg_cluster 15 main start",
                        "sudo pg_cluster 16 main start"
                };

                for (String command : linuxCommands) {
                    try {
                        Process process = Runtime.getRuntime().exec(command);
                        process.waitFor(5, TimeUnit.SECONDS);

                        Thread.sleep(3000);
                        if (isPostgresRunning()) {
                           success = true;
                            break;
                        }
                    } catch (Exception e) {
                    }
                }
            }

            // Se tutti i tentativi falliscono, mostra un avviso ma continua comunque
            if (!success) {

                Platform.runLater(() -> {
                    // Mostra un alert con le istruzioni per l'avvio manuale
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("PostgreSQL non avviato");
                    alert.setHeaderText("PostgreSQL deve essere avviato manualmente");

                    String osSpecificInstructions;
                    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                        osSpecificInstructions = "Esegui in un terminale uno dei seguenti comandi:\n\n" +
                                "Per Mac con Apple Silicon (M1/M2/M3):\n" +
                                "pg_ctl -D /opt/homebrew/var/postgresql@14 start\n\n" +
                                "Per Mac con Intel:\n" +
                                "pg_ctl -D /usr/local/var/postgresql@14 start\n\n" +
                                "Oppure installa PostgreSQL.app da: https://postgresapp.com";
                    } else if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        osSpecificInstructions = "Apri Servizi (services.msc), trova PostgreSQL e avvialo\n" +
                                "Oppure esegui in un prompt dei comandi (come amministratore):\n" +
                                "net start postgresql";
                    } else {
                        osSpecificInstructions = "Esegui in un terminale:\n" +
                                "sudo systemctl start postgresql";
                    }

                    alert.setContentText("Il server continuerà comunque l'avvio, ma se la connessione al database fallisce, " +
                            "potresti riscontrare errori.\n\n" + osSpecificInstructions);
                    alert.showAndWait();
                });
            }

            // Continua comunque, anche se PostgreSQL non è partito
            return true;
        } catch (Exception e) {
           return false;
        }
    }

    /**
     * Verifica se PostgreSQL è in esecuzione provando a connettersi alla porta 5432
     * @return true se PostgreSQL è in esecuzione, false altrimenti
     */
    private boolean isPostgresRunning() {
        // Prima prova a connettersi direttamente alla porta
        try (Socket socket = new Socket("localhost", 5432)) {
            return true; // Se riesce a connettersi, PostgreSQL è in esecuzione
        } catch (IOException e) {
            // Se fallisce, prova a eseguire un comando di controllo
            try {
                String checkCommand;
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    // Windows
                    checkCommand = "cmd /c pg_ready";
                } else {
                    // macOS o Linux
                    checkCommand = "pg_isready";
                }

                Process process = Runtime.getRuntime().exec(checkCommand);
                int exitCode = process.waitFor();

                return exitCode == 0;
            } catch (Exception ex) {
                // Se entrambi i metodi falliscono, PostgreSQL probabilmente non è in esecuzione
                return false;
            }
        }
    }

    private void importAdditionalBooks(Connection conn, String filePath) throws SQLException, IOException {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return;
        }


        String sql = "INSERT INTO books (title, authors, category, publisher, publish_year) " +
                "VALUES (?, ?, ?, ?, ?) ON CONFLICT (title, authors) DO NOTHING";

        int successCount = 0;
        int errorCount = 0;

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line = reader.readLine(); // Read the header line
            if (line == null) return;

            while ((line = reader.readLine()) != null) {
                try {
                    String[] fields = line.split("\t");
                    if (fields.length >= 5) {
                        pstmt.setString(1, fields[0].trim()); // Titolo
                        pstmt.setString(2, fields[1].trim()); // Autore
                        pstmt.setString(3, fields[2].trim()); // Categoria
                        pstmt.setString(4, fields[3].trim()); // Editore

                        // Anno
                        try {
                            pstmt.setInt(5, Integer.parseInt(fields[4].trim()));
                        } catch (NumberFormatException e) {
                            pstmt.setNull(5, Types.INTEGER);
                        }

                        pstmt.executeUpdate();
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                }
            }
        }
}

    private void importUsers(Connection conn) throws SQLException, IOException {
      
        String sql = "INSERT INTO users (user_id, full_name, fiscal_code, email, password) " +
                "VALUES (?, ?, ?, ?, ?) ON CONFLICT (user_id) DO UPDATE SET " +
                "full_name = EXCLUDED.full_name, " +
                "fiscal_code = EXCLUDED.fiscal_code, " +
                "email = EXCLUDED.email, " +
                "password = EXCLUDED.password";

        int userCount = 0;
        int errorCount = 0;

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader reader = new BufferedReader(new FileReader(TEMP_DIR + "UtentiRegistrati.csv"))) {

            String line = reader.readLine(); // Skip header if exists
       
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    String[] fields;

                    // Detect if this is a tab-delimited file
                    if (line.contains("\t")) {
                        fields = line.split("\t", -1);
                    } else {
                        fields = parseCsvLine(line);
                    }

                    if (fields.length < 5) {
                     continue;
                    }

                    String fullName = fields[0].trim();
                    String fiscalCode = fields[1].trim();
                    String email = fields[2].trim();
                    String userId = fields[3].trim();
                    String password = fields[4].trim();

                    // Skip if required fields are empty
                    if (userId.isEmpty() || fullName.isEmpty()) {
                        continue;
                    }

                    pstmt.setString(1, userId);
                    pstmt.setString(2, fullName);
                    pstmt.setString(3, fiscalCode);
                    pstmt.setString(4, email);
                    pstmt.setString(5, password);

                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        userCount++;
                        if (userCount % 50 == 0) {
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                }
            }
        }

    }


    private void importLibraries(Connection conn) throws SQLException, IOException {

        String insertLibrarySql = "INSERT INTO libraries (user_id, library_name) " +
                "VALUES (?, ?) ON CONFLICT (user_id, library_name) DO NOTHING " +
                "RETURNING id";

        String insertLibraryBookSql = "INSERT INTO library_books (library_id, book_id) " +
                "VALUES (?, ?) ON CONFLICT DO NOTHING";

        String findBookSql = "SELECT id FROM books WHERE title ILIKE ?";

        int libraryCount = 0;
        int bookCount = 0;
        int errorCount = 0;

        try (PreparedStatement pstmtLib = conn.prepareStatement(insertLibrarySql);
             PreparedStatement pstmtLibBook = conn.prepareStatement(insertLibraryBookSql);
             PreparedStatement pstmtFindBook = conn.prepareStatement(findBookSql);
             BufferedReader reader = new BufferedReader(new FileReader(TEMP_DIR + "Librerie.dati.csv"))) {

            String line = reader.readLine(); // Skip header if exists

            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    String[] fields;

                    // Detect if this is a tab-delimited file
                    if (line.contains("\t")) {
                        fields = line.split("\t", -1);
                    } else {
                        fields = parseCsvLine(line);
                    }

                    if (fields.length < 2) {
                        continue;
                    }

                    String userId = fields[0].trim();
                    String libraryName = fields[1].trim();

                    // Skip if required fields are empty
                    if (userId.isEmpty() || libraryName.isEmpty()) {
                        continue;
                    }

                    // If userId contains a tab character, it might be incorrectly parsed
                    if (userId.contains("\t")) {
                        String[] parts = userId.split("\t");
                        userId = parts[0].trim();

                        // Adjust other fields if needed
                        if (parts.length > 1 && (libraryName == null || libraryName.isEmpty())) {
                            libraryName = parts[1].trim();
                        }
                    }

                    // Check if user exists
                    String checkUserSql = "SELECT 1 FROM users WHERE user_id = ?";
                    try (PreparedStatement pstmtCheckUser = conn.prepareStatement(checkUserSql)) {
                        pstmtCheckUser.setString(1, userId);
                        ResultSet userRs = pstmtCheckUser.executeQuery();

                        if (!userRs.next()) {
                            // Try to create the user
                            try {
                                String createUserSql = "INSERT INTO users (user_id, full_name, fiscal_code, email, password) " +
                                        "VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";
                                try (PreparedStatement pstmtCreateUser = conn.prepareStatement(createUserSql)) {
                                    pstmtCreateUser.setString(1, userId);
                                    pstmtCreateUser.setString(2, userId); // Use userId as name temporarily
                                    pstmtCreateUser.setString(3, "UNKNOWN");
                                    pstmtCreateUser.setString(4, userId + "@example.com");
                                    pstmtCreateUser.setString(5, "password");

                                    int created = pstmtCreateUser.executeUpdate();
                                    if (created > 0) {
                                    } else {
                                        continue; // Skip if we can't create the user
                                    }
                                }
                            } catch (SQLException e) {
                                continue;
                            }
                        }
                    }

                    // Insert library
                    pstmtLib.setString(1, userId);
                    pstmtLib.setString(2, libraryName);

                    ResultSet rs = pstmtLib.executeQuery();
                    if (rs.next()) {
                        int libraryId = rs.getInt(1);
                        libraryCount++;

                        // Add books to library (remaining fields are book titles)
                        for (int i = 2; i < fields.length; i++) {
                            String bookTitle = fields[i].trim();
                            if (bookTitle.isEmpty() || bookTitle.equalsIgnoreCase("null")) {
                                continue;
                            }

                            // Try to find the book (case insensitive)
                            pstmtFindBook.setString(1, "%" + bookTitle + "%");
                            ResultSet bookRs = pstmtFindBook.executeQuery();

                            if (bookRs.next()) {
                                int bookId = bookRs.getInt(1);
                                pstmtLibBook.setInt(1, libraryId);
                                pstmtLibBook.setInt(2, bookId);

                                int added = pstmtLibBook.executeUpdate();
                                if (added > 0) {
                                    bookCount++;
                                }
                            } else {
                                // Create the book
                                String insertBookSql =
                                        "INSERT INTO books (title, authors, category, publisher) " +
                                                "VALUES (?, 'Unknown', 'Unknown', 'Unknown') RETURNING id";
                                try (PreparedStatement pstmtInsertBook = conn.prepareStatement(insertBookSql)) {
                                    pstmtInsertBook.setString(1, bookTitle);
                                    ResultSet newBookRs = pstmtInsertBook.executeQuery();

                                    if (newBookRs.next()) {
                                        int bookId = newBookRs.getInt(1);
                                        pstmtLibBook.setInt(1, libraryId);
                                        pstmtLibBook.setInt(2, bookId);

                                        int added = pstmtLibBook.executeUpdate();
                                        if (added > 0) {
                                            bookCount++;
                                        }
                                    }
                                } catch (SQLException e) {
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                }
            }
        }

   }

    private void importRatings(Connection conn) throws SQLException, IOException {
        String findBookSql = "SELECT id FROM books WHERE title ILIKE ?";

        String insertRatingSql = "INSERT INTO book_ratings " +
                "(user_id, book_id, style_rating, content_rating, pleasantness_rating, " +
                "originality_rating, edition_rating, average_rating, general_comment, " +
                "style_comment, content_comment, pleasantness_comment, originality_comment, edition_comment) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (user_id, book_id) DO NOTHING";

        int ratingCount = 0;
        int errorCount = 0;

        try (PreparedStatement pstmtFindBook = conn.prepareStatement(findBookSql);
             PreparedStatement pstmtRating = conn.prepareStatement(insertRatingSql);
             BufferedReader reader = new BufferedReader(new FileReader(TEMP_DIR + "ValutazioniLibri.csv"))) {

            String line = reader.readLine(); // Skip header

            // Check if the format is numeric ID or user_id
            boolean isNumericIdFormat = line != null && line.contains("\"id\"");

            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    // Parse CSV line handling quoted values
                    String[] fields = parseCsvLine(line);

                    if (fields.length < 5) {
                        continue;
                    }

                    String userId;
                    String bookTitle;
                    int startIndexForRatings;

                    if (isNumericIdFormat) {
                        // Format is like: id, user_id, book_id, ...
                        // Skip the id field
                        userId = fields[1].trim();
                        bookTitle = fields[2].trim();
                        startIndexForRatings = 3;
                    } else {
                        // Format is like: user_id, book_title, ...
                        userId = fields[0].trim();
                        bookTitle = fields[1].trim();
                        startIndexForRatings = 2;
                    }

                    // Skip if required fields are empty
                    if (userId.isEmpty() || bookTitle.isEmpty()) {
                        continue;
                    }

                    // Check if user exists
                    String checkUserSql = "SELECT 1 FROM users WHERE user_id = ?";
                    try (PreparedStatement pstmtCheckUser = conn.prepareStatement(checkUserSql)) {
                        pstmtCheckUser.setString(1, userId);
                        ResultSet userRs = pstmtCheckUser.executeQuery();

                        if (!userRs.next()) {
                            // Try to create the user
                            try {
                                String createUserSql = "INSERT INTO users (user_id, full_name, fiscal_code, email, password) " +
                                        "VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";
                                try (PreparedStatement pstmtCreateUser = conn.prepareStatement(createUserSql)) {
                                    pstmtCreateUser.setString(1, userId);
                                    pstmtCreateUser.setString(2, userId); // Use userId as name temporarily
                                    pstmtCreateUser.setString(3, "UNKNOWN");
                                    pstmtCreateUser.setString(4, userId + "@example.com");
                                    pstmtCreateUser.setString(5, "password");

                                    int created = pstmtCreateUser.executeUpdate();
                                    if (created <= 0) {
                                        continue; // Skip if we can't create the user
                                    }
                                }
                            } catch (SQLException e) {
                                continue;
                            }
                        }
                    }

                    // Find or create book
                    Integer bookId = null;

                    // Try to find the book (case insensitive)
                    pstmtFindBook.setString(1, "%" + bookTitle + "%");
                    ResultSet bookRs = pstmtFindBook.executeQuery();

                    if (bookRs.next()) {
                        bookId = bookRs.getInt(1);
                    } else {
                        // Book not found, create it
                        String insertBookSql =
                                "INSERT INTO books (title, authors, category, publisher) " +
                                        "VALUES (?, 'Unknown', 'Unknown', 'Unknown') RETURNING id";
                        try (PreparedStatement pstmtInsertBook = conn.prepareStatement(insertBookSql)) {
                            pstmtInsertBook.setString(1, bookTitle);
                            ResultSet newBookRs = pstmtInsertBook.executeQuery();

                            if (newBookRs.next()) {
                                bookId = newBookRs.getInt(1);
                            }
                        } catch (SQLException e) {
                            continue;
                        }
                    }

                    if (bookId == null) {
                        continue;
                    }

                    // Set user_id and book_id - parametri 1 e 2
                    pstmtRating.setString(1, userId);
                    pstmtRating.setInt(2, bookId);

                    // Process ratings from fields
                    if (fields.length >= startIndexForRatings + 5) {
                        try {
                            // Parse the ratings (indexes 3-7 for numeric format, or 2-6 for standard format)
                            int styleRating = parseInt(fields[startIndexForRatings], 3);
                            int contentRating = parseInt(fields[startIndexForRatings + 1], 3);
                            int pleasantnessRating = parseInt(fields[startIndexForRatings + 2], 3);
                            int originalityRating = parseInt(fields[startIndexForRatings + 3], 3);
                            int editionRating = parseInt(fields[startIndexForRatings + 4], 3);

                            // Set the ratings - parametri 3-7
                            pstmtRating.setInt(3, styleRating);
                            pstmtRating.setInt(4, contentRating);
                            pstmtRating.setInt(5, pleasantnessRating);
                            pstmtRating.setInt(6, originalityRating);
                            pstmtRating.setInt(7, editionRating);

                            // Calculate average rating - parametro 8
                            float avgRating = (styleRating + contentRating + pleasantnessRating +
                                    originalityRating + editionRating) / 5.0f;
                            // Round to one decimal place
                            avgRating = Math.round(avgRating * 10) / 10.0f;
                            pstmtRating.setFloat(8, avgRating);

                            // L'indice per accedere all'array fields
                            int commentIndex = startIndexForRatings + 6;

                            // CORRISPONDENZA TRA INDICI DELL'ARRAY E PARAMETRI JDBC:
                            // fields[commentIndex] -> parametro 9 (general_comment)
                            // fields[commentIndex+1] -> parametro 10 (style_comment)
                            // fields[commentIndex+2] -> parametro 11 (content_comment)
                            // fields[commentIndex+3] -> parametro 12 (pleasantness_comment)
                            // fields[commentIndex+4] -> parametro 13 (originality_comment)
                            // fields[commentIndex+5] -> parametro 14 (edition_comment)

                            // Set general_comment (parametro 9, indice array: commentIndex)
                            if (fields.length > commentIndex) {
                                String comment = fields[commentIndex].trim();
                                pstmtRating.setString(9, comment.isEmpty() ? null : comment);
                            } else {
                                pstmtRating.setNull(9, Types.VARCHAR);
                            }

                            // Set style_comment (parametro 10, indice array: commentIndex+1)
                            if (fields.length > commentIndex + 1) {
                                String comment = fields[commentIndex + 1].trim();
                                pstmtRating.setString(10, comment.isEmpty() ? null : comment);
                            } else {
                                pstmtRating.setNull(10, Types.VARCHAR);
                            }

                            // Set content_comment (parametro 11, indice array: commentIndex+2)
                            if (fields.length > commentIndex + 2) {
                                String comment = fields[commentIndex + 2].trim();
                                pstmtRating.setString(11, comment.isEmpty() ? null : comment);
                            } else {
                                pstmtRating.setNull(11, Types.VARCHAR);
                            }

                            // Set pleasantness_comment (parametro 12, indice array: commentIndex+3)
                            if (fields.length > commentIndex + 3) {
                                String comment = fields[commentIndex + 3].trim();
                                pstmtRating.setString(12, comment.isEmpty() ? null : comment);
                            } else {
                                pstmtRating.setNull(12, Types.VARCHAR);
                            }

                            // Set originality_comment (parametro 13, indice array: commentIndex+4)
                            if (fields.length > commentIndex + 4) {
                                String comment = fields[commentIndex + 4].trim();
                                pstmtRating.setString(13, comment.isEmpty() ? null : comment);
                            } else {
                                pstmtRating.setNull(13, Types.VARCHAR);
                            }

                            // Set edition_comment (parametro 14, indice array: commentIndex+5)
                            if (fields.length > commentIndex + 5) {
                                String comment = fields[commentIndex + 5].trim();
                                pstmtRating.setString(14, comment.isEmpty() ? null : comment);
                            } else {
                                pstmtRating.setNull(14, Types.VARCHAR);
                            }

                            // Execute the query
                            int rowsAffected = pstmtRating.executeUpdate();
                            if (rowsAffected > 0) {
                                ratingCount++;
                                if (ratingCount % 50 == 0) {
                                    // Optional logging if needed
                                }
                            }
                        } catch (NumberFormatException e) {
                            errorCount++;
                        }
                    } else {
                        errorCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                }
            }
        }
    }

    private void startSocketServer() {
        int[] portsToTry = {8888, 8889, 8890, 8891, 8892};
        boolean success = false;

        for (int port : portsToTry) {
            try {
                // Create a server socket that binds to all network interfaces
                serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
                success = true;

                // Start a thread for accepting client connections
                new Thread(() -> {
                    while (serverRunning) {
                        try {
                            Socket clientSocket = serverSocket.accept();

                            // The rest of your code remains the same
                            // ...

                        } catch (IOException e) {
                            if (serverRunning) {
                           }
                        }
                    }
                }).start();

                break; // Exit the loop if successful

            } catch (IOException e) {
            }
        }

        if (!success) {
            String errorMsg = "Could not bind to any port. Tried ports: " + Arrays.toString(portsToTry);
          throw new RuntimeException("Failed to start server: " + errorMsg);
        }
    }
    private void handleClient(Socket clientSocket) {
        try {
            // Add socket to the list of connected clients
            synchronized(connectedClientSockets) {
                connectedClientSockets.add(clientSocket);
            }

            // Set up input stream for reading commands
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Wait for a client to disconnect
            while (!clientSocket.isClosed() && serverRunning) {
                try {
                    // Check for messages
                    if (in.ready()) {
                        String message = in.readLine();
                        // Process any client messages if needed
                    }

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
        } finally {
            // Clean up
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }

            // Remove from a list of connected clients
            synchronized(connectedClientSockets) {
                connectedClientSockets.remove(clientSocket);
            }

            // Decrement client counter on disconnect
            int currentClients = connectedClients.decrementAndGet();
            Platform.runLater(() -> {
                clientCountLabel.setText(String.valueOf(currentClients));
            });
 }
    }
    /**
     * Notifies all connected clients about server shutdown and initiates server shutdown
     */
    public void notifyAllClientsAndShutdown() {
        if (!serverRunning) return;


        // Notify all connected clients
        synchronized (connectedClientSockets) {
            for (Socket clientSocket : connectedClientSockets) {
                try {
                    if (!clientSocket.isClosed()) {
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println("SERVER_SHUTDOWN");
                    }
                } catch (IOException e) {
                }
            }
        }

        // Now proceed with a normal shutdown
        serverRunning = false;

        // Close a server socket if it exists
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }

        // Shutdown scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        // Reset UI state
        Platform.runLater(() -> {
            updateUIState(false);

            // Reset client count
            connectedClients.set(0);
            clientCountLabel.setText("0");

            serverStatusLabel.setText("Stopped");
            serverStatusLabel.setTextFill(Color.RED);
            startTimeLabel.setText("-");
            uptimeLabel.setText("-");
        });

    }


    /**
     * Improved CSV line parser that handles both comma and tab-delimited formats
     * and properly manages quoted fields
     */
    private String[] parseCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new String[0];
        }

        // Check if the line contains tabs - if so, use tab delimiter
        if (line.contains("\t")) {
            return parseTabDelimitedLine(line);
        } else {
            return parseCommaDelimitedLine(line);
        }
    }

    /**
     * Parse a tab-delimited line
     */
    private String[] parseTabDelimitedLine(String line) {
        String[] fields = line.split("\t", -1); // -1 to keep empty fields

        // Trim each field and handle null values
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
            if (fields[i].equalsIgnoreCase("null") || fields[i].isEmpty()) {
                fields[i] = "";
            }
        }

        return fields;
    }

    /**
     * Parse a comma-delimited line with proper handling of quoted fields
     */
    private String[] parseCommaDelimitedLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // If we're in quotes and the next char is also a quote, it's an escaped quote
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // Skip the next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field, add to list
                tokens.add(sb.toString().trim());
                sb = new StringBuilder();
            } else {
                // Regular character, append
                sb.append(c);
            }
        }

        // Add the last token
        tokens.add(sb.toString().trim());

        // Process each token to handle "null" values
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("null")) {
                tokens.set(i, "");
            }
        }

        return tokens.toArray(new String[0]);
    }


    private void startUptimeCounter() {
        // Cancel the existing scheduler if any
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        // Create new scheduler
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (serverStartTime != null) {
                Duration uptime = Duration.between(serverStartTime, LocalDateTime.now());
                long days = uptime.toDays();
                long hours = uptime.toHoursPart();
                long minutes = uptime.toMinutesPart();
                long seconds = uptime.toSecondsPart();

                String uptimeStr = String.format("%d days, %02d:%02d:%02d", days, hours, minutes, seconds);

                Platform.runLater(() -> {
                    uptimeLabel.setText(uptimeStr);
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void updateProgress(double progress, String message) {
        Platform.runLater(() -> {
            initProgressBar.setProgress(progress);
            serverStatusLabel.setText(message);
        });
    }

    private void updateUIState(boolean running) {
        Platform.runLater(() -> {
            // Update button states
            startButton.setDisable(running);
            stopButton.setDisable(!running);


            // Update fields states
            dbUrlField.setDisable(running);
            dbUserField.setDisable(running);
            dbPasswordField.setDisable(running);

            // Update status label
            if (!running) {
                serverStatusLabel.setText("Stopped");
                serverStatusLabel.setTextFill(Color.RED);
                startTimeLabel.setText("-");
                uptimeLabel.setText("-");
            }
        });
    }



    private enum LogType {
        INFO(Color.RED),
        SUCCESS(Color.GREEN),
        WARNING(Color.ORANGE),
        ERROR(Color.RED);

        private final Color color;

        LogType(Color color) {
            this.color = color;
        }
    }
}