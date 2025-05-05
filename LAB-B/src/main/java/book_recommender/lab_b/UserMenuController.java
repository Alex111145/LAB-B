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

public class UserMenuController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label statusLabel;

    private String userId;
    private DatabaseManager dbManager;

    /**
     * Inizializza il controller.
     * Questo metodo viene chiamato automaticamente da JavaFX dopo che l'FXML è stato caricato
     */
    public void initialize() {
        // Le inizializzazioni che non dipendono da dati esterni vanno qui
        statusLabel.setText("");

        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
        }
    }

    /**
     * Imposta i dati dell'utente e aggiorna l'interfaccia.
     * Questo metodo deve essere chiamato dopo il caricamento del controller
     * per impostare i dati specifici dell'utente.
     *
     * @param userId l'ID dell'utente che ha effettuato l'accesso
     */
    public void setUserData(String userId) {
        this.userId = userId;
        welcomeLabel.setText("Ciao, " + userId + "!");
    }

    /**
     * Imposta un messaggio di stato nell'interfaccia.
     *
     * @param message il messaggio da visualizzare
     */
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    /**
     * Gestisce il clic sul pulsante "Logout"
     */
    @FXML
    public void onLogoutClicked(ActionEvent event) {
        try {
            // Rimuove l'utente dalla lista degli utenti connessi
            unregisterUserConnection();

            // Torna alla homepage
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/homepage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            System.err.println("Errore nel caricamento della homepage: " + e.getMessage());

            statusLabel.setText("Errore: Impossibile tornare alla homepage");
        }
    }

    /**
     * Rimuove l'utente dalla tabella degli utenti connessi
     */
    private void unregisterUserConnection() {
        try {
            if (dbManager != null && userId != null) {
                // Cerca tutti i client IDs che contengono questo userID
                String clientIdPattern = "user_" + userId + "_%";

                // Rimuove l'utente dalla lista degli utenti connessi usando una query speciale
                String sql = "DELETE FROM active_clients WHERE client_id LIKE ?";

                java.sql.Connection conn = dbManager.getConnection();
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, clientIdPattern);
                pstmt.executeUpdate();
                pstmt.close();

                System.out.println("Utente " + userId + " disconnesso");
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la disconnessione dell'utente: " + e.getMessage());
        }
    }

    /**
     * Gestisce il clic sul pulsante "Crea Libreria"
     */
    @FXML
    public void onCreateLibraryClicked(ActionEvent event) {
        try {
            // Carica la pagina di creazione libreria
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/crealibreria.fxml"));
            Parent root = loader.load();

            // Ottieni il controller e passa l'ID utente
            CreateLibraryController controller = loader.getController();
            controller.setUserId(userId);

            // Imposta la nuova scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Errore nel caricamento della pagina di creazione libreria: " + e.getMessage());

            statusLabel.setText("Errore: Impossibile aprire la pagina di creazione libreria");
        }
    }

    /**
     * Gestisce il clic sul pulsante "Aggiungi Libro alla Libreria"
     * Naviga alla schermata di selezione della libreria con modalità di aggiunta libri
     */
    @FXML
    public void onAddBookClicked(ActionEvent event) {
        try {
            // Carica la pagina di selezione libreria
            String fxmlFile = "/book_recommender/lab_b/selezionalib.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));

            if (getClass().getResource(fxmlFile) == null) {
                throw new IOException("File FXML non trovato: " + fxmlFile);
            }

            Parent root = loader.load();

            // Ottieni il controller e passa i dati necessari
            LibrarySelectionController controller = loader.getController();
            // Specifica che l'operazione è di aggiunta libri
            controller.setUserId(userId, "add");

            // Imposta la nuova scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento della pagina di selezione libreria: " + e.getMessage());

            statusLabel.setText("Errore: Impossibile aprire la pagina di selezione libreria");
        }
    }
    /**
     * Gestisce il clic sul pulsante "Valuta Libro"
     * Naviga alla schermata di selezione della libreria con modalità valutazione
     */
    @FXML
    public void onRateBookClicked(ActionEvent event) {
        try {
            // Carica la pagina di selezione libreria
            String fxmlFile = "/book_recommender/lab_b/selezionalib.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));

            if (getClass().getResource(fxmlFile) == null) {
                throw new IOException("File FXML non trovato: " + fxmlFile);
            }

            Parent root = loader.load();

            // Ottieni il controller e passa i dati necessari
            LibrarySelectionController controller = loader.getController();
            // Specifica che l'operazione è di valutazione
            controller.setUserId(userId, "rate");

            // Imposta la nuova scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento della pagina di selezione libreria: " + e.getMessage());

            statusLabel.setText("Errore: Impossibile aprire la pagina di selezione libreria");
        }
    }

    /**
     * Gestisce il clic sul pulsante "Consiglia Libro"
     * Naviga alla schermata di selezione della libreria con modalità consiglio
     */
    @FXML
    public void onRecommendBookClicked(ActionEvent event) {
        try {
            // Carica la pagina di selezione libreria
            String fxmlFile = "/book_recommender/lab_b/selezionalib.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));

            if (getClass().getResource(fxmlFile) == null) {
                throw new IOException("File FXML non trovato: " + fxmlFile);
            }

            Parent root = loader.load();

            // Ottieni il controller e passa i dati necessari
            LibrarySelectionController controller = loader.getController();
            // Specifica che l'operazione è di consiglio
            controller.setUserId(userId, "recommend");

            // Imposta la nuova scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento della pagina di selezione libreria: " + e.getMessage());

            statusLabel.setText("Errore: Impossibile aprire la pagina di selezione libreria");
        }
    }
}