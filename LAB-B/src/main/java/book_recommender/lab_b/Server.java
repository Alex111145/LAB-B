package book_recommender.lab_b;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.*;

public class Server extends Application {

    private ServerInterfaceController controller;
    private NgrokManager ngrokManager;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/server_interface.fxml"));
        Parent root = loader.load();

        // Get controller reference
        controller = loader.getController();

        // Inizializza NgrokManager
        ngrokManager = new NgrokManager();

        // Aggiungi un hook di shutdown migliorato
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          if (controller != null) {
                controller.cleanupDatabaseAndShutdown();
            }

            // Esecuzione diretta del cleanup dei file come ulteriore precauzione
            deleteDownloadedFiles();

            // Assicurati di arrestare il tunnel ngrok quando l'applicazione si chiude
            if (ngrokManager != null) {
                ngrokManager.stopTunnel();
            }

       }));

        Scene scene = new Scene(root, 800, 600);

        // Set up stage
        primaryStage.setTitle("Book Recommender Server");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(550);

        // Handle window close event con più controlli
        primaryStage.setOnCloseRequest(event -> {

            // Prevent immediate closing - we'll handle it after confirmation
            event.consume();

            // Show the same confirmation dialog as in onStopServer
            showShutdownConfirmationDialog(primaryStage);
        });

        // Show the window
        primaryStage.show();

        // Automatically start the server when launched
        controller.onStartServer(null);
    }

    /**
     * Shows a confirmation dialog before shutting down the server
     */
    private void showShutdownConfirmationDialog(Stage primaryStage) {
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
                if (controller != null) {
                    controller.cleanupDatabaseAndShutdown();

                    // Cleanup diretto come ulteriore verifica
                    deleteDownloadedFiles();
                }

                // Arresta il tunnel ngrok
                if (ngrokManager != null) {
                    ngrokManager.stopTunnel();
                }


                // Close the application
                Platform.exit();
            }
            // If cancel, do nothing and the window will stay open
        });
    }

    /**
     * Delete all downloaded files from temp directory when server shuts down
     * Metodo migliorato per la cancellazione più robusta dei file temporanei
     */
    private void deleteDownloadedFiles() {
       File tempDir = new File("temp_data");
        if (tempDir.exists() && tempDir.isDirectory()) {
            // First, delete all files in the directory
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    try {
                        if (file.isFile()) {
                            boolean deleted = file.delete();
                            if (!deleted) {
                              file.deleteOnExit();
                            }
                        } else if (file.isDirectory()) {
                            // Gestione delle sottodirectory
                            deleteDirectory(file);
                        }
                    } catch (Exception e) {
                      file.deleteOnExit();
                    }
                }
            }

            // Try to delete the directory itself
            try {
                boolean dirDeleted = tempDir.delete();
                if (!dirDeleted) {


                    System.gc();
                    try {
                        Thread.sleep(200); // Give a little time for GC
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }

                    dirDeleted = tempDir.delete();
                    if (!dirDeleted) {
                        // If still can't delete, mark for deletion on exit
                        tempDir.deleteOnExit();
                    }
                }
            } catch (Exception e) {
              tempDir.deleteOnExit();
            }
        }
    }

    /**
     * Helper method to delete a directory and all its contents recursively
     */
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
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
     * Checks if another server is already running on the network
     * @param dbUrl JDBC URL to check
     * @return true if another server is active
     */
    public static boolean isAnotherServerRunning(String dbUrl) {
        // Parse host from JDBC URL
        String host = "localhost"; // Default

        // Extract hostname from JDBC URL
        if (dbUrl.contains("//")) {
            String[] parts = dbUrl.split("//")[1].split("/");
            if (parts.length > 0) {
                String[] hostParts = parts[0].split(":");
                host = hostParts[0];
            }
        }

        try {
            // Try to reach the host
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(1000); // 1 second timeout

            if (reachable) {
                try (Connection conn = DriverManager.getConnection(dbUrl, "book_admin_8530", "CPuc#@r-zbKY")) {
                    // Successfully connected, check if tables exist
                    DatabaseMetaData meta = conn.getMetaData();
                    ResultSet tables = meta.getTables(null, null, "users", null);
                    return tables.next();
                } catch (SQLException e) {
                    return false;
                }
            }
            return false;
        } catch (IOException e) {
            return false; // Host not reachable
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}