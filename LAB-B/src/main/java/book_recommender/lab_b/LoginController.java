package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML
    private TextField userIdField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorMessage;

    private DatabaseManager dbManager;

    public LoginController() {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
        }
    }

    @FXML
    public void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin(new ActionEvent());
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String userId = userIdField.getText();
        String password = passwordField.getText();

        // Verifica se le credenziali sono valide
        if (isValidLogin(userId, password)) {
            errorMessage.setVisible(false);

            // Verifica se l'utente è già connesso
            if (isUserAlreadyLoggedIn(userId)) {
                // Mostra alert di utente già connesso
                showAlreadyLoggedInAlert(event);
            } else {
                // Registra l'utente come connesso
                registerUserConnection(userId, true);
                // Procedi al menu utente
                navigateToUserMenu(event, userId);
            }
        } else {
            errorMessage.setVisible(true);
        }
    }

    /**
     * Verifica se un utente è già connesso al sistema
     * @param userId ID dell'utente da verificare
     * @return true se l'utente è già connesso, false altrimenti
     */
    private boolean isUserAlreadyLoggedIn(String userId) {
        String sql = "SELECT 1 FROM active_clients WHERE client_id LIKE ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Cerca qualsiasi client ID che contenga l'user ID dell'utente
            pstmt.setString(1, "%" + userId + "%");

            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Se c'è almeno un risultato, l'utente è già connesso

        } catch (SQLException e) {
            System.err.println("Errore durante la verifica degli utenti connessi: " + e.getMessage());
            return false; // In caso di errore, procediamo come se l'utente non fosse connesso
        }
    }

    /**
     * Registra la connessione o disconnessione di un utente
     * @param userId ID dell'utente
     * @param isConnecting true per connessione, false per disconnessione
     */
    private void registerUserConnection(String userId, boolean isConnecting) {
        try {
            // Crea un ID cliente che include l'user ID per tracciare quale utente è connesso
            String clientId = "user_" + userId + "_" + System.currentTimeMillis();

            // Usa il DatabaseManager per aggiornare la tabella active_clients
            dbManager.updateClientConnection(clientId, isConnecting);

        } catch (SQLException e) {
            System.err.println("Errore durante la registrazione della connessione utente: " + e.getMessage());
        }
    }

    /**
     * Mostra un alert che informa l'utente che è già connesso
     * e lo reindirizza alla homepage
     */
    private void showAlreadyLoggedInAlert(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "Un altro utente con lo stesso ID e password è già connesso.",
                ButtonType.OK);
        alert.setTitle("Accesso Negato");
        alert.setHeaderText("Utente già connesso");

        // Mostra l'alert e attendi che venga chiuso
        alert.showAndWait();

        // Reindirizza alla homepage
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/homepage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Errore nel caricamento della homepage: " + e.getMessage());
            errorMessage.setText("Errore: " + e.getMessage());
            errorMessage.setVisible(true);
        }
    }

    private boolean isValidLogin(String userId, String password) {
        String sql = "SELECT * FROM users WHERE user_id = ? AND password = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("Error validating login: " + e.getMessage());
            return false;
        }
    }

    private void navigateToUserMenu(ActionEvent event, String userId) {
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

            errorMessage.setText("Errore: " + e.getMessage());
            errorMessage.setVisible(true);
        }
    }

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

            errorMessage.setText("Errore: " + e.getMessage());
            errorMessage.setVisible(true);
        }
    }
}