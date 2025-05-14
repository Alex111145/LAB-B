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

/**
 * Controller per la schermata di selezione dei libri.
 * Gestisce la visualizzazione e la selezione dei libri all'interno di una libreria specifica.
 * Supporta diverse modalità operative (selezione, valutazione, consigli).
 */
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

    /**
     * Costruttore del controller.
     * Inizializza la connessione al database manager.
     */
    public BookSelectionController() {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Gestione silenziosa dell'errore - la connessione sarà tentata di nuovo in seguito
        }
    }

    /**
     * Metodo di inizializzazione chiamato automaticamente dopo che l'FXML è stato caricato.
     * Configura lo stato iniziale dell'interfaccia e registra il gestore degli eventi per i tasti.
     */
    public void initialize() {
        errorLabel.setVisible(false);
        noBooksLabel.setVisible(false);
        operationType = "select";
        booksListView.setOnKeyPressed(this::handleKeyPress);
    }

    /**
     * Gestisce gli eventi di pressione dei tasti nella lista dei libri.
     * Quando viene premuto il tasto Enter, seleziona il libro corrente.
     *
     * @param event Evento di input da tastiera
     */
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String selectedBook = booksListView.getSelectionModel().getSelectedItem();
            if (selectedBook != null) {
                handleSelect(new ActionEvent(event.getSource(), null));
            }
        }
    }

    /**
     * Imposta i dati di base per il controller: ID utente e nome libreria.
     * Carica i libri dell'utente e aggiorna l'interfaccia di conseguenza.
     *
     * @param userId ID dell'utente
     * @param libraryName Nome della libreria
     */
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

    /**
     * Versione estesa del metodo setData che consente di specificare anche il tipo di operazione.
     * Modifica l'etichetta del pulsante di selezione in base al tipo di operazione.
     *
     * @param userId ID dell'utente
     * @param libraryName Nome della libreria
     * @param operationType Tipo di operazione ("rate", "recommend", o "select")
     */
    public void setData(String userId, String libraryName, String operationType) {
        this.operationType = operationType;

        if ("rate".equals(operationType)) {
            selectButton.setText("Avanti");
        } else if ("recommend".equals(operationType)) {
            selectButton.setText("Avanti");
        }

        setData(userId, libraryName);
    }

    /**
     * Carica i libri dell'utente dalla libreria specificata utilizzando una query SQL.
     * Aggiorna la ListView con i titoli dei libri trovati.
     *
     * @param userId ID dell'utente
     * @param libraryName Nome della libreria
     */
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
            errorLabel.setText("Errore: Impossibile caricare i libri. " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Gestisce la selezione di un libro dalla lista.
     * In base al tipo di operazione, naviga alla schermata appropriata o mostra un messaggio.
     *
     * @param event Evento di azione dal pulsante o dalla tastiera
     */
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

    /**
     * Naviga alla schermata di raccomandazione libri per il libro selezionato.
     * Carica il controller RecommendBookController e gli passa i dati necessari.
     *
     * @param event Evento di azione
     * @param bookTitle Titolo del libro selezionato
     */
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
            // Gestione dell'errore di caricamento della vista
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Naviga alla schermata di valutazione per il libro selezionato.
     * Carica il controller RateBookController e gli passa i dati necessari.
     *
     * @param event Evento di azione
     * @param bookTitle Titolo del libro selezionato
     */
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
            // Gestione dell'errore di caricamento della vista
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Gestisce l'evento click sul pulsante "Indietro".
     * Naviga alla schermata di selezione della libreria.
     *
     * @param event Evento di azione
     */
    @FXML
    public void handleBack(ActionEvent event) {
        navigateToLibrarySelection(event);
    }

    /**
     * Gestisce l'evento click sul pulsante "Annulla".
     * Naviga alla schermata di selezione della libreria.
     *
     * @param event Evento di azione
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        navigateToLibrarySelection(event);
    }

    /**
     * Naviga alla schermata di selezione della libreria.
     * Passa il tipo di operazione al controller LibrarySelectionController.
     *
     * @param event Evento di azione
     */
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
            // Gestione dell'errore di caricamento della vista
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
}