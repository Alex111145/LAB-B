package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller per la funzionalità di consiglio libri.
 * Questo controller gestisce l'interfaccia utente per consigliare libri in base a un libro selezionato,
 * con un limite massimo di 3 libri consigliabili.
 */
public class RecommendBookController {

    // Componenti dell'interfaccia utente
    @FXML private Label userIdLabel;
    @FXML private Label selectedBookLabel;
    @FXML private ListView<String> recommendedBooksListView;
    @FXML private Label recommendationsCountLabel;
    @FXML private Label errorLabel;

    // Pulsanti
    @FXML private Button saveButton;

    @FXML private Button clearAllButton;


    // Campi di ricerca
    @FXML private TextField titleSearchField;
    @FXML private TextField authorSearchField;
    @FXML private TextField authorYearSearchField;
    @FXML private TextField yearSearchField;

    // Pulsanti di ricerca


    // Contenitori per i risultati della ricerca
    @FXML private VBox titleResultsContainer;
    @FXML private VBox authorResultsContainer;
    @FXML private VBox authorYearResultsContainer;

    // Variabili di stato
    private String userId;
    private String selectedBook;
    private String libraryName;
    private int selectedBookId;
    private final List<String> recommendedBooks = new ArrayList<>();
    private List<Book> searchResults = new ArrayList<>();

    private DatabaseManager dbManager;

    /**
     * Inizializza il controller.
     */
    public void initialize() {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
        }

        // Nasconde eventuali messaggi di errore
        errorLabel.setVisible(false);

        // Configura gli handler per il tasto Invio nei campi di ricerca
        setupEnterKeyHandlers();

        // Configura la ListView per mostrare i libri consigliati
        setupRecommendedBooksListView();

        // Il bottone saveButton è sempre attivo
        saveButton.setDisable(false);

        // Aggiorna il conteggio delle raccomandazioni
        updateRecommendationsCount();

        // Aggiorna lo stato del bottone clearAllButton
        updateClearAllButtonState();

        // Imposta il colore rosso per il pulsante "Cancella Tutti"
        clearAllButton.setStyle("-fx-background-color: #ff4136; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
    }

    /**
     * Aggiorna lo stato del pulsante ClearAll in base alla presenza di libri selezionati.
     */
    private void updateClearAllButtonState() {
        boolean hasRecommendedBooks = !recommendedBooks.isEmpty();
        clearAllButton.setDisable(!hasRecommendedBooks);
    }

