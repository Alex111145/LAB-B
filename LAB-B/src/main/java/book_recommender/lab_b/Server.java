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

/**
 * Classe principale dell'applicazione server del sistema Book Recommender.
 * Gestisce l'avvio del server, la pulizia delle risorse durante la chiusura
 * e fornisce funzionalità per il rilevamento di istanze server già in esecuzione.
 */
public class Server extends Application {

    private ServerInterfaceController controller;
    private NgrokManager ngrokManager;

    /**
     * Metodo principale per l'avvio dell'applicazione JavaFX.
     * Configura l'interfaccia utente, inizializza i componenti necessari
     * e avvia automaticamente il server.
     *
     * @param primaryStage Lo stage principale dell'applicazione JavaFX
     * @throws Exception Se si verifica un errore durante l'inizializzazione
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Carica il file FXML che definisce l'interfaccia utente
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/server_interface.fxml"));
        Parent root = loader.load();

        // Ottiene un riferimento al controller dell'interfaccia
        controller = loader.getController();

        // Inizializza il gestore per il tunnel ngrok, che permette l'accesso remoto al database
        ngrokManager = new NgrokManager();

        // Registra un hook di spegnimento per eseguire pulizia quando l'applicazione termina
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Pulisce il database tramite il controller, se disponibile
            if (controller != null) {
                controller.cleanupDatabaseAndShutdown();
            }

            // Esegue direttamente la pulizia dei file temporanei come precauzione aggiuntiva
            deleteDownloadedFiles();

            // Arresta il tunnel ngrok quando l'applicazione si chiude
            if (ngrokManager != null) {
                ngrokManager.stopTunnel();
            }
        }));

        // Crea e configura la scena principale
        Scene scene = new Scene(root, 800, 600);

        // Configura lo stage principale
        primaryStage.setTitle("Book Recommender Server");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(550);

        // Gestisce l'evento di chiusura della finestra mostrando un dialogo di conferma
        primaryStage.setOnCloseRequest(event -> {
            // Previene la chiusura immediata - la gestiremo dopo la conferma dell'utente
            event.consume();

            // Mostra il dialogo di conferma per lo spegnimento
            showShutdownConfirmationDialog(primaryStage);
        });

        // Mostra la finestra principale
        primaryStage.show();

        // Avvia automaticamente il server all'avvio dell'applicazione
        controller.onStartServer(null);
    }

    /**
     * Mostra un dialogo di conferma prima di spegnere il server.
     * Se l'utente conferma, esegue la pulizia e termina l'applicazione.
     *
     * @param primaryStage Lo stage principale per l'allineamento del dialogo
     */
    private void showShutdownConfirmationDialog(Stage primaryStage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Spegni Server");
        alert.setHeaderText("Spegnimento del Server in corso...");
        alert.setContentText("Sei sicuro di voler arrestare il Server? \nTutti gli utenti collegati verranno immediatamente scollegati.");

        // Personalizza i pulsanti del dialogo
        ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("OK");
        ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Annulla");

        // Gestisce il risultato del dialogo
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // L'utente ha confermato, procede con lo spegnimento
                if (controller != null) {
                    controller.cleanupDatabaseAndShutdown();

                    // Esegue pulizia diretta come ulteriore verifica
                    deleteDownloadedFiles();
                }

                // Arresta il tunnel ngrok
                if (ngrokManager != null) {
                    ngrokManager.stopTunnel();
                }

                // Chiude l'applicazione
                Platform.exit();
            }
            // Se l'utente annulla, la finestra rimane aperta
        });
    }

    /**
     * Elimina tutti i file scaricati dalla directory temporanea quando il server si spegne.
     * Metodo migliorato per gestire in modo robusto la cancellazione dei file.
     */
    private void deleteDownloadedFiles() {
        File tempDir = new File("temp_data");
        if (tempDir.exists() && tempDir.isDirectory()) {
            // Prima elimina tutti i file nella directory
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    try {
                        if (file.isFile()) {
                            boolean deleted = file.delete();
                            if (!deleted) {
                                // Se non è possibile eliminare immediatamente, pianifica l'eliminazione all'uscita
                                file.deleteOnExit();
                            }
                        } else if (file.isDirectory()) {
                            // Gestione ricorsiva delle sottodirectory
                            deleteDirectory(file);
                        }
                    } catch (Exception e) {
                        // In caso di eccezione durante l'eliminazione, pianifica l'eliminazione all'uscita
                        file.deleteOnExit();
                    }
                }
            }

            // Prova a eliminare la directory stessa
            try {
                boolean dirDeleted = tempDir.delete();
                if (!dirDeleted) {
                    // Se la directory non può essere eliminata, prova a forzare il garbage collector
                    // per rilasciare eventuali handle di file ancora aperti
                    System.gc();
                    try {
                        // Attendi un po' per dare tempo al GC di completare
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }

                    // Riprova l'eliminazione dopo il GC
                    dirDeleted = tempDir.delete();
                    if (!dirDeleted) {
                        // Se ancora non è possibile, pianifica l'eliminazione all'uscita
                        tempDir.deleteOnExit();
                    }
                }
            } catch (Exception e) {
                // In caso di eccezione, pianifica l'eliminazione all'uscita
                tempDir.deleteOnExit();
            }
        }
    }

    /**
     * Metodo ausiliario per eliminare ricorsivamente una directory e tutto il suo contenuto.
     *
     * @param directory La directory da eliminare
     * @return true se l'eliminazione è riuscita, false altrimenti
     */
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Ricorsione per le sottodirectory
                        deleteDirectory(file);
                    } else {
                        // Elimina i file
                        boolean deleted = file.delete();
                        if (!deleted) {
                            file.deleteOnExit();
                        }
                    }
                }
            }
        }
        // Elimina la directory vuota
        boolean deleted = directory.delete();
        if (!deleted) {
            directory.deleteOnExit();
        }
        return deleted;
    }

    /**
     * Verifica se un altro server è già in esecuzione nella rete.
     * Controlla se l'host è raggiungibile e se il database contiene già le tabelle necessarie.
     *
     * @param dbUrl URL JDBC da controllare
     * @return true se un altro server è attivo, false altrimenti
     */
    public static boolean isAnotherServerRunning(String dbUrl) {
        // Estrae l'host dall'URL JDBC
        String host = "localhost"; // Valore predefinito

        // Estrae il nome host dall'URL JDBC
        if (dbUrl.contains("//")) {
            String[] parts = dbUrl.split("//")[1].split("/");
            if (parts.length > 0) {
                String[] hostParts = parts[0].split(":");
                host = hostParts[0];
            }
        }

        try {
            // Prova a raggiungere l'host
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(1000); // Timeout di 1 secondo

            if (reachable) {
                try (Connection conn = DriverManager.getConnection(dbUrl, "book_admin_8530", "CPuc#@r-zbKY")) {
                    // Connessione riuscita, verifica se esistono le tabelle
                    DatabaseMetaData meta = conn.getMetaData();
                    ResultSet tables = meta.getTables(null, null, "users", null);
                    return tables.next(); // Restituisce true se esiste la tabella "users"
                } catch (SQLException e) {
                    // Errore di connessione o tabella non trovata
                    return false;
                }
            }
            // Host non raggiungibile
            return false;
        } catch (IOException e) {
            // Errore durante il tentativo di raggiungere l'host
            return false;
        }
    }

    /**
     * Punto di ingresso principale dell'applicazione.
     * Avvia l'applicazione JavaFX.
     *
     * @param args Argomenti della riga di comando (non utilizzati)
     */
    public static void main(String[] args) {
        launch(args);
    }
}