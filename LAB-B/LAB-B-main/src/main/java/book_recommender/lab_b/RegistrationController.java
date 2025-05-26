package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.regex.Pattern;

/**
 * Controller per la schermata di registrazione degli utenti.
 * Gestisce la validazione dei dati di registrazione, la verifica di unicità dell'ID utente,
 * e il salvataggio dei nuovi utenti nel database.
 */
public class RegistrationController {

    @FXML private TextField nomeCognomeField;
    @FXML private TextField userIdField;
    @FXML private TextField codiceFiscaleField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label nomeCognomeError;
    @FXML private Label userIdError;
    @FXML private Label userIdExistsError;
    @FXML private Label codiceFiscaleError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label successMessage;

    // Pattern regex per la validazione dei dati

    /**
     * Pattern per la validazione delle email.
     * Verifica che l'email contenga una @ seguita da un dominio valido con TLD riconosciuto.
     */
    private final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)\\.(com|it|org|net|edu|gov|mil|biz|info|io|co|uk|de|fr|es|ru|cn|jp|br|nl|eu|ch|se|no|dk|fi|pl|au|nz|in)$");

    /**
     * Pattern per la validazione dei codici fiscali italiani.
     * Formato: 6 lettere + 2 numeri + 1 lettera + 2 numeri + 1 lettera + 3 numeri + 1 lettera
     */
    private final Pattern CF_PATTERN = Pattern.compile("^[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]$");

    /**
     * Pattern per la validazione delle password.
     * Richiede almeno 8 caratteri, almeno una lettera minuscola, almeno una maiuscola,
     * almeno un numero e almeno un carattere speciale.
     */
    private final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    private DatabaseManager dbManager;

    /**
     * Costruttore che inizializza il gestore del database.
     */
    public RegistrationController() {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Gestione silenziosa dell'errore - sarà gestito nei metodi che usano il database
        }
    }

    /**
     * Gestisce il processo di registrazione quando l'utente clicca sul pulsante "Registrati".
     * Valida tutti i campi di input e, se validi, registra il nuovo utente nel database.
     *
     * @param event Evento di azione generato dal pulsante di registrazione
     */
    @FXML
    public void handleRegistration(ActionEvent event) {
        // Nasconde tutti i messaggi di errore prima di iniziare la validazione
        hideAllErrors();
        boolean isValid = true;

        // Controlla che il nome e cognome non siano vuoti
        if (nomeCognomeField.getText().trim().isEmpty()) {
            nomeCognomeError.setVisible(true);
            isValid = false;
        }

        // Controlla che l'ID utente sia valido e non già esistente
        String userId = userIdField.getText().trim();
        if (userId.length() > 8) {
            userIdError.setVisible(true);
            isValid = false;
        } else if (isUserIdExists(userId)) {
            userIdExistsError.setVisible(true);
            isValid = false;
        }

        // Controlla che il codice fiscale sia nel formato corretto
        String codiceFiscale = codiceFiscaleField.getText().trim().toUpperCase();
        if (!isValidCodiceFiscale(codiceFiscale)) {
            codiceFiscaleError.setVisible(true);
            isValid = false;
        }

        // Controlla che l'email sia in un formato valido
        if (!isValidEmail(emailField.getText().trim())) {
            emailError.setVisible(true);
            isValid = false;
        }

        // Controlla che la password rispetti i requisiti di sicurezza
        if (!isValidPassword(passwordField.getText())) {
            passwordError.setVisible(true);
            isValid = false;
        }

        // Se tutti i controlli sono passati, salva l'utente nel database
        if (isValid) {
            if (saveUserToDatabase()) {
                // Naviga al menu utente dopo una registrazione riuscita
                navigateToUserMenu(event, userIdField.getText().trim());
            } else {
                // Mostra un messaggio di errore se il salvataggio fallisce
                successMessage.setText("Errore durante la registrazione. Riprova.");
                successMessage.setVisible(true);
            }
        }
    }

    /**
     * Verifica se un ID utente esiste già nel database.
     *
     * @param userId ID utente da verificare
     * @return true se l'ID esiste già, false altrimenti
     */
    private boolean isUserIdExists(String userId) {
        String sql = "SELECT user_id FROM users WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // True se esiste già un utente con questo ID

        } catch (SQLException e) {
            // In caso di errore di database, assume che l'ID esista già per sicurezza
            return true;
        }
    }

    /**
     * Salva un nuovo utente nel database con tutti i dati forniti nel form.
     *
     * @return true se il salvataggio è riuscito, false altrimenti
     */
    private boolean saveUserToDatabase() {
        String sql = "INSERT INTO users (user_id, full_name, fiscal_code, email, password) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userIdField.getText().trim());
            pstmt.setString(2, nomeCognomeField.getText().trim());
            // Converte il codice fiscale in maiuscolo per uniformità
            pstmt.setString(3, codiceFiscaleField.getText().trim().toUpperCase());
            pstmt.setString(4, emailField.getText().trim());
            pstmt.setString(5, passwordField.getText());

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            // Fallimento nell'inserimento del nuovo utente
            return false;
        }
    }

    /**
     * Gestisce il click sul pulsante "Indietro" per tornare alla homepage.
     *
     * @param event Evento di azione generato dal pulsante
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
            // Gestione silenziosa dell'errore - l'utente può riprovare
        }
    }

    /**
     * Naviga al menu utente dopo una registrazione riuscita.
     * Passa l'ID utente al controller del menu utente e mostra un messaggio di benvenuto.
     *
     * @param event Evento di azione per il cambio di scena
     * @param userId ID dell'utente appena registrato
     */
    private void navigateToUserMenu(ActionEvent event, String userId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/userMenu.fxml"));
            Parent root = loader.load();

            // Ottiene il controller e imposta i dati dell'utente con un messaggio di benvenuto
            UserMenuController controller = loader.getController();
            controller.setUserData(userId);
            controller.setStatusMessage("Registrazione completata con successo! Benvenuto/a " + userId);

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            // In caso di errore, mostra un messaggio
            successMessage.setText(e.getMessage());
            successMessage.setVisible(true);
        }
    }

    /**
     * Verifica se un'email è in un formato valido.
     *
     * @param email Email da validare
     * @return true se l'email è valida, false altrimenti
     */
    private boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Verifica se un codice fiscale è in un formato valido.
     * Il codice fiscale deve rispettare il formato standard italiano.
     *
     * @param cf Codice fiscale da validare (già convertito in maiuscolo)
     * @return true se il codice fiscale è valido, false altrimenti
     */
    private boolean isValidCodiceFiscale(String cf) {
        return cf != null && !cf.isEmpty() && CF_PATTERN.matcher(cf).matches();
    }

    /**
     * Verifica se una password rispetta i requisiti di sicurezza.
     * La password deve contenere almeno 8 caratteri, una maiuscola, una minuscola,
     * un numero e un carattere speciale.
     *
     * @param password Password da validare
     * @return true se la password è valida, false altrimenti
     */
    private boolean isValidPassword(String password) {
        return password != null && !password.isEmpty() && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Nasconde tutti i messaggi di errore nell'interfaccia.
     * Utilizzato all'inizio della validazione per mostrare solo i messaggi pertinenti.
     */
    private void hideAllErrors() {
        nomeCognomeError.setVisible(false);
        userIdError.setVisible(false);
        userIdExistsError.setVisible(false);
        codiceFiscaleError.setVisible(false);
        emailError.setVisible(false);
        passwordError.setVisible(false);
        successMessage.setVisible(false);
    }
}