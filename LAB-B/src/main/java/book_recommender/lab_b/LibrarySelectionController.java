package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibrarySelectionController {

    @FXML private Label userIdLabel;
    @FXML private ListView<String> librariesListView;
    @FXML private Label noLibrariesLabel;
    @FXML private Label errorLabel;
    @FXML private Button selectButton;


    private String userId;
    private final List<String> libraries = new ArrayList<>();
    private String operationType = "select";
    private DatabaseManager dbManager;

    public LibrarySelectionController() {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
        }
    }

    public void initialize() {
        errorLabel.setVisible(false);
        noLibrariesLabel.setVisible(false);
        librariesListView.setOnKeyPressed(this::handleKeyPress);
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String selectedLibrary = librariesListView.getSelectionModel().getSelectedItem();
            if (selectedLibrary != null) {
                handleSelect(new ActionEvent(event.getSource(), null));
            }
        }
    }

    public void setUserId(String userId) {
        this.userId = userId;
        userIdLabel.setText(userId);
        loadUserLibraries(userId);

        if (libraries.isEmpty()) {
            noLibrariesLabel.setVisible(true);
            selectButton.setDisable(true);
        } else {
            noLibrariesLabel.setVisible(false);
            selectButton.setDisable(false);
        }
    }

    public void setUserId(String userId, String operationType) {
        this.operationType = operationType;

        if ("rate".equals(operationType)) {
            selectButton.setText("Avanti");
        } else if ("recommend".equals(operationType)) {
            selectButton.setText("Consiglia");
        } else if ("add".equals(operationType)) {
            selectButton.setText("Avanti");
        }

        setUserId(userId);
    }

    private void loadUserLibraries(String userId) {
        libraries.clear();
        librariesListView.getItems().clear();

        String sql = "SELECT library_name FROM libraries WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String libraryName = rs.getString("library_name");
                if (!libraries.contains(libraryName)) {
                    libraries.add(libraryName);
                }
            }

            if (!libraries.isEmpty()) {
                librariesListView.getItems().addAll(libraries);
            }

        } catch (SQLException e) {
            System.err.println("Error loading user libraries: " + e.getMessage());
            errorLabel.setText("Errore: Impossibile caricare le librerie. " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    @FXML
    public void handleSelect(ActionEvent event) {
        String selectedLibrary = librariesListView.getSelectionModel().getSelectedItem();

        if (selectedLibrary == null) {
            errorLabel.setText("Errore: Seleziona una libreria prima di procedere.");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setVisible(false);

        switch (operationType) {
            case "rate" -> navigateToBookSelection(event, selectedLibrary, "rate");
            case "recommend" -> navigateToBookSelection(event, selectedLibrary, "recommend");
            case "add" -> navigateToAddBooks(event, selectedLibrary);
            case null, default -> navigateToBookSelection(event, selectedLibrary);
        }
    }

    private void navigateToBookSelection(ActionEvent event, String libraryName) {
        navigateToBookSelection(event, libraryName, null);
    }

    private void navigateToBookSelection(ActionEvent event, String libraryName, String operationType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/selezionaLibro.fxml"));
            Parent root = loader.load();

            BookSelectionController controller = loader.getController();
            if (operationType != null) {
                controller.setData(userId, libraryName, operationType);
            } else {
                controller.setData(userId, libraryName);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento della pagina di selezione libro: " + e.getMessage());

            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

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

            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    @FXML
    public void handleBack(ActionEvent event) {
        navigateToUserMenu(event);
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        navigateToUserMenu(event);
    }

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

            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
}