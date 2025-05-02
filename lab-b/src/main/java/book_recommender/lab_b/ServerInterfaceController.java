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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerInterfaceController {

    // Google Drive file IDs
    private static final String VALUTAZIONI_FILE_ID = "1tA9TbRVQK6ioJPU36_aHqTaQLZp1nI8L";
    private static final String UTENTI_FILE_ID = "13D5cg16dW6gp4ZfsTBYVfGu1FUFBbIsD";
    private static final String LIBRI_FILE_ID = "1gGz_uVrfVlPvErMShcTU630dYGF5nNOO";
    private static final String LIBRERIE_FILE_ID = "1r0jBnnG-aKBQZ76eyKxzWpsL1aLThZW3";
    private static final String DATA_FILE_ID = "1jX6zWXBjf6-eT1y9ypv55tfEtawz86Cz";
    private static final String CONSIGLI_FILE_ID = "1hLT7yvnA2hV6Rg_oTnFsywyPaPstjKo8";
    // Add at the top of the class with other fields
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

    @FXML
    public void initialize() {
        // Create a temp directory if it doesn't exist
        new File(TEMP_DIR).mkdirs();

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
                String connectionInfo = getNgrokConnectionInfo();
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(connectionInfo);
                clipboard.setContent(content);
                addLogMessage("Informazioni di connessione ngrok copiate negli appunti", LogType.INFO);
            });
            contextMenu.getItems().add(copyItem);
            ngrokHostField.setContextMenu(contextMenu);
            ngrokPortField.setContextMenu(contextMenu);
        }

        // Remaining code stays the same...
    }

    @FXML
    public void onCopyNgrokInfo(ActionEvent event) {
        String connectionInfo = getNgrokConnectionInfo();

        // Copia negli appunti
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(connectionInfo);
        clipboard.setContent(content);

        addLogMessage("Informazioni di connessione ngrok copiate negli appunti", LogType.INFO);
    }

    /**
     * Gets a formatted string with Ngrok connection information
     * @return String containing connection details
     */
    private String getNgrokConnectionInfo() {
        if (!ngrokEnabled || ngrokManager == null) {
            return "Ngrok non attivo";
        }

        String publicUrl = ngrokManager.getPublicUrl();
        int publicPort = ngrokManager.getPublicPort();

        return "Host Ngrok: " + publicUrl + "\n" +
                "Porta Ngrok: " + publicPort + "\n" +
                "Database: book_recommender\n" +
                "Username: book_admin_8530\n" +
                "Password: CPuc#@r-zbKY";
    }


    @FXML
    public void onStartServer(ActionEvent event) {
        if (serverRunning) return;

        // Disable the button during initialization
        startButton.setDisable(true);

        addLogMessage("Checking for existing server...", LogType.INFO);

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
                    addLogMessage("PostgreSQL not installed, attempting to install...", LogType.WARNING);
                    if (!installPostgresIfNeeded()) {
                        Platform.runLater(() -> {
                            dbStatusLabel.setText("PostgreSQL not installed");
                            dbStatusLabel.setTextFill(Color.RED);
                            addLogMessage("PostgreSQL installation is required to continue", LogType.ERROR);
                            startButton.setDisable(false);
                        });
                        return;
                    }
                }

                if (!isPostgresRunning()) {
                    addLogMessage("PostgreSQL not running, attempting to start...", LogType.WARNING);
                    if (!startPostgresIfNeeded()) {
                        Platform.runLater(() -> {
                            dbStatusLabel.setText("PostgreSQL is not running");
                            dbStatusLabel.setTextFill(Color.RED);
                            addLogMessage("Unable to start PostgreSQL. Start it manually.", LogType.ERROR);
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
                    addLogMessage("Error: " + e.getMessage(), LogType.ERROR);
                    startButton.setDisable(false);
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
            if (serverRunning) {
                addLogMessage("Found existing server running", LogType.INFO);
            } else {
                addLogMessage("No existing server found, will create a new one", LogType.INFO);
            }
            return serverRunning;
        } catch (Exception e) {
            addLogMessage("Error checking for existing server: " + e.getMessage(), LogType.ERROR);
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

                    addLogMessage("Tunnel ngrok avviato con successo: " + publicUrl + ":" + publicPort, LogType.SUCCESS);
                } else {
                    ngrokStatusLabel.setText("Errore");
                    ngrokStatusLabel.setTextFill(Color.RED);
                    startNgrokButton.setDisable(false);

                    addLogMessage("Errore nell'avvio di ngrok", LogType.ERROR);
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

        addLogMessage("Tunnel ngrok arrestato", LogType.INFO);
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

            addLogMessage("Connected to existing server database", LogType.SUCCESS);

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
                addLogMessage("Error connecting to existing server: " + e.getMessage(), LogType.ERROR);
                startButton.setDisable(false);
            });
        }
    }

    /**
     * Starts a thread to periodically update the connected client count
     */
    private void startClientCountMonitoring() {
        // Scheduler to update the client count every 3 seconds
        ScheduledExecutorService clientMonitor = Executors.newScheduledThreadPool(1);
        clientMonitor.scheduleAtFixedRate(() -> {
            if (!serverRunning) {
                clientMonitor.shutdown();
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
                // Log error but continue trying
                addLogMessage("Error updating client count: " + e.getMessage(), LogType.ERROR);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }


    @FXML

    private void onStopServer(ActionEvent event) {
        if (!serverRunning) return;

        cleanupDatabaseAndShutdown();
    }
    @FXML
    private void onClearLog() {
        logContainer.getChildren().clear();
        addLogMessage("Log cleared", LogType.INFO);
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
            addLogMessage("Server initialization started", LogType.INFO);

            // Start a socket server early
            updateProgress(0.1, "Starting socket server...");
            startSocketServer();
            addLogMessage("Socket server started on port 8888", LogType.SUCCESS);

            // Step 1.5: Verifica e avvia PostgreSQL se necessario
            updateProgress(0.15, "Checking PostgreSQL status...");
            if (!isPostgresInstalled()) {
                addLogMessage("PostgreSQL not installed, attempting to install...", LogType.WARNING);
                installPostgresIfNeeded(); // Continua anche se fallisce
            }

            if (!isPostgresRunning()) {
                addLogMessage("PostgreSQL not running, attempting to start...", LogType.WARNING);
                startPostgresIfNeeded(); // Continua anche se fallisce
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

                addLogMessage("Database connection initialized successfully", LogType.SUCCESS);

                // Step 3: Always download files
                updateProgress(0.3, "Downloading data files...");
                downloadAllFiles();

                // Step 4: Always initialize a database
                updateProgress(0.5, "Recreating database tables...");
                initializeDatabase(dbUrl, finalDbUser, finalDbPassword);

                // Step 5: Always import data
                updateProgress(0.7, "Importing data...");
                populateDatabase(dbUrl, finalDbUser, finalDbPassword);

                // Step 7: Complete
                updateProgress(1.0, "Server started successfully!");
                Platform.runLater(() -> {
                    serverStatusLabel.setText("Running");
                    serverStatusLabel.setTextFill(Color.GREEN);

                    // Final step: Start Ngrok automatically
                    updateProgress(0.9, "Avvio tunnel ngrok...");
                    startNgrokAutomatically();

                    // Complete
                    updateProgress(1.0, "Server avviato con successo!");
                    Platform.runLater(() -> {
                        serverStatusLabel.setText("Running");
                        serverStatusLabel.setTextFill(Color.GREEN);
                    });
                });
            } catch (Exception e) {
                throw new RuntimeException("Database initialization failed", e);
            }

        } catch (Exception e) {
            String errorMsg = "Server initialization failed: " + e.getMessage();
            addLogMessage(errorMsg, LogType.ERROR);
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

                    addLogMessage("Tunnel ngrok avviato automaticamente", LogType.SUCCESS);

                    // Display connection info clearly
                    String dbInfo = "==========================================\n" +
                            "INFORMAZIONI DI CONNESSIONE PER IL CLIENT:\n" +
                            "Host Ngrok: " + publicUrl + "\n" +
                            "Porta Ngrok: " + publicPort + "\n" +
                            "Database: book_recommender\n" +
                            "Username: book_admin_8530\n" +
                            "Password: CPuc#@r-zbKY\n" +
                            "==========================================";
                    addLogMessage(dbInfo, LogType.INFO);
                } else {
                    ngrokStatusLabel.setText("Errore");
                    ngrokStatusLabel.setTextFill(Color.RED);
                    addLogMessage("Errore nell'avvio automatico di ngrok", LogType.ERROR);
                }
            });
        }).start();
    }
    /**
     * Notifies all clients of a shutdown, cleans up a database and shuts down the server
     */
    public void cleanupDatabaseAndShutdown() {
        if (!serverRunning) return;

        // Arresta il tunnel ngrok se attivo
        if (ngrokManager != null) {
            try {
                addLogMessage("Arresto del tunnel ngrok...", LogType.INFO);
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
                        startNgrokButton.setDisable(true);
                    }
                    if (stopNgrokButton != null) {
                        stopNgrokButton.setDisable(true);
                    }
                });

                addLogMessage("Tunnel ngrok arrestato con successo", LogType.SUCCESS);
            } catch (Exception e) {
                addLogMessage("Errore durante l'arresto del tunnel ngrok: " + e.getMessage(), LogType.ERROR);
            }
        }

        // Clean up a database
        cleanDatabase();

        // Delete all downloaded files
        deleteTemporaryFiles();

        // Now proceed with a normal shutdown
        serverRunning = false;

        // Close a server socket if it exists
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                addLogMessage("Server socket closed", LogType.SUCCESS);
            } catch (IOException e) {
                addLogMessage("Error closing server socket: " + e.getMessage(), LogType.ERROR);
            }
        }

        // Shutdown scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            try {
                scheduler.shutdown();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                addLogMessage("Scheduler terminated", LogType.SUCCESS);
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
                addLogMessage("Scheduler shutdown interrupted", LogType.WARNING);
            }
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

        addLogMessage("Server stopped, database cleaned, and all clients notified", LogType.SUCCESS);
    }
    /**
     * Delete all files in the temporary directory
     */
    private void deleteTemporaryFiles() {
        try {
            File tempDir = new File(TEMP_DIR);
            if (tempDir.exists() && tempDir.isDirectory()) {
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            boolean deleted = file.delete();
                            if (!deleted) {
                                addLogMessage("Failed to delete file: " + file.getName(), LogType.WARNING);
                                // Try to force to delete it on exit
                                file.deleteOnExit();
                            }
                        }
                    }
                }
                // Try to delete the directory itself
                boolean dirDeleted = tempDir.delete();
                if (!dirDeleted) {
                    // If not empty or in use, mark for deletion on exit
                    tempDir.deleteOnExit();
                }
                addLogMessage("Temporary files cleaned up", LogType.SUCCESS);
            }
        } catch (Exception e) {
            addLogMessage("Error deleting temp files: " + e.getMessage(), LogType.ERROR);
        }
    }
    /**
     * Cleans up the database by dropping all tables
     */
    private void cleanDatabase() {
        addLogMessage("Cleaning up database...", LogType.INFO);

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

            addLogMessage("Database cleaned successfully", LogType.SUCCESS);
        } catch (SQLException e) {
            addLogMessage("Error cleaning database: " + e.getMessage(), LogType.ERROR);
        }
    }

    private void downloadAllFiles() throws IOException {
        addLogMessage("Downloading files from Google Drive...", LogType.INFO);

        downloadFromGoogleDrive(VALUTAZIONI_FILE_ID, "ValutazioniLibri.csv");
        downloadFromGoogleDrive(UTENTI_FILE_ID, "UtentiRegistrati.csv");
        downloadFromGoogleDrive(LIBRI_FILE_ID, "Libri.csv");
        downloadFromGoogleDrive(LIBRERIE_FILE_ID, "Librerie.dati.csv");
        downloadFromGoogleDrive(DATA_FILE_ID, "Data.csv");
        downloadFromGoogleDrive(CONSIGLI_FILE_ID, "ConsigliLibri.dati.csv");

        addLogMessage("All files downloaded successfully", LogType.SUCCESS);
    }

    private void downloadFromGoogleDrive(String fileId, String fileName) throws IOException {
        String urlString = "https://drive.google.com/uc?id=" + fileId + "&export=download";

        try {
            URL url = URI.create(urlString).toURL();

            try (InputStream in = url.openStream();
                 ReadableByteChannel rbc = Channels.newChannel(in);
                 FileOutputStream fos = new FileOutputStream(TEMP_DIR + fileName)) {

                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                addLogMessage("Downloaded: " + fileName, LogType.SUCCESS);
            }
        } catch (Exception e) {
            String errorMsg = "Error downloading file " + fileName + ": " + e.getMessage();
            addLogMessage(errorMsg, LogType.ERROR);
            throw new IOException("Failed to download file: " + fileName, e);
        }
    }

    private void initializeDatabase(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        addLogMessage("Creating database tables...", LogType.INFO);

        try {
            // Use the DatabaseManager instance instead of direct connection
            DatabaseManager dbManager = DatabaseManager.getInstance();
            Connection conn = dbManager.getConnection();

            try (Statement stmt = conn.createStatement()) {
                // Drop existing tables if any
                String[] dropStatements = {
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

                // Create tables
                String[] createTableStatements = {
                        // Users' table
                        "CREATE TABLE IF NOT EXISTS users (" +
                                "    user_id VARCHAR(8) PRIMARY KEY," +
                                "    full_name VARCHAR(100) NOT NULL," +
                                "    fiscal_code VARCHAR(16) NOT NULL UNIQUE," +
                                "    email VARCHAR(100) NOT NULL," +
                                "    password VARCHAR(100) NOT NULL" +
                                ")",

                        // Books table
                        "CREATE TABLE IF NOT EXISTS books (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    title TEXT NOT NULL," +
                                "    authors TEXT NOT NULL," +
                                "    category TEXT," +
                                "    publisher TEXT," +
                                "    publish_year INTEGER," +
                                "    UNIQUE(title, authors)" +
                                ")",

                        // Libraries' table
                        "CREATE TABLE IF NOT EXISTS libraries (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    user_id VARCHAR(8) REFERENCES users(user_id) ON DELETE CASCADE," +
                                "    library_name VARCHAR(100) NOT NULL," +
                                "    UNIQUE(user_id, library_name)" +
                                ")",

                        // Library_Books table (many-to-many relationship)
                        "CREATE TABLE IF NOT EXISTS library_books (" +
                                "    library_id INTEGER REFERENCES libraries(id) ON DELETE CASCADE," +
                                "    book_id INTEGER REFERENCES books(id) ON DELETE CASCADE," +
                                "    PRIMARY KEY (library_id, book_id)" +
                                ")",

                        // Book_Ratings table
                        "CREATE TABLE IF NOT EXISTS book_ratings (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    user_id VARCHAR(8) REFERENCES users(user_id) ON DELETE CASCADE," +
                                "    book_id INTEGER REFERENCES books(id) ON DELETE CASCADE," +
                                "    style_rating INTEGER CHECK (style_rating >= 1 AND style_rating <= 5)," +
                                "    content_rating INTEGER CHECK (content_rating >= 1 AND content_rating <= 5)," +
                                "    pleasantness_rating INTEGER CHECK (pleasantness_rating >= 1 AND pleasantness_rating <= 5)," +
                                "    originality_rating INTEGER CHECK (originality_rating >= 1 AND originality_rating <= 5)," +
                                "    edition_rating INTEGER CHECK (edition_rating >= 1 AND edition_rating <= 5)," +
                                "    average_rating FLOAT," +
                                "    general_comment TEXT," +
                                "    style_comment TEXT," +
                                "    content_comment TEXT," +
                                "    pleasantness_comment TEXT," +
                                "    originality_comment TEXT," +
                                "    edition_comment TEXT," +
                                "    UNIQUE(user_id, book_id)" +
                                ")",

                        // Book_Recommendations table
                        "CREATE TABLE IF NOT EXISTS book_recommendations (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    user_id VARCHAR(8) REFERENCES users(user_id) ON DELETE CASCADE," +
                                "    source_book_id INTEGER REFERENCES books(id) ON DELETE CASCADE," +
                                "    recommended_book_id INTEGER REFERENCES books(id) ON DELETE CASCADE," +
                                "    UNIQUE(user_id, source_book_id, recommended_book_id)" +
                                ")"
                };

                for (String sql : createTableStatements) {
                    stmt.execute(sql);
                }

                addLogMessage("Database tables created successfully", LogType.SUCCESS);
            }
        } catch (SQLException e) {
            addLogMessage("Error creating database tables: " + e.getMessage(), LogType.ERROR);
            throw e;
        }
    }
    private void populateDatabase(String dbUrl, String dbUser, String dbPassword) throws SQLException, IOException {
        addLogMessage("Populating database with data...", LogType.INFO);

        try {
            // Use the DatabaseManager instance instead of direct connection
            DatabaseManager dbManager = DatabaseManager.getInstance();
            Connection conn = dbManager.getConnection();

            conn.setAutoCommit(false);

            try {
                // Import books from Data.csv
                importBooks(conn, TEMP_DIR + "Data.csv");
                // Also check if Libri.csv has additional books
                importAdditionalBooks(conn, TEMP_DIR + "Libri.csv");

                // Import users
                importUsers(conn);

                // Import libraries
                importLibraries(conn);

                // Import ratings
                importRatings(conn);

                // Import recommendations (if available)
                File recFile = new File(TEMP_DIR + "ConsigliLibri.dati.csv");
                if (recFile.exists() && recFile.length() > 0) {
                    importRecommendations(conn);
                }

                conn.commit();
                addLogMessage("Database populated successfully", LogType.SUCCESS);

            } catch (Exception e) {
                conn.rollback();
                addLogMessage("Error populating database: " + e.getMessage(), LogType.ERROR);
                throw e;
            }
        } catch (SQLException e) {
            addLogMessage("Error connecting to database: " + e.getMessage(), LogType.ERROR);
            throw e;
        }
    }

    private void importBooks(Connection conn, String filePath) throws SQLException, IOException {
        addLogMessage("Importing books from Data.csv...", LogType.INFO);

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
                    String[] fields = parseCsvLine(line);
                    if (fields.length >= 5) {
                        pstmt.setString(1, fields[0].trim()); // Title
                        pstmt.setString(2, fields[1].trim()); // Authors
                        pstmt.setString(3, fields[2].trim()); // Category
                        pstmt.setString(4, fields[3].trim()); // Publisher

                        // Publish Date (Year)
                        try {
                            float yearFloat = Float.parseFloat(fields[4].trim());
                            pstmt.setInt(5, (int) yearFloat);
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

        addLogMessage("Imported books: Success=" + successCount + ", Errors=" + errorCount, LogType.INFO);
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
            addLogMessage("Errore durante la verifica dell'installazione di PostgreSQL: " + e.getMessage(), LogType.ERROR);
            return false;
        }
    }

    /**
     * Installa PostgreSQL se non è già installato
     * @return true se l'installazione ha avuto successo o PostgreSQL è già installato, false altrimenti
     */
    private boolean installPostgresIfNeeded() {
        if (isPostgresInstalled()) {
            addLogMessage("PostgreSQL è già installato", LogType.INFO);
            return true;
        }

        addLogMessage("PostgreSQL non è installato. Tentativo di installazione...", LogType.WARNING);

        try {
            // Il comando di installazione dipende dal sistema operativo

            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("win")) {
                // Windows - apre una pagina di download per Windows
                addLogMessage("Apertura pagina di download di PostgreSQL per Windows...", LogType.INFO);
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
                // macOS - usa Homebrew se disponibile
                addLogMessage("Tentativo di installazione su macOS usando Homebrew...", LogType.INFO);

                // Verifica se Homebrew è installato
                Process brewCheck = Runtime.getRuntime().exec("which brew");
                if (brewCheck.waitFor() == 0) {
                    Process process = Runtime.getRuntime().exec("brew install postgresql");
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        addLogMessage("PostgreSQL installato con successo tramite Homebrew", LogType.SUCCESS);
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
                // Linux - assume un sistema basato su Debian/Ubuntu
                addLogMessage("Tentativo di installazione su Linux...", LogType.INFO);

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
                        addLogMessage("PostgreSQL installato con successo", LogType.SUCCESS);
                        return true;
                    }
                }
            }

            addLogMessage("Impossibile installare PostgreSQL automaticamente", LogType.ERROR);
            return false;
        } catch (Exception e) {
            addLogMessage("Errore durante l'installazione di PostgreSQL: " + e.getMessage(), LogType.ERROR);
            return false;
        }
    }

    private boolean startPostgresIfNeeded() {
        if (isPostgresRunning()) {
            addLogMessage("PostgreSQL è già in esecuzione", LogType.INFO);
            return true;
        }

        addLogMessage("PostgreSQL non è in esecuzione. Tentativo di avvio...", LogType.WARNING);

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
                        addLogMessage("Rilevato Mac con Apple Silicon", LogType.INFO);
                    } else {
                        addLogMessage("Rilevato Mac con Intel", LogType.INFO);
                    }
                } catch (Exception e) {
                    // In caso di errore, prova entrambi i tipi di comandi
                    addLogMessage("Impossibile rilevare l'architettura, verranno provati tutti i comandi", LogType.WARNING);
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
                    addLogMessage("Tentativo di avvio con comando: " + command, LogType.INFO);
                    try {
                        Process process = Runtime.getRuntime().exec(command);
                        process.waitFor(5, TimeUnit.SECONDS);

                        // Attendi e verifica se PostgreSQL è partito
                        Thread.sleep(3000);
                        if (isPostgresRunning()) {
                            addLogMessage("PostgreSQL avviato con successo usando: " + command, LogType.SUCCESS);
                            success = true;
                            break;
                        }
                    } catch (Exception e) {
                        addLogMessage("Comando fallito: " + e.getMessage(), LogType.WARNING);
                    }
                }

                // Se tutti i comandi falliscono, prova un approccio alternativo
                if (!success) {
                    addLogMessage("Tentativo di avvio tramite comando specifico per la nostra installazione...", LogType.INFO);
                    try {
                        // Comando specifico basato sul percorso dove hai installato PostgreSQL
                        Process process = Runtime.getRuntime().exec("pg_ctl -D /opt/homebrew/var/postgresql@14 start");
                        process.waitFor(10, TimeUnit.SECONDS);

                        // Leggi l'uscita del processo
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            addLogMessage("Output: " + line, LogType.INFO);
                        }

                        // Attendi un po' più a lungo
                        Thread.sleep(5000);
                        if (isPostgresRunning()) {
                            addLogMessage("PostgreSQL avviato con successo usando comando personalizzato", LogType.SUCCESS);
                            success = true;
                        }
                    } catch (Exception e) {
                        addLogMessage("Anche il comando personalizzato è fallito: " + e.getMessage(), LogType.WARNING);
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
                    addLogMessage("Tentativo di avvio con comando: " + command, LogType.INFO);
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
                            addLogMessage("PostgreSQL avviato con successo usando: " + command, LogType.SUCCESS);
                            success = true;
                            break;
                        }
                    } catch (Exception e) {
                        addLogMessage("Comando fallito: " + e.getMessage(), LogType.WARNING);
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
                            addLogMessage("PostgreSQL avviato con successo usando: " + command, LogType.SUCCESS);
                            success = true;
                            break;
                        }
                    } catch (Exception e) {
                        addLogMessage("Comando fallito: " + e.getMessage(), LogType.WARNING);
                    }
                }
            }

            // Se tutti i tentativi falliscono, mostra un avviso ma continua comunque
            if (!success) {
                addLogMessage("Impossibile avviare PostgreSQL automaticamente", LogType.WARNING);

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
            addLogMessage("Errore durante il tentativo di avvio di PostgreSQL: " + e.getMessage(), LogType.ERROR);
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
            addLogMessage("Libri.csv is empty or does not exist. Skipping...", LogType.WARNING);
            return;
        }

        addLogMessage("Importing additional books from Libri.csv...", LogType.INFO);

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

        addLogMessage("Imported additional books: Success=" + successCount + ", Errors=" + errorCount, LogType.INFO);
    }

    private void importUsers(Connection conn) throws SQLException, IOException {
        addLogMessage("Importing users...", LogType.INFO);

        String sql = "INSERT INTO users (user_id, full_name, fiscal_code, email, password) " +
                "VALUES (?, ?, ?, ?, ?) ON CONFLICT (user_id) DO NOTHING";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader reader = new BufferedReader(new FileReader(TEMP_DIR + "UtentiRegistrati.csv"))) {

            String line;

            int userCount = 0;
            while ((line = reader.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                if (fields.length >= 5) {
                    String userId = fields[3].trim().replaceAll("^\"|\"$", "");
                    String fullName = fields[0].trim().replaceAll("^\"|\"$", "");
                    String fiscalCode = fields[1].trim().replaceAll("^\"|\"$", "");
                    String email = fields[2].trim().replaceAll("^\"|\"$", "");
                    String password = fields[4].trim().replaceAll("^\"|\"$", "");

                    pstmt.setString(1, userId);
                    pstmt.setString(2, fullName);
                    pstmt.setString(3, fiscalCode);
                    pstmt.setString(4, email);
                    pstmt.setString(5, password);
                    pstmt.executeUpdate();
                    userCount++;
                }
            }
            addLogMessage("Users imported successfully. Total: " + userCount, LogType.SUCCESS);
        }
    }

    private void importLibraries(Connection conn) throws SQLException, IOException {
        addLogMessage("Importing libraries...", LogType.INFO);

        String insertLibrarySql = "INSERT INTO libraries (user_id, library_name) " +
                "VALUES (?, ?) ON CONFLICT (user_id, library_name) DO NOTHING " +
                "RETURNING id";

        String insertLibraryBookSql = "INSERT INTO library_books (library_id, book_id) " +
                "VALUES (?, ?) ON CONFLICT DO NOTHING";

        String findBookSql = "SELECT id FROM books WHERE title = ?";

        int libraryCount = 0;
        int bookCount = 0;

        try (PreparedStatement pstmtLib = conn.prepareStatement(insertLibrarySql);
             PreparedStatement pstmtLibBook = conn.prepareStatement(insertLibraryBookSql);
             PreparedStatement pstmtFindBook = conn.prepareStatement(findBookSql);
             BufferedReader reader = new BufferedReader(new FileReader(TEMP_DIR + "Librerie.dati.csv"))) {

            String line;

            while ((line = reader.readLine()) != null) {
                String[] fields = parseCsvLine(line);

                if (fields.length >= 2) {
                    String userId = fields[0].trim().replaceAll("^\"|\"$", "");
                    String libraryName = fields[1].trim().replaceAll("^\"|\"$", "");

                    // Check if user exists
                    String checkUserSql = "SELECT 1 FROM users WHERE user_id = ?";
                    try (PreparedStatement pstmtCheckUser = conn.prepareStatement(checkUserSql)) {
                        pstmtCheckUser.setString(1, userId);
                        ResultSet userRs = pstmtCheckUser.executeQuery();

                        if (!userRs.next()) {
                            continue;
                        }
                    }

                    // Insert library
                    pstmtLib.setString(1, userId);
                    pstmtLib.setString(2, libraryName);

                    ResultSet rs = pstmtLib.executeQuery();
                    if (rs.next()) {
                        int libraryId = rs.getInt(1);
                        libraryCount++;

                        // Add books to a library (remaining fields are book titles)
                        for (int i = 2; i < fields.length && i < 12; i++) {
                            String bookField = fields[i].trim().replaceAll("^\"|\"$", "");
                            if (!bookField.equals("null") && !bookField.isEmpty()) {
                                String bookTitle = bookField.replace("_", " ");

                                pstmtFindBook.setString(1, bookTitle);
                                ResultSet bookRs = pstmtFindBook.executeQuery();

                                if (bookRs.next()) {
                                    int bookId = bookRs.getInt(1);
                                    pstmtLibBook.setInt(1, libraryId);
                                    pstmtLibBook.setInt(2, bookId);
                                    pstmtLibBook.executeUpdate();
                                    bookCount++;
                                }
                            }
                        }
                    }
                }
            }

            addLogMessage("Libraries imported: " + libraryCount + " libraries with " + bookCount + " books", LogType.SUCCESS);
        } catch (Exception e) {
            addLogMessage("Error importing libraries: " + e.getMessage(), LogType.ERROR);
            throw e;
        }
    }

    private void importRatings(Connection conn) throws SQLException, IOException {
        addLogMessage("Importing book ratings...", LogType.INFO);

        String findBookSql = "SELECT id FROM books WHERE title = ?";
        String insertRatingSql = "INSERT INTO book_ratings (user_id, book_id, style_rating, " +
                "content_rating, pleasantness_rating, originality_rating, " +
                "edition_rating, average_rating, general_comment, style_comment, content_comment, " +
                "pleasantness_comment, originality_comment, edition_comment) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (user_id, book_id) DO NOTHING";

        int ratingCount = 0;

        try (PreparedStatement pstmtFindBook = conn.prepareStatement(findBookSql);
             PreparedStatement pstmtRating = conn.prepareStatement(insertRatingSql);
             BufferedReader reader = new BufferedReader(new FileReader(TEMP_DIR + "ValutazioniLibri.csv"))) {

            String line = reader.readLine(); // Read header

            while ((line = reader.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                if (fields.length >= 8) {
                    String userId = fields[0].trim();      // userid
                    String bookTitle = fields[1].trim();   // titoloLibro

                    // Find book ID
                    pstmtFindBook.setString(1, bookTitle);
                    ResultSet bookRs = pstmtFindBook.executeQuery();

                    if (bookRs.next()) {
                        int bookId = bookRs.getInt(1);

                        pstmtRating.setString(1, userId);
                        pstmtRating.setInt(2, bookId);
                        pstmtRating.setInt(3, Integer.parseInt(fields[2].trim())); // stile
                        pstmtRating.setInt(4, Integer.parseInt(fields[3].trim())); // contenuto
                        pstmtRating.setInt(5, Integer.parseInt(fields[4].trim())); // gradevolezza
                        pstmtRating.setInt(6, Integer.parseInt(fields[5].trim())); // originalita
                        pstmtRating.setInt(7, Integer.parseInt(fields[6].trim())); // edizione
                        pstmtRating.setFloat(8, Float.parseFloat(fields[7].trim())); // media

                        // Comments
                        pstmtRating.setString(9, fields.length > 8 ? fields[8].trim() : null);  // recensione
                        pstmtRating.setString(10, fields.length > 9 ? fields[9].trim() : null); // commentoStile
                        pstmtRating.setString(11, fields.length > 10 ? fields[10].trim() : null); // commentoContenuto
                        pstmtRating.setString(12, fields.length > 11 ? fields[11].trim() : null); // commentoGradevolezza
                        pstmtRating.setString(13, fields.length > 12 ? fields[12].trim() : null); // commentoOriginalita
                        pstmtRating.setString(14, fields.length > 13 ? fields[13].trim() : null); // commentoEdizione

                        pstmtRating.executeUpdate();
                        ratingCount++;
                    }
                }
            }

            addLogMessage("Ratings imported successfully. Total: " + ratingCount, LogType.SUCCESS);
        }
    }

    private void importRecommendations(Connection conn) throws SQLException, IOException {
        addLogMessage("Checking for recommendations data...", LogType.INFO);

        // First, check if ConsigliLibri.dati.csv exists and has content
        File file = new File(TEMP_DIR + "ConsigliLibri.dati.csv");
        if (!file.exists() || file.length() == 0) {
            addLogMessage("ConsigliLibri.dati.csv is empty or does not exist. Skipping...", LogType.WARNING);
            return;
        }

        addLogMessage("Importing recommendations...", LogType.INFO);

        // Implementation could be added here based on file structure

        addLogMessage("Recommendations import - structure not defined, skipping", LogType.WARNING);
    }

    private void startSocketServer() {
        int[] portsToTry = {8888, 8889, 8890, 8891, 8892};
        boolean success = false;

        for (int port : portsToTry) {
            try {
                // Create a server socket that binds to all network interfaces
                serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
                addLogMessage("Server socket created on port " + port + " (accessible from network)", LogType.SUCCESS);
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
                                addLogMessage("Error accepting client connection: " + e.getMessage(), LogType.ERROR);
                            }
                        }
                    }
                }).start();

                break; // Exit the loop if successful

            } catch (IOException e) {
                addLogMessage("Failed to bind to port " + port + ": " + e.getMessage(), LogType.WARNING);
                // Continue to try the next port
            }
        }

        if (!success) {
            String errorMsg = "Could not bind to any port. Tried ports: " + Arrays.toString(portsToTry);
            addLogMessage(errorMsg, LogType.ERROR);
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
            addLogMessage("Error handling client: " + e.getMessage(), LogType.ERROR);
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

            addLogMessage("Client disconnected", LogType.INFO);
        }
    }
    /**
     * Notifies all connected clients about server shutdown and initiates server shutdown
     */
    public void notifyAllClientsAndShutdown() {
        if (!serverRunning) return;

        addLogMessage("Notifying all clients of server shutdown...", LogType.INFO);

        // Notify all connected clients
        synchronized(connectedClientSockets) {
            for (Socket clientSocket : connectedClientSockets) {
                try {
                    if (!clientSocket.isClosed()) {
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println("SERVER_SHUTDOWN");
                    }
                } catch (IOException e) {
                    // Ignore errors during shutdown
                    addLogMessage("Error notifying client: " + e.getMessage(), LogType.WARNING);
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
                addLogMessage("Error closing server socket: " + e.getMessage(), LogType.ERROR);
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

        addLogMessage("Server stopped and all clients notified", LogType.SUCCESS);
    }
    private String[] parseCsvLine(String line) {
        // First, try to split by tabs if the line contains tabs
        if (line.contains("\t")) {
            return line.split("\t");
        }

        // Otherwise, use the standard CSV parsing logic
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        fields.add(currentField.toString());
        return fields.toArray(new String[0]);
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

    private void addLogMessage(String message, LogType logType) {
        String timestamp = getCurrentTimestamp();
        String logEntry = "[" + timestamp + "] [" + logType.name() + "] " + message;

        // Output to console instead of UI
        System.out.println(logEntry);

        // If for some reason we need to keep UI updates (e.g., for future reference)
        // but the logContainer doesn't exist in the FXML, we can just skip that part
    }
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    private enum LogType {
        INFO(Color.BLUE),
        SUCCESS(Color.GREEN),
        WARNING(Color.ORANGE),
        ERROR(Color.RED);

        private final Color color;

        LogType(Color color) {
            this.color = color;
        }


    }
}