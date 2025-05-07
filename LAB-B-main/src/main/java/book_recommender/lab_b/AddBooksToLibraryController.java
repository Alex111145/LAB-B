package book_recommender.lab_b;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;


/**
 * Controller per la gestione dell'aggiunta di libri a una libreria.
 * Consente di cercare libri tramite titolo, autore o combinazione autore/anno
 * e di aggiungerli alla libreria dell'utente selezionata.
 */
public class AddBooksToLibraryController implements Initializable {

    // Componenti dell'interfaccia utente
    @FXML private Label userIdLabel;
    @FXML private Label libraryNameLabel;
    @FXML private Label selectedBooksCountLabel;
    @FXML private Label errorLabel;

    // Campi di ricerca
    @FXML private TextField titleSearchField;
    @FXML private TextField authorSearchField;
    @FXML private TextField authorYearSearchField;
    @FXML private TextField yearSearchField;

    // Pulsanti
    @FXML private Button clearAllButton;
    @FXML private Button saveButton;

    // Contenitori per i risultati della ricerca
    @FXML private VBox titleResultsContainer;
    @FXML private VBox authorResultsContainer;
    @FXML private VBox authorYearResultsContainer;

    // Lista dei libri selezionati
    @FXML private ListView<String> selectedBooksListView;

    // Variabili di stato
    private String userId;
    private String libraryName;
    private Set<String> selectedBooks = new HashSet<>();
    private List<Book> searchResults = new ArrayList<>();

    private DatabaseManager dbManager;


    /**
     * Metodo chiamato durante l'inizializzazione del controller.
     * Configura l'interfaccia utente, inizializza il database manager e imposta gli handler per gli eventi.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
        }

        // Nascondi l'etichetta di errore all'avvio
        errorLabel.setVisible(false);

        // Imposta handler per la pressione del tasto Invio nei campi di ricerca
        setupEnterKeyHandlers();

        // Configura la ListView con i libri selezionati
        setupSelectedBooksListView();

        // Il bottone saveButton è sempre attivo
        saveButton.setDisable(false);

        // Aggiorna lo stato del bottone clearAllButton
        updateClearAllButtonState();

        // Imposta il colore rosso per il pulsante "Cancella Tutti"
        clearAllButton.setStyle("-fx-background-color: #ff4136; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
    }

    /**
     * Aggiorna lo stato del pulsante "Cancella Tutti" in base alla presenza di libri selezionati.
     * Disabilita il pulsante se non ci sono libri selezionati.
     */
    private void updateClearAllButtonState() {
        boolean hasSelectedBooks = !selectedBooks.isEmpty();
        clearAllButton.setDisable(!hasSelectedBooks);
    }

