package book_recommender.lab_b;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

        // Aggiungi un hook di shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (controller != null) {
                controller.cleanupDatabaseAndShutdown();
                deleteDownloadedFiles();

                // Assicurati di arrestare il tunnel ngrok quando l'applicazione si chiude
                if (ngrokManager != null) {
                    ngrokManager.stopTunnel();
                }
            }
        }));

        Scene scene = new Scene(root, 800, 600);

        // Set up stage
        primaryStage.setTitle("Book Recommender Server");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(550);

        // Handle window close event
        primaryStage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.cleanupDatabaseAndShutdown();
                deleteDownloadedFiles();

                // Arresta il tunnel ngrok quando l'utente chiude la finestra
                if (ngrokManager != null) {
                    ngrokManager.stopTunnel();
                }
            }
        });

        // Show the window
        primaryStage.show();

        // Automatically start the server when launched
        controller.onStartServer(null);
    }

    /**
     * Delete all downloaded files from temp directory when server shuts down
     */
    private void deleteDownloadedFiles() {
        File tempDir = new File("temp_data");
        if (tempDir.exists() && tempDir.isDirectory()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        boolean deleted = file.delete();
                        if (!deleted) {
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
        }
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