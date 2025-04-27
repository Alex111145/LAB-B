package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.regex.Pattern;

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
    @FXML private Label welcomeLabel;
    @FXML private Button registerButton;
    @FXML private Button backButton;

    private final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private final Pattern CF_PATTERN = Pattern.compile("^[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]$");
    private final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    private DatabaseManager dbManager;

    public RegistrationController() {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
        }
    }

    @FXML
    public void handleRegistration(ActionEvent event) {
        hideAllErrors();
        boolean isValid = true;

        if (nomeCognomeField.getText().trim().isEmpty()) {
            nomeCognomeError.setVisible(true);
            isValid = false;
        }

        String userId = userIdField.getText().trim();
        if (userId.length() > 8) {
            userIdError.setVisible(true);
            isValid = false;
        } else if (isUserIdExists(userId)) {
            userIdExistsError.setVisible(true);
            isValid = false;
        }

        if (!isValidCodiceFiscale(codiceFiscaleField.getText().trim())) {
            codiceFiscaleError.setVisible(true);
            isValid = false;
        }

        if (!isValidEmail(emailField.getText().trim())) {
            emailError.setVisible(true);
            isValid = false;
        }

        if (!isValidPassword(passwordField.getText())) {
            passwordError.setVisible(true);
            isValid = false;
        }

        if (isValid) {
            if (saveUserToDatabase()) {
                navigateToUserMenu(event, userIdField.getText().trim());
            } else {
                successMessage.setText("Errore durante la registrazione. Riprova.");
                successMessage.setVisible(true);
            }
        }
    }

    private boolean isUserIdExists(String userId) {
        String sql = "SELECT user_id FROM users WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("Error checking if userId exists: " + e.getMessage());
            return true; // Return true to prevent registration on error
        }
    }

    private boolean saveUserToDatabase() {
        String sql = "INSERT INTO users (user_id, full_name, fiscal_code, email, password) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userIdField.getText().trim());
            pstmt.setString(2, nomeCognomeField.getText().trim());
            pstmt.setString(3, codiceFiscaleField.getText().trim());
            pstmt.setString(4, emailField.getText().trim());
            pstmt.setString(5, passwordField.getText());

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Error saving user to database: " + e.getMessage());
            return false;
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
            e.printStackTrace();
        }
    }

    private void navigateToUserMenu(ActionEvent event, String userId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/userMenu.fxml"));
            Parent root = loader.load();

            UserMenuController controller = loader.getController();
            controller.setUserData(userId);
            controller.setStatusMessage("Registrazione completata con successo! Benvenuto/a " + userId);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento del menu utente: " + e.getMessage());
            e.printStackTrace();
            successMessage.setText("Errore: " + e.getMessage());
            successMessage.setVisible(true);
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidCodiceFiscale(String cf) {
        return cf != null && !cf.isEmpty() && CF_PATTERN.matcher(cf).matches();
    }

    private boolean isValidPassword(String password) {
        return password != null && !password.isEmpty() && PASSWORD_PATTERN.matcher(password).matches();
    }

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