    /**
     * Configura la lista dei libri selezionati con funzionalità di selezione multipla
     * e pulsanti per rimuovere singoli libri dalla selezione.
     */
    private void setupSelectedBooksListView() {
        selectedBooksListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        selectedBooksListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateClearAllButtonState();
        });

        selectedBooksListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                ListCell<String> cell = new ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            HBox container = new HBox(10);
                            container.setAlignment(Pos.CENTER_LEFT);

                            Label titleLabel = new Label(item);
                            titleLabel.setWrapText(true);
                            titleLabel.setMaxWidth(280);
                            HBox.setHgrow(titleLabel, Priority.ALWAYS);

                            Button deleteButton = new Button("❌");
                            deleteButton.setStyle(
                                    "-fx-background-color: transparent; " +
                                            "-fx-text-fill: #FF0000; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-cursor: hand;"
                            );
                            deleteButton.setOnAction(event -> {
                                selectedBooks.remove(item);
                                updateSelectedBooksList();
                                updateSelectedBooksCount();
                                updateClearAllButtonState();
                                updateSearchResults();
                            });

                            container.getChildren().addAll(titleLabel, deleteButton);
                            setGraphic(container);
                            setText(null);
                        }
                    }
                };

                cell.setPadding(new Insets(5, 0, 5, 5));
                return cell;
            }
        });
    }

    /**
     * Gestisce l'evento di pulizia di tutti i libri selezionati.
     * Rimuove tutti i libri dalla lista dei selezionati e aggiorna l'interfaccia.
     */
    @FXML
    public void handleClearAll(ActionEvent event) {
        if (selectedBooks.isEmpty()) {
            return;
        }

        selectedBooks.clear();
        updateSelectedBooksList();
        updateSelectedBooksCount();
        updateClearAllButtonState();
        updateSearchResults();
    }

    /**
     * Gestisce l'evento di salvataggio della libreria.
     * Verifica che ci sia almeno un libro selezionato, quindi salva la libreria nel database
     * e naviga al menu utente.
     */
    @FXML
    public void handleSave(ActionEvent event) {
        if (selectedBooks.isEmpty()) {
            errorLabel.setText("Devi aggiungere almeno un libro.");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setVisible(false);
        saveLibraryToDatabase();
        navigateToUserMenuWithMessage(event, "");
    }


    /**
     * Naviga al menu utente visualizzando un messaggio di stato opzionale.
     * Carica il controller UserMenuController e imposta i dati dell'utente.
     */
    private void navigateToUserMenuWithMessage(ActionEvent event, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/userMenu.fxml"));
            Parent root = loader.load();

            UserMenuController controller = loader.getController();
            controller.setUserData(userId);
            controller.setStatusMessage(message);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Configura gli handler per la pressione del tasto Invio nei campi di ricerca.
     * Permette di avviare la ricerca premendo Invio anziché cliccare il pulsante corrispondente.
     */
    private void setupEnterKeyHandlers() {
        titleSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleTitleSearch(new ActionEvent());
            }
        });

        authorSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleAuthorSearch(new ActionEvent());
            }
        });

        authorYearSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleAuthorYearSearch(new ActionEvent());
            }
        });

        yearSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleAuthorYearSearch(new ActionEvent());
            }
        });
    }

    /**
     * Imposta i dati dell'utente e della libreria.
     * Carica i libri esistenti dalla libreria selezionata e aggiorna l'interfaccia.
     */
    public void setData(String userId, String libraryName) {
        this.userId = userId;
        this.libraryName = libraryName;

        userIdLabel.setText(userId);
        libraryNameLabel.setText("Libreria: " + libraryName);

        loadExistingBooks();
        updateSelectedBooksCount();
    }

    /**
     * Carica i libri esistenti nella libreria dal database.
     * Aggiorna la lista dei libri selezionati con quelli già presenti nella libreria.
     */
    private void loadExistingBooks() {
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT b.title FROM books b " +
                    "JOIN library_books lb ON b.id = lb.book_id " +
                    "JOIN libraries l ON lb.library_id = l.id " +
                    "WHERE l.user_id = ? AND l.library_name = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, libraryName);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    selectedBooks.add(rs.getString("title"));
                }
            }

            updateSelectedBooksList();
        } catch (SQLException e) {
            errorLabel.setText("Errore nel caricamento dei libri esistenti: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Gestisce la ricerca dei libri per titolo.
     * Utilizza il servizio BookService per cercare libri corrispondenti al titolo inserito.
     */
    @FXML
    public void handleTitleSearch(ActionEvent event) {
        String title = titleSearchField.getText().trim();

        if (title.isEmpty()) {
            errorLabel.setVisible(false);
            return;
        }

        searchResults = BookService.searchBooksByTitle(title);
        updateSearchResultsContainer(titleResultsContainer, searchResults);
    }

    /**
     * Gestisce la ricerca dei libri per autore.
     * Utilizza il servizio BookService per cercare libri corrispondenti all'autore inserito.
     */
    @FXML
    public void handleAuthorSearch(ActionEvent event) {
        String author = authorSearchField.getText().trim();

        if (author.isEmpty()) {
            errorLabel.setVisible(false);
            return;
        }

        searchResults = BookService.searchBooksByAuthor(author);
        updateSearchResultsContainer(authorResultsContainer, searchResults);
    }

    /**
     * Gestisce la ricerca dei libri per autore e anno di pubblicazione.
     * Verifica che l'anno sia un numero valido prima di effettuare la ricerca.
     */
    @FXML
    public void handleAuthorYearSearch(ActionEvent event) {
        String author = authorYearSearchField.getText().trim();
        String yearText = yearSearchField.getText().trim();

        if (author.isEmpty() || yearText.isEmpty()) {
            errorLabel.setVisible(false);
            return;
        }

        try {
            int year = Integer.parseInt(yearText);
            searchResults = BookService.searchBooksByAuthorAndYear(author, year);
            updateSearchResultsContainer(authorYearResultsContainer, searchResults);
        } catch (NumberFormatException e) {
            errorLabel.setText("Errore: L'anno deve essere un numero.");
            errorLabel.setVisible(true);
        }
    }

    /**
     * Aggiorna il container dei risultati di ricerca con i libri trovati.
     * Mostra un messaggio se non ci sono risultati, altrimenti crea un box per ogni libro.
     */
    private void updateSearchResultsContainer(VBox container, List<Book> books) {
        container.getChildren().clear();

        if (books.isEmpty()) {
            Label noResultsLabel = new Label("Nessun libro trovato.");
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #777777; -fx-padding: 20px;");
            container.getChildren().add(noResultsLabel);
            return;
        }

        for (Book book : books) {
            HBox bookBox = createBookResultBox(book);
            container.getChildren().add(bookBox);
        }
    }

    /**
     * Crea un componente grafico che rappresenta un libro nei risultati di ricerca.
     * Include informazioni sul libro e un pulsante per aggiungerlo/rimuoverlo dalla selezione.
     */
    private HBox createBookResultBox(Book book) {
        HBox bookBox = new HBox();
        bookBox.setAlignment(Pos.CENTER_LEFT);
        bookBox.setSpacing(10);
        bookBox.setPadding(new Insets(10));
        bookBox.setStyle("-fx-background-color: white; -fx-border-color: #EEEEEE; -fx-border-radius: 5px;");

        // Informazioni del libro in un contenitore
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Titolo del libro più grande e in evidenza
        Label titleLabel = new Label(book.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(480); // Imposta una larghezza massima per evitare troncamenti

        // Autore del libro
        Label authorLabel = new Label("Autore: " + book.getAuthors());
        authorLabel.setStyle("-fx-font-size: 14px;");
        authorLabel.setWrapText(true);

        // Dettagli del libro su una riga
        Label detailsLabel = new Label(" Autore: " + book.getAuthors() + " | Anno: " + book.getPublishYear());
        detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #777777;");
        detailsLabel.setWrapText(true);

        // Aggiungi le etichette al contenitore delle informazioni
        infoBox.getChildren().addAll(titleLabel, authorLabel, detailsLabel);

        // Creazione del pulsante con le stesse proprietà di consigli.fxml
        Button actionButton = new Button();

        // CONFIGURAZIONE FISSA DEL PULSANTE - Garantisce sempre le stesse dimensioni
        actionButton.setPrefHeight(40.0);
        actionButton.setPrefWidth(100.0);
        actionButton.setMinHeight(40.0);
        actionButton.setMinWidth(100.0);
        actionButton.setMaxHeight(40.0);   // Fissa l'altezza massima
        actionButton.setMaxWidth(100.0);   // Fissa la larghezza massima

        // Impedisce al pulsante di adattarsi al contenuto
        HBox.setHgrow(actionButton, Priority.NEVER);

        // Aggiungi l'effetto ombra
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#00000080"));
        shadow.setHeight(4.0);
        shadow.setRadius(1.5);
        shadow.setWidth(4.0);
        actionButton.setEffect(shadow);

        // Controlla se il libro è già selezionato
        boolean isSelected = selectedBooks.contains(book.getTitle());

        if (isSelected) {
            // Il libro è già nella lista dei selezionati
            actionButton.setText("Rimuovi");
            actionButton.setStyle(
                    "-fx-background-color: red; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 15; " +
                            "-fx-padding: 10px 15px; " +
                            "-fx-cursor: hand; " +
                            "-fx-min-width: 100px; " +   // Garantisce una larghezza minima
                            "-fx-min-height: 40px; " +   // Garantisce un'altezza minima
                            "-fx-max-width: 100px; " +   // Limita la larghezza massima
                            "-fx-max-height: 40px;"      // Limita l'altezza massima
            );
        } else {
            // Il libro non è ancora selezionato
            actionButton.setText("Aggiungi");
            actionButton.setStyle(
                    "-fx-background-color: #75B965; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 15; " +
                            "-fx-padding: 10px 15px; " +
                            "-fx-cursor: hand; " +
                            "-fx-min-width: 100px; " +   // Garantisce una larghezza minima
                            "-fx-min-height: 40px; " +   // Garantisce un'altezza minima
                            "-fx-max-width: 100px; " +   // Limita la larghezza massima
                            "-fx-max-height: 40px;"      // Limita l'altezza massima
            );
        }

        // Crea un contenitore per il pulsante per garantire posizionamento e dimensioni fisse
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPrefWidth(100);   // Larghezza fissa per il contenitore
        buttonContainer.setMinWidth(100);    // Larghezza minima fissa
        buttonContainer.setMaxWidth(100);    // Larghezza massima fissa
        buttonContainer.getChildren().add(actionButton);

        // Imposta l'azione per il pulsante
        actionButton.setOnAction(e -> toggleBookSelection(book.getTitle(), actionButton));

        // Aggiungi gli elementi al box
        bookBox.getChildren().addAll(infoBox, buttonContainer);
        return bookBox;
    }

    /**
     * Alterna lo stato di selezione di un libro.
     * Se il libro è già selezionato lo rimuove, altrimenti lo aggiunge alla selezione.
     * Aggiorna l'aspetto del pulsante in base allo stato corrente.
     */
    private void toggleBookSelection(String bookTitle, Button button) {
        if (selectedBooks.contains(bookTitle)) {
            selectedBooks.remove(bookTitle);
            button.setText("Aggiungi");
            button.setStyle(
                    "-fx-background-color: #75B965; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 15; " +
                            "-fx-padding: 10px 15px; " +
                            "-fx-cursor: hand; " +
                            "-fx-min-width: 100px; " +
                            "-fx-min-height: 40px; " +
                            "-fx-max-width: 100px; " +
                            "-fx-max-height: 40px;"
            );
        } else {
            selectedBooks.add(bookTitle);
            button.setText("Rimuovi");
            button.setStyle(
                    "-fx-background-color: red; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 15; " +
                            "-fx-padding: 10px 15px; " +
                            "-fx-cursor: hand; " +
                            "-fx-min-width: 100px; " +
                            "-fx-min-height: 40px; " +
                            "-fx-max-width: 100px; " +
                            "-fx-max-height: 40px;"
            );
        }

        updateSelectedBooksList();
        updateSelectedBooksCount();
        updateClearAllButtonState();
        errorLabel.setVisible(false);
    }

    /**
     * Aggiorna la lista dei libri selezionati nell'interfaccia.
     * Ordina alfabeticamente i titoli dei libri per una migliore visualizzazione.
     */
    private void updateSelectedBooksList() {
        selectedBooksListView.getItems().clear();
        List<String> sortedBooks = new ArrayList<>(selectedBooks);
        Collections.sort(sortedBooks);
        selectedBooksListView.getItems().addAll(sortedBooks);
    }

    /**
     * Aggiorna il contatore del numero totale di libri selezionati.
     */
    private void updateSelectedBooksCount() {
        selectedBooksCountLabel.setText("Libri totali: " + selectedBooks.size());
    }

    /**
     * Aggiorna l'interfaccia dei risultati di ricerca.
     * Si assicura che i libri selezionati siano correttamente visualizzati come tali
     * nei risultati di ricerca.
     */
    private void updateSearchResults() {
        if (!searchResults.isEmpty()) {
            if (!titleResultsContainer.getChildren().isEmpty()) {
                updateSearchResultsContainer(titleResultsContainer, searchResults);
            } else if (!authorResultsContainer.getChildren().isEmpty()) {
                updateSearchResultsContainer(authorResultsContainer, searchResults);
            } else if (!authorYearResultsContainer.getChildren().isEmpty()) {
                updateSearchResultsContainer(authorYearResultsContainer, searchResults);
            }
        }
    }

    /**
     * Salva la libreria nel database.
     * Crea la libreria se non esiste, altrimenti aggiorna i libri contenuti.
     * Utilizza una transazione per garantire l'integrità dei dati.
     */
    private void saveLibraryToDatabase() {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // First, get or create the library
                int libraryId = getOrCreateLibrary(conn, userId, libraryName);

                // Clear existing books from the library
                clearLibraryBooks(conn, libraryId);

                // Add selected books to the library
                addBooksToLibrary(conn, libraryId, selectedBooks);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            errorLabel.setText("Errore nel salvataggio della libreria: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Ottiene l'ID di una libreria esistente o ne crea una nuova.
     * Cerca prima una libreria con il nome e ID utente specificati.
     * Se non esiste, ne crea una nuova e restituisce il suo ID.
     */
    private int getOrCreateLibrary(Connection conn, String userId, String libraryName) throws SQLException {
        // Check if a library exists
        String selectSql = "SELECT id FROM libraries WHERE user_id = ? AND library_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, libraryName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        // Create a new library
        String insertSql = "INSERT INTO libraries (user_id, library_name) VALUES (?, ?) RETURNING id";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, libraryName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new SQLException("Failed to create library");
        }
    }

    /**
     * Rimuove tutti i libri esistenti da una libreria.
     * Utilizzato prima di aggiungere i nuovi libri selezionati.
     */
    private void clearLibraryBooks(Connection conn, int libraryId) throws SQLException {
        String sql = "DELETE FROM library_books WHERE library_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, libraryId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Aggiunge i libri selezionati alla libreria.
     * Per ogni titolo di libro selezionato, cerca l'ID del libro nel database
     * e lo associa alla libreria.
     */
    private void addBooksToLibrary(Connection conn, int libraryId, Set<String> bookTitles) throws SQLException {
        String findBookSql = "SELECT id FROM books WHERE title = ?";
        String insertSql = "INSERT INTO library_books (library_id, book_id) VALUES (?, ?)";

        try (PreparedStatement findBookStmt = conn.prepareStatement(findBookSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            for (String bookTitle : bookTitles) {
                findBookStmt.setString(1, bookTitle);
                ResultSet rs = findBookStmt.executeQuery();

                if (rs.next()) {
                    int bookId = rs.getInt("id");
                    insertStmt.setInt(1, libraryId);
                    insertStmt.setInt(2, bookId);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    /**
     * Gestisce l'evento di annullamento dell'operazione.
     * Naviga al menu utente senza salvare le modifiche.
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        navigateToUserMenu(event);
    }

    /**
     * Gestisce l'evento di ritorno al menu precedente.
     * Naviga al menu utente senza salvare le modifiche.
     */
    @FXML
    public void handleBack(ActionEvent event) {
        navigateToUserMenu(event);
    }

    /**
     * Naviga al menu utente.
     * Carica la vista userMenu.fxml e imposta i dati dell'utente.
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
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
}