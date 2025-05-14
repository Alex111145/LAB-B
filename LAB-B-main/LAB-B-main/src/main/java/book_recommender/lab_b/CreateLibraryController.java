package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

/**
 * Controller che gestisce la creazione di nuove librerie personali.
 * Permette all'utente di creare una libreria con un nome univoco e
 * successivamente navigare alla schermata di aggiunta libri.
 */
public class CreateLibraryController implements Initializable {

    @FXML private Label userIdLabel;
    @FXML private TextField libraryNameField;
    @FXML private Label errorLabel;

    private String userId;
    private DatabaseManager dbManager;

    /**
     * Inizializza il controller recuperando l'istanza del DatabaseManager
     * e impostando lo stato iniziale dell'interfaccia utente.
     *
     * @param location URL della risorsa FXML
     * @param resources ResourceBundle per localizzare l'oggetto root
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Gestione silenziosa dell'errore - verrà gestito in altri metodi
        }
        // Nasconde l'etichetta di errore all'avvio
        errorLabel.setVisible(false);
    }

    /**
     * Imposta l'ID dell'utente nel controller e lo visualizza nell'interfaccia.
     * Questo metodo deve essere chiamato subito dopo aver caricato la vista.
     *
     * @param userId ID dell'utente corrente
     */
    public void setUserId(String userId) {
        this.userId = userId;
        userIdLabel.setText(userId);
    }

    /**
     * Gestisce l'evento di click sul pulsante "Crea".
     * Valida il nome della libreria inserito, verifica che non esista già,
     * crea la nuova libreria nel database e naviga alla schermata di aggiunta libri.
     *
     * @param event L'evento di azione generato dal pulsante
     */
    @FXML
    public void handleCreate(ActionEvent event) {
        String libraryName = libraryNameField.getText().trim();

        // Validazione: il nome della libreria non può essere vuoto
        if (libraryName.isEmpty()) {
            errorLabel.setText("Il nome della libreria non può essere vuoto.");
            errorLabel.setVisible(true);
            return;
        }

        // Verifica se la libreria esiste già per questo utente
        if (checkLibraryExists(libraryName)) {
            errorLabel.setText("Una libreria con questo nome esiste già.");
            errorLabel.setVisible(true);
            return;
        }

        // Crea la libreria nel database e naviga alla prossima schermata
        if (createLibrary(libraryName)) {
            navigateToAddBooks(event, libraryName);
        } else {
            errorLabel.setText("Errore nella creazione della libreria.");
            errorLabel.setVisible(true);
        }
    }

    /**
     * Verifica se una libreria con lo stesso nome esiste già per l'utente corrente.
     * Questo evita duplicazioni di nomi di libreria per lo stesso utente.
     *
     * @param libraryName Nome della libreria da verificare
     * @return true se la libreria esiste già, false altrimenti
     */
    private boolean checkLibraryExists(String libraryName) {
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT id FROM libraries WHERE user_id = ? AND library_name = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, libraryName);
                ResultSet rs = pstmt.executeQuery();

                return rs.next(); // Ritorna true se esiste già una libreria con questo nome
            }
        } catch (SQLException e) {
            // In caso di errore di database, per sicurezza assumiamo che la libreria esista
            return true;
        }
    }

    /**
     * Crea una nuova libreria nel database per l'utente corrente.
     *
     * @param libraryName Nome della nuova libreria
     * @return true se l'operazione è andata a buon fine, false altrimenti
     */
    private boolean createLibrary(String libraryName) {
        try (Connection conn = dbManager.getConnection()) {
            String sql = "INSERT INTO libraries (user_id, library_name) VALUES (?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, libraryName);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            // Fallimento dell'operazione di inserimento
            return false;
        }
    }

    /**
     * Naviga alla pagina di aggiunta libri dopo aver creato la libreria.
     * Carica il controller AddBooksToLibraryController e passa i dati necessari.
     *
     * @param event L'evento di azione usato per il cambio di scena
     * @param libraryName Nome della libreria creata
     */
    private void navigateToAddBooks(ActionEvent event, String libraryName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/aggiungilibro.fxml"));
            Parent root = loader.load();

            // Ottieni il controller e passa i dati
            AddBooksToLibraryController controller = loader.getController();
            controller.setData(userId, libraryName);

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Gestisce l'evento di click sul pulsante "Annulla".
     * Torna al menu utente senza creare alcuna libreria.
     *
     * @param event L'evento di azione generato dal pulsante
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        navigateToUserMenu(event);
    }

    /**
     * Gestisce l'evento di click sul pulsante "Indietro".
     * Torna alla homepage invece che al menu utente.
     *
     * @param event L'evento di azione generato dal pulsante
     */
    @FXML
    public void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/homepage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Naviga al menu utente.
     * Carica il controller UserMenuController e passa l'ID utente.
     *
     * @param event L'evento di azione usato per il cambio di scena
     */
    private void navigateToUserMenu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/userMenu.fxml"));
            Parent root = loader.load();

            // Ottieni il controller e passa l'ID utente
            UserMenuController controller = loader.getController();
            controller.setUserData(userId);

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
}