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

public class BookSelectionController {

    @FXML private Label userIdLabel;
    @FXML private Label libraryNameLabel;
    @FXML private ListView<String> booksListView;
    @FXML private Label noBooksLabel;
    @FXML private Label errorLabel;
    @FXML private Button selectButton;

    private String userId;
    private String libraryName;
    private final List<String> books = new ArrayList<>();
    private String operationType;
    private DatabaseManager dbManager;

    public BookSelectionController() {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
        }
    }

    public void initialize() {
        errorLabel.setVisible(false);
        noBooksLabel.setVisible(false);
        operationType = "select";
        booksListView.setOnKeyPressed(this::handleKeyPress);
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String selectedBook = booksListView.getSelectionModel().getSelectedItem();
            if (selectedBook != null) {
                handleSelect(new ActionEvent(event.getSource(), null));
            }
        }
    }

    public void setData(String userId, String libraryName) {
        this.userId = userId;
        this.libraryName = libraryName;

        userIdLabel.setText(userId);
        libraryNameLabel.setText(libraryName);

        loadUserBooks(userId, libraryName);

        if (books.isEmpty()) {
            noBooksLabel.setVisible(true);
            selectButton.setDisable(true);
        } else {
            noBooksLabel.setVisible(false);
            selectButton.setDisable(false);
        }
    }

    public void setData(String userId, String libraryName, String operationType) {
        this.operationType = operationType;

        if ("rate".equals(operationType)) {
            selectButton.setText("Avanti");
        } else if ("recommend".equals(operationType)) {
            selectButton.setText("Avanti");
        }

        setData(userId, libraryName);
    }

    private void loadUserBooks(String userId, String libraryName) {
        books.clear();
        booksListView.getItems().clear();

        String sql = "SELECT b.title FROM books b " +
                "JOIN library_books lb ON b.id = lb.book_id " +
                "JOIN libraries l ON lb.library_id = l.id " +
                "WHERE l.user_id = ? AND l.library_name = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, libraryName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String bookTitle = rs.getString("title");
                books.add(bookTitle);
            }

            if (!books.isEmpty()) {
                booksListView.getItems().addAll(books);
            }

        } catch (SQLException e) {
            System.err.println("Error loading user books: " + e.getMessage());
            errorLabel.setText("Errore: Impossibile caricare i libri. " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    @FXML
    public void handleSelect(ActionEvent event) {
        String selectedBook = booksListView.getSelectionModel().getSelectedItem();

        if (selectedBook == null) {
            errorLabel.setText("Errore: Seleziona un libro prima di procedere.");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setVisible(false);

        if ("rate".equals(operationType)) {
            navigateToRateBook(event, selectedBook);
        } else if ("recommend".equals(operationType)) {
            navigateToRecommendBook(event, selectedBook);
        } else {
            errorLabel.setText("Libro '" + selectedBook + "' selezionato con successo!");
            errorLabel.setStyle("-fx-text-fill: #75B965;");
            errorLabel.setVisible(true);
        }
    }

    private void navigateToRecommendBook(ActionEvent event, String bookTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/consigli.fxml"));
            Parent root = loader.load();

            RecommendBookController controller = loader.getController();
            controller.setData(userId, bookTitle, libraryName);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento della pagina di consiglio libro: " + e.getMessage());

            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    private void navigateToRateBook(ActionEvent event, String bookTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/valutazione.fxml"));
            Parent root = loader.load();

            RateBookController controller = loader.getController();
            controller.setData(userId, bookTitle, libraryName);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento della pagina di valutazione libro: " + e.getMessage());

            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    @FXML
    public void handleBack(ActionEvent event) {
        navigateToLibrarySelection(event);
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        navigateToLibrarySelection(event);
    }

    private void navigateToLibrarySelection(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/selezionalib.fxml"));
            Parent root = loader.load();

            LibrarySelectionController controller = loader.getController();
            if ("rate".equals(operationType)) {
                controller.setUserId(userId, "rate");
            } else if ("recommend".equals(operationType)) {
                controller.setUserId(userId, "recommend");
            } else {
                controller.setUserId(userId);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento della schermata di selezione libreria: " + e.getMessage());

            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
}