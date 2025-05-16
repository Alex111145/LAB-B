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

/**
 * Controller per la gestione della schermata di login.
 * Gestisce l'autenticazione degli utenti, verificando le credenziali nel database
 * e impedendo login multipli simultanei dello stesso utente.
 */
public class LoginController {

    @FXML private TextField userIdField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorMessage;

    private DatabaseManager dbManager;

    /**
     * Costruttore che inizializza il gestore del database.
     * Ottiene l'istanza singleton del DatabaseManager per gestire le connessioni al database.
     */
    public LoginController() {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Gestione silenziosa dell'errore - sarà gestito in altri metodi
        }
    }

    /**
     * Gestisce la pressione dei tasti nell'interfaccia di login.
     * Quando l'utente preme il tasto Enter, simula il click sul pulsante di login.
     *
     * @param event Evento di pressione tasto
     */
    @FXML
    public void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin(new ActionEvent());
        }
    }

    /**
     * Gestisce il processo di login quando l'utente clicca sul pulsante "Accedi".
     * Verifica le credenziali, controlla se l'utente è già connesso,
     * registra la connessione e naviga al menu utente in caso di successo.
     *
     * @param event Evento di azione generato dal pulsante di login
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String userId = userIdField.getText();
        String password = passwordField.getText();

        // Verifica validità delle credenziali nel database
        if (isValidLogin(userId, password)) {
            errorMessage.setVisible(false);

            // Verifica se l'utente è già connesso al sistema
            if (isUserAlreadyLoggedIn(userId)) {
                // Mostra un avviso e impedisce il login multiplo
                showAlreadyLoggedInAlert(event);
            } else {
                // Registra la connessione dell'utente nel sistema
                registerUserConnection(userId, true);
                // Naviga al menu principale dell'utente
                navigateToUserMenu(event, userId);
            }
        } else {
            // Mostra messaggio di errore per credenziali non valide
            errorMessage.setVisible(true);
        }
    }

    /**
     * Verifica se un utente è già connesso al sistema.
     * Controlla la tabella active_clients per trovare connessioni attive con lo stesso userId.
     *
     * @param userId ID dell'utente da verificare
     * @return true se l'utente è già connesso, false altrimenti
     */
    private boolean isUserAlreadyLoggedIn(String userId) {
        String sql = "SELECT 1 FROM active_clients WHERE client_id LIKE ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Cerca client ID che contengano l'user ID dell'utente con pattern matching
            pstmt.setString(1, "%" + userId + "%");

            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Se c'è almeno un risultato, l'utente è già connesso

        } catch (SQLException e) {
            // In caso di errore, assumiamo che l'utente non sia connesso per sicurezza
            return false;
        }
    }

    /**
     * Registra la connessione o disconnessione di un utente nel sistema.
     * Crea un ID cliente univoco che include l'ID utente e un timestamp.
     *
     * @param userId ID dell'utente
     * @param isConnecting true per registrare una connessione, false per una disconnessione
     */
    private void registerUserConnection(String userId, boolean isConnecting) {
        try {
            // Crea un ID cliente univoco che include l'ID utente e un timestamp
            String clientId = "user_" + userId + "_" + System.currentTimeMillis();

            // Aggiorna la tabella active_clients tramite il DatabaseManager
            dbManager.updateClientConnection(clientId, isConnecting);

        } catch (SQLException e) {
            // Gestione silenziosa dell'errore - l'utente può comunque procedere
        }
    }

    /**
     * Mostra un avviso all'utente che indica che è già connesso in un'altra sessione.
     * Reindirizza l'utente alla homepage dopo la conferma.
     *
     * @param event L'evento di azione per il cambio di scena
     */
    private void showAlreadyLoggedInAlert(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "Un altro utente con lo stesso ID e password è già connesso.",
                ButtonType.OK);
        alert.setTitle("Accesso Negato");
        alert.setHeaderText("Utente già connesso");

        // Mostra l'alert e attende la conferma dell'utente
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
            // In caso di errore durante il caricamento della homepage, mostra un messaggio
            errorMessage.setText("Errore: " + e.getMessage());
            errorMessage.setVisible(true);
        }
    }

    /**
     * Verifica se le credenziali inserite sono valide confrontandole con il database.
     *
     * @param userId ID utente inserito
     * @param password Password inserita
     * @return true se le credenziali sono valide, false altrimenti
     */
    private boolean isValidLogin(String userId, String password) {
        String sql = "SELECT * FROM users WHERE user_id = ? AND password = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // True se esiste un utente con quelle credenziali

        } catch (SQLException e) {
            // In caso di errore di database, assume che il login non sia valido
            return false;
        }
    }

    /**
     * Naviga al menu utente dopo un login riuscito.
     * Carica il controller UserMenuController e passa l'ID utente.
     *
     * @param event L'evento di azione per il cambio di scena
     * @param userId ID dell'utente autenticato
     */
    private void navigateToUserMenu(ActionEvent event, String userId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/userMenu.fxml"));
            Parent root = loader.load();

            // Ottiene il controller e imposta i dati dell'utente
            UserMenuController controller = loader.getController();
            controller.setUserData(userId);

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            // In caso di errore, mostra un messaggio
            errorMessage.setText("Errore: " + e.getMessage());
            errorMessage.setVisible(true);
        }
    }

    /**
     * Gestisce il click sul pulsante "Indietro" per tornare alla homepage.
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
            // In caso di errore, mostra un messaggio
            errorMessage.setText("Errore: " + e.getMessage());
            errorMessage.setVisible(true);
        }
    }
}