    /**
     * Configura gli handler per il tasto Invio nei campi di ricerca.
     */
    private void setupEnterKeyHandlers() {
        titleSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleTitleSearch(new ActionEvent());
            }
        });

        authorSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleAuthorSearch(new ActionEvent());
            }
        });

        authorYearSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleAuthorYearSearch(new ActionEvent());
            }
        });

        yearSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleAuthorYearSearch(new ActionEvent());
            }
        });
    }

    /**
     * Configura la ListView per i libri consigliati.
     */
    private void setupRecommendedBooksListView() {
        recommendedBooksListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Aggiungi un listener per aggiornare lo stato dei pulsanti quando cambia la selezione
        recommendedBooksListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateClearAllButtonState();
        });

        // Modifica la visualizzazione delle celle per mostrare meglio i titoli dei libri
        recommendedBooksListView.setCellFactory(new Callback<>() {
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
                            // Creiamo un contenitore orizzontale per l'elemento e il pulsante
                            HBox container = new HBox(10);
                            container.setAlignment(Pos.CENTER_LEFT);

                            // Etichetta per il titolo del libro con wrapping
                            Label titleLabel = new Label(item);
                            titleLabel.setWrapText(true);
                            titleLabel.setMaxWidth(280);
                            HBox.setHgrow(titleLabel, Priority.ALWAYS);

                            // Pulsante ❌ per rimuovere l'elemento
                            Button deleteButton = new Button("❌");
                            deleteButton.setStyle(
                                    "-fx-background-color: transparent; " +  // Sfondo trasparente
                                            "-fx-text-fill: #FF0000; " +     // Testo (❌) rosso
                                            "-fx-font-weight: bold; " +
                                            "-fx-cursor: hand;"
                            );
                            deleteButton.setOnAction(event -> {
                                recommendedBooks.remove(item);
                                updateSelectedBooksList();
                                updateRecommendationsCount();
                                updateClearAllButtonState();
                                updateSearchResults();
                            });

                            container.getChildren().addAll(titleLabel, deleteButton);
                            setGraphic(container);
                            setText(null);
                        }
                    }
                };

                // Aggiungiamo un po' di padding alla cella
                cell.setPadding(new Insets(5, 0, 5, 5));
                return cell;
            }
        });
    }

    /**
     * Imposta i dati necessari per il controller.
     *
     * @param userId ID dell'utente
     * @param selectedBook libro selezionato per cui si stanno facendo raccomandazioni
     * @param libraryName nome della libreria
     */
    public void setData(String userId, String selectedBook, String libraryName) {
        this.userId = userId;
        this.selectedBook = selectedBook;
        this.libraryName = libraryName;

        userIdLabel.setText(userId);
        selectedBookLabel.setText("Libro selezionato: " + selectedBook);

        // Get the book ID from the database
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT id FROM books WHERE title = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, selectedBook);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    this.selectedBookId = rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting book ID: " + e.getMessage());
        }

        // Controlla se ci sono già raccomandazioni per questo libro
        loadExistingRecommendations();
    }

    /**
     * Carica le raccomandazioni esistenti per il libro selezionato.
     */
    private void loadExistingRecommendations() {
        recommendedBooks.clear();
        recommendedBooksListView.getItems().clear();

        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT b.title FROM books b " +
                    "JOIN book_recommendations br ON b.id = br.recommended_book_id " +
                    "WHERE br.user_id = ? AND br.source_book_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setInt(2, selectedBookId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    recommendedBooks.add(rs.getString("title"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading recommendations: " + e.getMessage());
        }

        // Aggiorna la ListView con i libri consigliati trovati
        updateSelectedBooksList();

        // Aggiorna il conteggio
        updateRecommendationsCount();

        // Aggiorna lo stato del bottone clearAllButton
        updateClearAllButtonState();
    }

    /**
     * Gestisce la ricerca per titolo.
     */
    @FXML
    public void handleTitleSearch(ActionEvent event) {
        String searchTerm = titleSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            errorLabel.setText("Inserisci un termine di ricerca.");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setVisible(false);
        titleResultsContainer.getChildren().clear();

        // Cerca i libri per titolo
        searchResults = BookService.searchBooksByTitle(searchTerm);

        // Filtra i risultati per escludere il libro selezionato
        searchResults.removeIf(book -> book.getTitle().equals(selectedBook));

        if (searchResults.isEmpty()) {
            Label noResultsLabel = new Label("Nessun risultato trovato.");
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #777777; -fx-padding: 20px;");
            titleResultsContainer.getChildren().add(noResultsLabel);
        } else {
            // Visualizza i risultati
            for (Book book : searchResults) {
                HBox bookBox = createBookResultBox(book);
                titleResultsContainer.getChildren().add(bookBox);
            }
        }
    }

    /**
     * Gestisce la ricerca per autore.
     */
    @FXML
    public void handleAuthorSearch(ActionEvent event) {
        String searchTerm = authorSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            errorLabel.setText("Inserisci un termine di ricerca.");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setVisible(false);
        authorResultsContainer.getChildren().clear();

        // Cerca i libri per autore
        searchResults = BookService.searchBooksByAuthor(searchTerm);

        // Filtra i risultati per escludere il libro selezionato
        searchResults.removeIf(book -> book.getTitle().equals(selectedBook));

        if (searchResults.isEmpty()) {
            Label noResultsLabel = new Label("Nessun risultato trovato.");
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #777777; -fx-padding: 20px;");
            authorResultsContainer.getChildren().add(noResultsLabel);
        } else {
            // Visualizza i risultati
            for (Book book : searchResults) {
                HBox bookBox = createBookResultBox(book);
                authorResultsContainer.getChildren().add(bookBox);
            }
        }
    }

    /**
     * Gestisce la ricerca per autore e anno.
     */
    @FXML
    public void handleAuthorYearSearch(ActionEvent event) {
        String authorTerm = authorYearSearchField.getText().trim();
        String yearTerm = yearSearchField.getText().trim();

        if (authorTerm.isEmpty() || yearTerm.isEmpty()) {
            errorLabel.setText("Inserisci sia l'autore che l'anno.");
            errorLabel.setVisible(true);
            return;
        }

        int year;
        try {
            year = Integer.parseInt(yearTerm);
        } catch (NumberFormatException e) {
            errorLabel.setText("L'anno deve essere un numero.");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setVisible(false);
        authorYearResultsContainer.getChildren().clear();

        // Cerca i libri per autore e anno
        searchResults = BookService.searchBooksByAuthorAndYear(authorTerm, year);

        // Filtra i risultati per escludere il libro selezionato
        searchResults.removeIf(book -> book.getTitle().equals(selectedBook));

        if (searchResults.isEmpty()) {
            Label noResultsLabel = new Label("Nessun risultato trovato.");
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #777777; -fx-padding: 20px;");
            authorYearResultsContainer.getChildren().add(noResultsLabel);
        } else {
            // Visualizza i risultati
            for (Book book : searchResults) {
                HBox bookBox = createBookResultBox(book);
                authorYearResultsContainer.getChildren().add(bookBox);
            }
        }
    }

    /**
     * Crea un box per visualizzare un libro nei risultati di ricerca.
     *
     * @param book Il libro da visualizzare
     * @return Un HBox contenente le informazioni del libro e un pulsante per aggiungerlo
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
        Label detailsLabel = new Label("Categoria: " + book.getCategory() + " | Editore: " + book.getPublisher() + " | Anno: " + book.getPublishYear());
        detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #777777;");
        detailsLabel.setWrapText(true);

        // Aggiungi le etichette al contenitore delle informazioni
        infoBox.getChildren().addAll(titleLabel, authorLabel, detailsLabel);


        Button actionButton = new Button();
        actionButton.setPrefHeight(40);  // Aumentata l'altezza del pulsante
        actionButton.setPrefWidth(100);  // Impostata una larghezza fissa per il pulsante
        actionButton.setMinWidth(100);   // Larghezza minima per garantire che la scritta sia visibile

        // Controlla se il libro è già consigliato o se abbiamo raggiunto il limite
        boolean isRecommended = recommendedBooks.contains(book.getTitle());
        boolean isLimitReached = recommendedBooks.size() >= 3 && !isRecommended;

        if (isRecommended) {
            // Il libro è già nella lista dei consigliati
            actionButton.setText("Rimuovi");
            actionButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 10px 15px; -fx-cursor: hand;");

            // Imposta l'azione per rimuovere il libro
            actionButton.setOnAction(e -> toggleBookSelection(book.getTitle(), actionButton));
        } else if (isLimitReached) {
            // Abbiamo raggiunto il limite di 3 libri
            actionButton.setText("Aggiungi");
            actionButton.setStyle("-fx-background-color: #75B965; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 10px 15px; -fx-cursor: hand;");

            // Se si tenta di aggiungere un quarto libro, mostra un alert
            actionButton.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Limite raggiunto");
                alert.setHeaderText("Limite massimo di libri");
                alert.setContentText("Puoi consigliare al massimo 3 libri. Rimuovi un libro prima di aggiungerne un altro.");
                alert.showAndWait();
            });
        } else {
            // Possiamo ancora aggiungere libri
            actionButton.setText("Aggiungi");
            actionButton.setStyle("-fx-background-color: #75B965; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 10px 15px; -fx-cursor: hand;");

            // Imposta l'azione per aggiungere il libro
            actionButton.setOnAction(e -> toggleBookSelection(book.getTitle(), actionButton));
        }

        bookBox.getChildren().addAll(infoBox, actionButton);
        return bookBox;
    }

    /**
     * Gestisce la selezione/deselezione di un libro.
     *
     * @param bookTitle Il titolo del libro
     * @param button Il pulsante associato al libro
     */
    private void toggleBookSelection(String bookTitle, Button button) {
        // Controlla se il libro è già selezionato
        if (recommendedBooks.contains(bookTitle)) {
            // Rimuovi il libro dalla selezione
            recommendedBooks.remove(bookTitle);
            updateSelectedBooksList();
            button.setText("Aggiungi");
            button.setStyle("-fx-background-color: #75B965; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand;");
        } else {
            // Verifica che non si sia già raggiunto il limite di 3 libri
            if (recommendedBooks.size() >= 3) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Limite raggiunto");
                alert.setHeaderText("Limite massimo di libri");
                alert.setContentText("Puoi consigliare al massimo 3 libri. Rimuovi un libro prima di aggiungerne un altro.");
                alert.showAndWait();
                return;
            }

            // Aggiungi il libro alla selezione
            recommendedBooks.add(bookTitle);
            updateSelectedBooksList();
            button.setText("Rimuovi");
            button.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand;");
        }

        // Aggiorna il contatore dei libri selezionati
        updateRecommendationsCount();

        // Aggiorna lo stato del pulsante clearAllButton
        updateClearAllButtonState();

        // Aggiorna i risultati di ricerca per riflettere il nuovo stato
        updateSearchResults();

        // Nascondi eventuali messaggi di errore
        errorLabel.setVisible(false);
    }

    /**
     * Aggiorna la lista dei libri consigliati.
     */
    private void updateSelectedBooksList() {
        // Pulisci la ListView
        recommendedBooksListView.getItems().clear();

        // Aggiungi i libri selezionati
        List<String> sortedBooks = new ArrayList<>(recommendedBooks);
        Collections.sort(sortedBooks);
        recommendedBooksListView.getItems().addAll(sortedBooks);
    }

    /**
     * Aggiorna il conteggio delle raccomandazioni.
     */
    private void updateRecommendationsCount() {
        recommendationsCountLabel.setText("Selezionati: " + recommendedBooks.size() + "/3");
    }

    /**
     * Aggiorna i pulsanti nei risultati di ricerca in base ai libri attualmente consigliati.
     */
    private void updateSearchResults() {
        // Determina quale contenitore aggiornare in base ai risultati visibili
        if (!titleResultsContainer.getChildren().isEmpty()) {
            titleResultsContainer.getChildren().clear();
            handleTitleSearch(new ActionEvent());
        }

        if (!authorResultsContainer.getChildren().isEmpty()) {
            authorResultsContainer.getChildren().clear();
            handleAuthorSearch(new ActionEvent());
        }

        if (!authorYearResultsContainer.getChildren().isEmpty()) {
            authorYearResultsContainer.getChildren().clear();
            handleAuthorYearSearch(new ActionEvent());
        }
    }

    /**
     * Gestisce la cancellazione di tutti i libri consigliati.
     */
    @FXML
    public void handleClearAll(ActionEvent event) {
        if (recommendedBooks.isEmpty()) {
            return;
        }

        // Cancella tutti i libri selezionati
        recommendedBooks.clear();

        // Aggiorna la lista dei libri selezionati
        updateSelectedBooksList();

        // Aggiorna il contatore
        updateRecommendationsCount();

        // Aggiorna lo stato del pulsante clearAllButton
        updateClearAllButtonState();

        // Aggiorna i pulsanti nei risultati di ricerca
        updateSearchResults();

        // Nascondi eventuali messaggi di errore
        errorLabel.setVisible(false);
    }

    /**
     * Gestisce il salvataggio dei consigli.
     */
    @FXML
    public void handleSave(ActionEvent event) {
        // Controlla se ci sono libri consigliati
        if (recommendedBooks.isEmpty()) {
            // Crea un alert dialog per avvisare l'utente
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Nessun libro consigliato");
            alert.setHeaderText("Nessun libro consigliato");
            alert.setContentText("Vuoi tornare indietro?");

            // Personalizza i pulsanti
            ButtonType buttonTypeSi = new ButtonType("Sì");
            ButtonType buttonTypeNo = new ButtonType("No");

            alert.getButtonTypes().setAll(buttonTypeSi, buttonTypeNo);

            // Mostra l'alert e attendi la risposta
            alert.showAndWait().ifPresent(result -> {
                if (result == buttonTypeSi) {
                    // Se l'utente sceglie "Sì", torna al menu
                    navigateToUserMenu(event);
                }
                // Se l'utente sceglie "No", non fare nulla e rimani nella schermata
            });

            return; // Esci dalla funzione senza salvare
        }

        // Se ci sono libri consigliati, salva i consigli
        if (saveRecommendations()) {
            // Mostra un messaggio di successo
            errorLabel.setText("Consigli salvati con successo!");
            errorLabel.setStyle("-fx-text-fill: green; -fx-font-size: 18px;");
            errorLabel.setVisible(true);
        }
    }

    /**
     * Salva le raccomandazioni nel database.
     * @return true se il salvataggio è avvenuto con successo, false altrimenti
     */
    private boolean saveRecommendations() {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // First, delete existing recommendations for this book
                String deleteSql = "DELETE FROM book_recommendations WHERE user_id = ? AND source_book_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                    pstmt.setString(1, userId);
                    pstmt.setInt(2, selectedBookId);
                    pstmt.executeUpdate();
                }

                // Then, insert new recommendations
                String insertSql = "INSERT INTO book_recommendations (user_id, source_book_id, recommended_book_id) VALUES (?, ?, ?)";
                String findBookSql = "SELECT id FROM books WHERE title = ?";

                try (PreparedStatement findBookStmt = conn.prepareStatement(findBookSql);
                     PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                    for (String recommendedBookTitle : recommendedBooks) {
                        findBookStmt.setString(1, recommendedBookTitle);
                        ResultSet rs = findBookStmt.executeQuery();

                        if (rs.next()) {
                            int recommendedBookId = rs.getInt("id");
                            insertStmt.setString(1, userId);
                            insertStmt.setInt(2, selectedBookId);
                            insertStmt.setInt(3, recommendedBookId);
                            insertStmt.executeUpdate();
                        }
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error saving recommendations: " + e.getMessage());
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
            return false;
        }
    }

    /**
     * Gestisce la rimozione di libri selezionati dalla ListView.
     */
    @FXML
    public void handleRemoveSelected(ActionEvent event) {
        // Ottieni i libri selezionati nella ListView
        List<String> selectedItems = new ArrayList<>(recommendedBooksListView.getSelectionModel().getSelectedItems());

        if (selectedItems.isEmpty()) {
            errorLabel.setText("Seleziona un libro da rimuovere.");
            errorLabel.setVisible(true);
            return;
        }

        // Rimuovi i libri selezionati
        for (String book : selectedItems) {
            recommendedBooks.remove(book);
        }

        // Aggiorna la lista dei libri selezionati
        updateSelectedBooksList();

        // Aggiorna il contatore
        updateRecommendationsCount();

        // Aggiorna lo stato del pulsante clearAllButton
        updateClearAllButtonState();

        // Aggiorna i pulsanti nei risultati di ricerca
        updateSearchResults();

        // Nascondi eventuali messaggi di errore
        errorLabel.setVisible(false);
    }

    /**
     * Gestisce il click sul pulsante "Torna al menu".
     */
    @FXML
    public void handleBack(ActionEvent event) {
        navigateToUserMenu(event);
    }

    /**
     * Gestisce il click sul pulsante "Annulla".
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        navigateToLibrarySelection(event);
    }

    /**
     * Naviga alla schermata di selezione della libreria.
     */
    private void navigateToLibrarySelection(ActionEvent event) {
        try {
            // Carica la schermata di selezione libreria
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/selezionalib.fxml"));
            Parent root = loader.load();

            // Ottieni il controller e passa i dati dell'utente
            LibrarySelectionController controller = loader.getController();
            controller.setUserId(userId, "recommend");

            // Imposta la nuova scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Book Recommender - Seleziona Libreria");
            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento della schermata di selezione libreria: " + e.getMessage());

            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }


    /**
     * Naviga al menu utente.
     */
    private void navigateToUserMenu(ActionEvent event) {
        try {
            // Carica il menu utente
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/userMenu.fxml"));
            Parent root = loader.load();

            // Ottieni il controller e passa i dati dell'utente
            UserMenuController controller = loader.getController();
            controller.setUserData(userId);

            // Imposta la nuova scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Book Recommender - Menu Utente");
            stage.show();

        } catch (IOException e) {
            System.err.println("Errore nel caricamento del menu utente: " + e.getMessage());

            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }}