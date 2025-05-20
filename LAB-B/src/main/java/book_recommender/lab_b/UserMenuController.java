package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller per la schermata del menu utente principale.
 * Gestisce la navigazione verso le varie funzionalità dell'applicazione
 * come creazione librerie, aggiunta libri, valutazione e consigli.
 * Fornisce anche il meccanismo di logout.
 */
public class UserMenuController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;

    private String userId;
    private DatabaseManager dbManager;

    /**
     * Inizializza il controller dopo che il file FXML è stato caricato.
     * Configura lo stato iniziale dell'interfaccia e inizializza la connessione al database.
     * Questo metodo viene chiamato automaticamente da JavaFX.
     */
    public void initialize() {
        // Pulisce eventuali messaggi di stato precedenti
        statusLabel.setText("");

        // Inizializza la connessione al database manager
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Gestione silenziosa dell'errore - verrà gestito nei metodi che usano il database
        }
    }

    /**
     * Imposta l'ID utente nel controller e aggiorna il messaggio di benvenuto.
     * Questo metodo deve essere chiamato dopo il caricamento della vista
     * per fornire i dati dell'utente autenticato.
     *
     * @param userId ID dell'utente che ha effettuato l'accesso
     */
    public void setUserData(String userId) {
        this.userId = userId;
        welcomeLabel.setText("Ciao, " + userId + "!");
    }

    /**
     * Imposta un messaggio di stato nell'interfaccia utente.
     * Utilizzato per mostrare feedback all'utente come conferme di operazioni
     * o messaggi di benvenuto dopo la registrazione.
     *
     * @param message Messaggio da visualizzare nella label di stato
     */
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    /**
     * Gestisce il click sul pulsante "Logout".
     * Rimuove l'utente dalla lista degli utenti connessi e torna alla homepage.
     *
     * @param event Evento di azione generato dal pulsante
     */
    @FXML
    public void onLogoutClicked(ActionEvent event) {
        try {
            // Rimuove l'utente dalla lista degli utenti connessi nel database
            unregisterUserConnection();

            // Naviga alla homepage
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/homepage.fxml"));
            Parent root = loader.load();

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            // Mostra un messaggio di errore in caso di problemi con il caricamento della homepage
            statusLabel.setText("Errore: Impossibile tornare alla homepage");
        }
    }

    /**
     * Rimuove l'utente dalla tabella degli utenti connessi nel database.
     * Questo metodo viene chiamato durante il logout per garantire che
     * l'utente venga correttamente disconnesso dal sistema.
     */
    private void unregisterUserConnection() {
        try {
            if (dbManager != null && userId != null) {
                // Crea un pattern per cercare tutti i client IDs associati all'utente corrente
                String clientIdPattern = "user_" + userId + "_%";

                // Esegue una query SQL per eliminare tutte le connessioni dell'utente
                String sql = "DELETE FROM active_clients WHERE client_id LIKE ?";

                java.sql.Connection conn = dbManager.getConnection();
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, clientIdPattern);
                pstmt.executeUpdate();
                pstmt.close();
            }
        } catch (SQLException e) {
            // Gestione silenziosa dell'errore - l'utente può comunque essere disconnesso
        }
    }

    /**
     * Gestisce il click sul pulsante "Crea Libreria".
     * Naviga alla schermata di creazione libreria e passa i dati dell'utente.
     *
     * @param event Evento di azione generato dal pulsante
     */
    @FXML
    public void onCreateLibraryClicked(ActionEvent event) {
        try {
            // Carica la pagina di creazione libreria
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/crealibreria.fxml"));
            Parent root = loader.load();

            // Ottiene il controller e passa l'ID utente
            CreateLibraryController controller = loader.getController();
            controller.setUserId(userId);

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            // Mostra un messaggio di errore in caso di problemi
            statusLabel.setText("Errore: Impossibile aprire la pagina di creazione libreria");
        }
    }

    /**
     * Gestisce il click sul pulsante "Aggiungi Libro alla Libreria".
     * Naviga alla schermata di selezione libreria con modalità di aggiunta libri.
     *
     * @param event Evento di azione generato dal pulsante
     */
    @FXML
    public void onAddBookClicked(ActionEvent event) {
        try {
            // Definisce il percorso del file FXML
            String fxmlFile = "/book_recommender/lab_b/selezionalib.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));

            // Verifica che il file FXML esista
            if (getClass().getResource(fxmlFile) == null) {
                throw new IOException("File FXML non trovato: " + fxmlFile);
            }

            Parent root = loader.load();

            // Ottiene il controller e imposta la modalità "add" (aggiunta libri)
            LibrarySelectionController controller = loader.getController();
            controller.setUserId(userId, "add");

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            // Mostra un messaggio di errore in caso di problemi
            statusLabel.setText("Errore: Impossibile aprire la pagina di selezione libreria");
        }
    }

    /**
     * Gestisce il click sul pulsante "Valuta Libro".
     * Naviga alla schermata di selezione libreria con modalità valutazione.
     *
     * @param event Evento di azione generato dal pulsante
     */
    @FXML
    public void onRateBookClicked(ActionEvent event) {
        try {
            // Definisce il percorso del file FXML
            String fxmlFile = "/book_recommender/lab_b/selezionalib.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));

            // Verifica che il file FXML esista
            if (getClass().getResource(fxmlFile) == null) {
                throw new IOException("File FXML non trovato: " + fxmlFile);
            }

            Parent root = loader.load();

            // Ottiene il controller e imposta la modalità "rate" (valutazione)
            LibrarySelectionController controller = loader.getController();
            controller.setUserId(userId, "rate");

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            // Mostra un messaggio di errore in caso di problemi
            statusLabel.setText("Errore: Impossibile aprire la pagina di selezione libreria");
        }
    }

    /**
     * Gestisce il click sul pulsante "Consiglia Libro".
     * Naviga alla schermata di selezione libreria con modalità consiglio.
     *
     * @param event Evento di azione generato dal pulsante
     */
    @FXML
    public void onRecommendBookClicked(ActionEvent event) {
        try {
            // Definisce il percorso del file FXML
            String fxmlFile = "/book_recommender/lab_b/selezionalib.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));

            // Verifica che il file FXML esista
            if (getClass().getResource(fxmlFile) == null) {
                throw new IOException("File FXML non trovato: " + fxmlFile);
            }

            Parent root = loader.load();

            // Ottiene il controller e imposta la modalità "recommend" (consiglio)
            LibrarySelectionController controller = loader.getController();
            controller.setUserId(userId, "recommend");

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            // Mostra un messaggio di errore in caso di problemi
            statusLabel.setText("Errore: Impossibile aprire la pagina di selezione libreria");
        }
    }
}