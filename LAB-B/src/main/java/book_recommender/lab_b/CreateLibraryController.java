package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

/**
 * Controller per la pagina di creazione della libreria.
 */
public class CreateLibraryController implements Initializable {

    @FXML private Label userIdLabel;
    @FXML private TextField libraryNameField;
    @FXML private Label infoLabel;
    @FXML private Label errorLabel;
    @FXML private Button createButton;
    @FXML private Button cancelButton;
    @FXML private Button backButton;

    private String userId;
    private DatabaseManager dbManager;

    /**
     * Inizializza il controller.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
        }
        errorLabel.setVisible(false);
    }

    /**
     * Imposta l'ID dell'utente corrente.
     *
     * @param userId l'ID dell'utente
     */
    public void setUserId(String userId) {
        this.userId = userId;
        userIdLabel.setText(userId);
    }

    /**
     * Gestisce il click sul pulsante "Crea e aggiungi libri".
     */
    @FXML
    public void handleCreate(ActionEvent event) {
        String libraryName = libraryNameField.getText().trim();

        if (libraryName.isEmpty()) {
            // Mostra il messaggio di errore se il nome della libreria è vuoto
            errorLabel.setText("Il nome della libreria non può essere vuoto.");
            errorLabel.setVisible(true);
            return;
        }

        // Verifica se la libreria esiste già
        if (checkLibraryExists(libraryName)) {
            errorLabel.setText("Una libreria con questo nome esiste già.");
            errorLabel.setVisible(true);
            return;
        }

        // Crea la libreria nel database
        if (createLibrary(libraryName)) {
            // Naviga alla pagina di aggiunta libri
            navigateToAddBooks(event, libraryName);
        } else {
            errorLabel.setText("Errore nella creazione della libreria.");
            errorLabel.setVisible(true);
        }
    }

    /**
     * Verifica se una libreria con lo stesso nome esiste già per l'utente.
     */
    private boolean checkLibraryExists(String libraryName) {
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT id FROM libraries WHERE user_id = ? AND library_name = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, libraryName);
                ResultSet rs = pstmt.executeQuery();

                return rs.next(); // Ritorna true se esiste già
            }
        } catch (SQLException e) {
            System.err.println("Error checking library existence: " + e.getMessage());
            return true; // In caso di errore, assumiamo che esista per sicurezza
        }
    }

    /**
     * Crea una nuova libreria nel database.
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
            System.err.println("Error creating library: " + e.getMessage());
            return false;
        }
    }

    /**
     * Naviga alla pagina di aggiunta libri.
     */
    private void navigateToAddBooks(ActionEvent event, String libraryName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/aggiungilibro.fxml"));
            Parent root = loader.load();

            AddBooksToLibraryController controller = loader.getController();
            controller.setData(userId, libraryName);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento della pagina di aggiunta libri: " + e.getMessage());
            e.printStackTrace();
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Gestisce il click sul pulsante "Annulla".
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        navigateToUserMenu(event);
    }

    /**
     * Gestisce il click sul pulsante "Logout".
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
            System.err.println("Errore nel caricamento della homepage: " + e.getMessage());
            e.printStackTrace();
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Naviga al menu utente.
     */
    private void navigateToUserMenu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/userMenu.fxml"));
            Parent root = loader.load();

            UserMenuController controller = loader.getController();
            controller.setUserData(userId);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento del menu utente: " + e.getMessage());
            e.printStackTrace();
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
}