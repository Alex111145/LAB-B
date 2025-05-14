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
 * Gestisce l'interfaccia utente per consigliare libri in base a un libro selezionato.
 * Permette all'utente di cercare, selezionare e salvare fino a 3 libri come raccomandazioni
 * per un libro di origine.
 *
 * Funzionalità principali:
 * - Ricerca di libri per titolo, autore o combinazione di autore e anno
 * - Selezione fino a 3 libri come raccomandazioni
 * - Salvataggio delle raccomandazioni nel database
 * - Visualizzazione delle raccomandazioni esistenti
 */
public class RecommendBookController {

    /**
     * Etichetta che mostra l'ID dell'utente corrente.
     */
    @FXML private Label userIdLabel;

    /**
     * Etichetta che mostra il titolo del libro per cui si stanno facendo raccomandazioni.
     */
    @FXML private Label selectedBookLabel;

    /**
     * ListView che mostra i libri attualmente consigliati.
     */
    @FXML private ListView<String> recommendedBooksListView;

    /**
     * Etichetta che mostra il conteggio delle raccomandazioni (x/3).
     */
    @FXML private Label recommendationsCountLabel;

    /**
     * Etichetta per mostrare messaggi di errore.
     */
    @FXML private Label errorLabel;

    /**
     * Pulsante per salvare le raccomandazioni selezionate nel database.
     */
    @FXML private Button saveButton;

    /**
     * Pulsante per cancellare tutte le raccomandazioni selezionate.
     */
    @FXML private Button clearAllButton;

    /**
     * Campo di testo per la ricerca per titolo.
     */
    @FXML private TextField titleSearchField;

    /**
     * Campo di testo per la ricerca per autore.
     */
    @FXML private TextField authorSearchField;

    /**
     * Campo di testo per la ricerca combinata per autore (con anno).
     */
    @FXML private TextField authorYearSearchField;

    /**
     * Campo di testo per la ricerca per anno.
     */
    @FXML private TextField yearSearchField;

    /**
     * Contenitore per visualizzare i risultati della ricerca per titolo.
     */
    @FXML private VBox titleResultsContainer;

    /**
     * Contenitore per visualizzare i risultati della ricerca per autore.
     */
    @FXML private VBox authorResultsContainer;

    /**
     * Contenitore per visualizzare i risultati della ricerca per autore e anno.
     */
    @FXML private VBox authorYearResultsContainer;

    /**
     * ID dell'utente corrente.
     */
    private String userId;

    /**
     * Titolo del libro selezionato per cui si consigliano altri libri.
     */
    private String selectedBook;

    /**
     * Nome della libreria selezionata.
     */
    private String libraryName;

    /**
     * ID nel database del libro selezionato.
     */
    private int selectedBookId;

    /**
     * Lista dei titoli dei libri consigliati.
     */
    private final List<String> recommendedBooks = new ArrayList<>();

    /**
     * Lista di oggetti Book risultanti dall'ultima ricerca.
     */
    private List<Book> searchResults = new ArrayList<>();

    /**
     * Gestore della connessione al database.
     */
    private DatabaseManager dbManager;

    /**
     * Inizializza il controller.
     * Questo metodo viene chiamato automaticamente quando l'FXML viene caricato.
     * Configura il layout, gli eventi e ottiene una connessione al database.
     */
    public void initialize() {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Gestione dell'errore silente - potrebbe essere migliorata con log o messaggi
        }

        // Nasconde eventuali messaggi di errore all'avvio
        errorLabel.setVisible(false);

        // Configura gli handler per il tasto Invio nei campi di ricerca
        setupEnterKeyHandlers();

        // Configura la ListView per mostrare i libri consigliati con opzioni di eliminazione
        setupRecommendedBooksListView();

        // Il bottone saveButton è sempre attivo (potrebbe essere cambiato in base alla logica)
        saveButton.setDisable(false);

        // Aggiorna il conteggio delle raccomandazioni (0/3 all'inizio)
        updateRecommendationsCount();

        // Aggiorna lo stato del bottone clearAllButton (disabilitato se non ci sono libri)
        updateClearAllButtonState();

        // Imposta lo stile personalizzato per il pulsante "Cancella Tutti"
        clearAllButton.setStyle("-fx-background-color: #ff4136; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
    }

    /**
     * Aggiorna lo stato del pulsante "Cancella Tutti" in base alla presenza di libri selezionati.
     * Il pulsante viene disabilitato se non ci sono libri da cancellare.
     */
    private void updateClearAllButtonState() {
        boolean hasRecommendedBooks = !recommendedBooks.isEmpty();
        clearAllButton.setDisable(!hasRecommendedBooks);
    }

    /**
     * Configura gli handler per il tasto Invio nei campi di ricerca.
     * Consente all'utente di eseguire le ricerche semplicemente premendo Invio
     * dopo aver inserito i termini di ricerca, senza dover cliccare sui pulsanti.
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
     * Configura la ListView per visualizzare i libri consigliati.
     * Personalizza le celle per includere un pulsante di eliminazione accanto a ogni libro.
     * Aggiunge listener per aggiornare lo stato dei pulsanti quando cambia la selezione.
     */
    private void setupRecommendedBooksListView() {
        // Consente la selezione multipla per permettere di rimuovere più libri contemporaneamente
        recommendedBooksListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Aggiungi un listener per aggiornare lo stato dei pulsanti quando cambia la selezione
        recommendedBooksListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateClearAllButtonState();
        });

        // Personalizza la visualizzazione delle celle per mostrare meglio i titoli dei libri e aggiungere un pulsante di rimozione
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

                            // Etichetta per il titolo del libro con wrapping per gestire titoli lunghi
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

                // Aggiungiamo un po' di padding alla cella per migliorare la leggibilità
                cell.setPadding(new Insets(5, 0, 5, 5));
                return cell;
            }
        });
    }

    /**
     * Imposta i dati necessari per il controller.
     * Questo metodo viene chiamato dopo l'inizializzazione per configurare il controller
     * con i dati specifici dell'utente e del libro selezionato.
     *
     * @param userId ID dell'utente corrente
     * @param selectedBook libro selezionato per cui si stanno facendo raccomandazioni
     * @param libraryName nome della libreria selezionata
     */
    public void setData(String userId, String selectedBook, String libraryName) {
        this.userId = userId;
        this.selectedBook = selectedBook;
        this.libraryName = libraryName;

        // Aggiorna le etichette dell'interfaccia con i dati dell'utente e del libro
        userIdLabel.setText(userId);
        selectedBookLabel.setText("Libro selezionato: " + selectedBook);

        // Recupera l'ID del libro dal database per utilizzo nelle query successive
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
            // Gestione dell'errore silente
        }

        // Carica le raccomandazioni esistenti per questo libro e utente
        loadExistingRecommendations();
    }

    /**
     * Carica le raccomandazioni esistenti per il libro selezionato.
     * Interroga il database per trovare le raccomandazioni precedentemente salvate
     * per il libro e l'utente correnti e le visualizza nell'interfaccia.
     */
    private void loadExistingRecommendations() {
        // Pulisce la lista e l'interfaccia prima di caricare i dati
        recommendedBooks.clear();
        recommendedBooksListView.getItems().clear();

        try (Connection conn = dbManager.getConnection()) {
            // Query per recuperare i titoli dei libri consigliati per il libro selezionato
            String sql = "SELECT b.title FROM books b " +
                    "JOIN book_recommendations br ON b.id = br.recommended_book_id " +
                    "WHERE br.user_id = ? AND br.source_book_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setInt(2, selectedBookId);
                ResultSet rs = pstmt.executeQuery();

                // Popola la lista con i titoli trovati
                while (rs.next()) {
                    recommendedBooks.add(rs.getString("title"));
                }
            }
        } catch (SQLException e) {
            // Gestione dell'errore silente
        }

        // Aggiorna l'interfaccia utente con i dati caricati
        updateSelectedBooksList();
        updateRecommendationsCount();
        updateClearAllButtonState();
    }

    /**
     * Gestisce la ricerca per titolo.
     * Esegue una ricerca nel database per trovare libri il cui titolo contiene
     * il termine di ricerca inserito dall'utente.
     *
     * @param event L'evento che ha scatenato la ricerca
     */
    @FXML
    public void handleTitleSearch(ActionEvent event) {
        String searchTerm = titleSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            errorLabel.setText("Inserisci un termine di ricerca.");
            errorLabel.setVisible(true);
            return;
        }

        // Nasconde messaggi di errore precedenti e pulisce i risultati vecchi
        errorLabel.setVisible(false);
        titleResultsContainer.getChildren().clear();

        // Esegue la ricerca per titolo attraverso il servizio BookService
        searchResults = BookService.searchBooksByTitle(searchTerm);

        // Esclude il libro selezionato dai risultati per evitare autoreferenze
        searchResults.removeIf(book -> book.getTitle().equals(selectedBook));

        if (searchResults.isEmpty()) {
            // Mostra un messaggio se non ci sono risultati
            Label noResultsLabel = new Label("Nessun risultato trovato.");
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #777777; -fx-padding: 20px;");
            titleResultsContainer.getChildren().add(noResultsLabel);
        } else {
            // Visualizza i risultati trovati
            for (Book book : searchResults) {
                HBox bookBox = createBookResultBox(book);
                titleResultsContainer.getChildren().add(bookBox);
            }
        }
    }

    /**
     * Gestisce la ricerca per autore.
     * Esegue una ricerca nel database per trovare libri scritti da autori
     * il cui nome contiene il termine di ricerca inserito dall'utente.
     *
     * @param event L'evento che ha scatenato la ricerca
     */
    @FXML
    public void handleAuthorSearch(ActionEvent event) {
        String searchTerm = authorSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            errorLabel.setText("Inserisci un termine di ricerca.");
            errorLabel.setVisible(true);
            return;
        }

        // Nasconde messaggi di errore precedenti e pulisce i risultati vecchi
        errorLabel.setVisible(false);
        authorResultsContainer.getChildren().clear();

        // Esegue la ricerca per autore attraverso il servizio BookService
        searchResults = BookService.searchBooksByAuthor(searchTerm);

        // Esclude il libro selezionato dai risultati
        searchResults.removeIf(book -> book.getTitle().equals(selectedBook));

        if (searchResults.isEmpty()) {
            // Mostra un messaggio se non ci sono risultati
            Label noResultsLabel = new Label("Nessun risultato trovato.");
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #777777; -fx-padding: 20px;");
            authorResultsContainer.getChildren().add(noResultsLabel);
        } else {
            // Visualizza i risultati trovati
            for (Book book : searchResults) {
                HBox bookBox = createBookResultBox(book);
                authorResultsContainer.getChildren().add(bookBox);
            }
        }
    }

    /**
     * Gestisce la ricerca combinata per autore e anno.
     * Esegue una ricerca nel database per trovare libri scritti da un autore specifico
     * in un determinato anno di pubblicazione.
     *
     * @param event L'evento che ha scatenato la ricerca
     */
    @FXML
    public void handleAuthorYearSearch(ActionEvent event) {
        String authorTerm = authorYearSearchField.getText().trim();
        String yearTerm = yearSearchField.getText().trim();

        // Verifica che entrambi i campi siano stati compilati
        if (authorTerm.isEmpty() || yearTerm.isEmpty()) {
            errorLabel.setText("Inserisci sia l'autore che l'anno.");
            errorLabel.setVisible(true);
            return;
        }

        // Verifica che l'anno sia un numero valido
        int year;
        try {
            year = Integer.parseInt(yearTerm);
        } catch (NumberFormatException e) {
            errorLabel.setText("L'anno deve essere un numero.");
            errorLabel.setVisible(true);
            return;
        }

        // Nasconde messaggi di errore precedenti e pulisce i risultati vecchi
        errorLabel.setVisible(false);
        authorYearResultsContainer.getChildren().clear();

        // Esegue la ricerca combinata attraverso il servizio BookService
        searchResults = BookService.searchBooksByAuthorAndYear(authorTerm, year);

        // Esclude il libro selezionato dai risultati
        searchResults.removeIf(book -> book.getTitle().equals(selectedBook));

        if (searchResults.isEmpty()) {
            // Mostra un messaggio se non ci sono risultati
            Label noResultsLabel = new Label("Nessun risultato trovato.");
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #777777; -fx-padding: 20px;");
            authorYearResultsContainer.getChildren().add(noResultsLabel);
        } else {
            // Visualizza i risultati trovati
            for (Book book : searchResults) {
                HBox bookBox = createBookResultBox(book);
                authorYearResultsContainer.getChildren().add(bookBox);
            }
        }
    }

    /**
     * Crea un box per visualizzare un libro nei risultati di ricerca.
     * Genera un componente grafico che mostra le informazioni del libro e un pulsante
     * per aggiungerlo o rimuoverlo dai consigli.
     *
     * @param book Il libro da visualizzare
     * @return Un HBox contenente le informazioni del libro e un pulsante per aggiungerlo/rimuoverlo
     */
    private HBox createBookResultBox(Book book) {
        // Contenitore principale per il risultato del libro
        HBox bookBox = new HBox();
        bookBox.setAlignment(Pos.CENTER_LEFT);
        bookBox.setSpacing(10);
        bookBox.setPadding(new Insets(10));
        bookBox.setStyle("-fx-background-color: white; -fx-border-color: #EEEEEE; -fx-border-radius: 5px;");

        // Contenitore per le informazioni testuali del libro
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Titolo del libro in evidenza
        Label titleLabel = new Label(book.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(480); // Larghezza massima per evitare troncamenti

        // Informazione sull'autore
        Label authorLabel = new Label("Autore: " + book.getAuthors());
        authorLabel.setStyle("-fx-font-size: 14px;");
        authorLabel.setWrapText(true);

        // Dettagli aggiuntivi del libro
        Label detailsLabel = new Label("Categoria: " + book.getCategory() + " | Editore: " + book.getPublisher() + " | Anno: " + book.getPublishYear());
        detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #777777;");
        detailsLabel.setWrapText(true);

        // Aggiungi le etichette al contenitore delle informazioni
        infoBox.getChildren().addAll(titleLabel, authorLabel, detailsLabel);

        // Crea il pulsante di azione (Aggiungi/Rimuovi)
        Button actionButton = new Button();
        actionButton.setPrefHeight(40);
        actionButton.setPrefWidth(100);
        actionButton.setMinWidth(100);

        // Determina lo stato del libro e configura il pulsante di conseguenza
        boolean isRecommended = recommendedBooks.contains(book.getTitle());
        boolean isLimitReached = recommendedBooks.size() >= 3 && !isRecommended;

        if (isRecommended) {
            // Il libro è già nella lista dei consigliati - mostra opzione per rimuoverlo
            actionButton.setText("Rimuovi");
            actionButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 10px 15px; -fx-cursor: hand;");
            actionButton.setOnAction(e -> toggleBookSelection(book.getTitle(), actionButton));
        } else if (isLimitReached) {
            // Limite massimo di 3 libri raggiunto - pulsante disabilitato
            actionButton.setText("Aggiungi");
            actionButton.setStyle("-fx-background-color: #75B965; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 10px 15px; -fx-cursor: hand;");
            // Mostra un avviso se si tenta di aggiungere oltre il limite
            actionButton.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Limite raggiunto");
                alert.setHeaderText("Limite massimo di libri");
                alert.setContentText("Puoi consigliare al massimo 3 libri. Rimuovi un libro prima di aggiungerne un altro.");
                alert.showAndWait();
            });
        } else {
            // Possibilità di aggiungere ancora libri
            actionButton.setText("Aggiungi");
            actionButton.setStyle("-fx-background-color: #75B965; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 10px 15px; -fx-cursor: hand;");
            actionButton.setOnAction(e -> toggleBookSelection(book.getTitle(), actionButton));
        }

        // Assembla il contenitore finale
        bookBox.getChildren().addAll(infoBox, actionButton);
        return bookBox;
    }

    /**
     * Gestisce la selezione/deselezione di un libro.
     * Alterna lo stato di un libro tra "selezionato" e "non selezionato",
     * aggiungendolo o rimuovendolo dalla lista dei libri consigliati.
     *
     * @param bookTitle Il titolo del libro da selezionare/deselezionare
     * @param button Il pulsante associato al libro per aggiornarne l'aspetto
     */
    private void toggleBookSelection(String bookTitle, Button button) {
        // Verifica se il libro è già selezionato
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

        // Aggiorna l'interfaccia utente
        updateRecommendationsCount();
        updateClearAllButtonState();
        updateSearchResults();
        errorLabel.setVisible(false);
    }

    /**
     * Aggiorna la lista dei libri consigliati nell'interfaccia utente.
     * Ordina alfabeticamente i titoli e li visualizza nella ListView.
     */
    private void updateSelectedBooksList() {
        // Pulisci la ListView
        recommendedBooksListView.getItems().clear();

        // Ordina alfabeticamente i titoli per una migliore presentazione
        List<String> sortedBooks = new ArrayList<>(recommendedBooks);
        Collections.sort(sortedBooks);

        // Aggiorna la ListView con i libri ordinati
        recommendedBooksListView.getItems().addAll(sortedBooks);
    }

    /**
     * Aggiorna il conteggio delle raccomandazioni visualizzato nell'interfaccia.
     * Mostra quanti libri sono stati selezionati su un massimo di 3.
     */
    private void updateRecommendationsCount() {
        recommendationsCountLabel.setText("Selezionati: " + recommendedBooks.size() + "/3");
    }

    /**
     * Aggiorna i pulsanti nei risultati di ricerca in base ai libri attualmente consigliati.
     * Rigenera i risultati di ricerca per riflettere le modifiche nella selezione.
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
     * Rimuove tutti i libri dalla lista dei consigliati e aggiorna l'interfaccia.
     *
     * @param event L'evento che ha scatenato l'azione
     */
    @FXML
    public void handleClearAll(ActionEvent event) {
        if (recommendedBooks.isEmpty()) {
            return;
        }

        // Cancella tutti i libri selezionati
        recommendedBooks.clear();

        // Aggiorna l'interfaccia utente
        updateSelectedBooksList();
        updateRecommendationsCount();
        updateClearAllButtonState();
        updateSearchResults();
        errorLabel.setVisible(false);
    }

    /**
     * Gestisce il salvataggio dei consigli nel database.
     * Se non ci sono libri consigliati, chiede all'utente se desidera tornare indietro.
     * Altrimenti, salva i consigli e torna al menu utente.
     *
     * @param event L'evento che ha scatenato l'azione
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
            // Naviga direttamente al menu utente dopo il salvataggio
            navigateToUserMenu(event);
        }
    }

    /**
     * Salva le raccomandazioni nel database.
     * Prima elimina le raccomandazioni esistenti per il libro selezionato,
     * poi inserisce le nuove raccomandazioni. L'operazione viene eseguita
     * come una transazione per garantire l'integrità dei dati.
     *
     * @return true se il salvataggio è avvenuto con successo, false altrimenti
     */
    private boolean saveRecommendations() {
        try (Connection conn = dbManager.getConnection()) {
            // Disabilita l'auto-commit per gestire l'operazione come una transazione
            conn.setAutoCommit(false);

            try {
                // Prima elimina le raccomandazioni esistenti per questo libro
                String deleteSql = "DELETE FROM book_recommendations WHERE user_id = ? AND source_book_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                    pstmt.setString(1, userId);
                    pstmt.setInt(2, selectedBookId);
                    pstmt.executeUpdate();
                }

                // Poi inserisce le nuove raccomandazioni
                String insertSql = "INSERT INTO book_recommendations (user_id, source_book_id, recommended_book_id) VALUES (?, ?, ?)";
                String findBookSql = "SELECT id FROM books WHERE title = ?";

                try (PreparedStatement findBookStmt = conn.prepareStatement(findBookSql);
                     PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                    // Per ogni libro consigliato, trova il suo ID e inseriscilo nella tabella delle raccomandazioni
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

                // Conferma la transazione
                conn.commit();
                return true;
            } catch (SQLException e) {
                // In caso di errore, annulla tutte le operazioni della transazione
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Mostra l'errore all'utente
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
            return false;
        }
    }

    /**
     * Gestisce il click sul pulsante "Torna al menu".
     * Naviga alla schermata del menu utente principale.
     *
     * @param event L'evento che ha scatenato l'azione
     */
    @FXML
    public void handleBack(ActionEvent event) {
        navigateToUserMenu(event);
    }

    /**
     * Gestisce il click sul pulsante "Annulla".
     * Annulla l'operazione corrente e torna alla schermata di selezione libro.
     *
     * @param event L'evento che ha scatenato l'azione
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        try {
            // Torna alla schermata di selezione libro senza salvare le modifiche
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/selezionaLibro.fxml"));
            Parent root = loader.load();

            // Configura il controller della nuova schermata
            BookSelectionController controller = loader.getController();
            controller.setData(userId, libraryName, "recommend");

            // Visualizza la nuova schermata
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            // Mostra l'errore all'utente
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Naviga alla schermata di selezione della libreria.
     * Carica l'interfaccia di selezione della libreria e passa l'ID dell'utente.
     *
     * @param event L'evento che ha scatenato l'azione
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
            // Mostra l'errore all'utente
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Naviga al menu utente principale.
     * Carica l'interfaccia del menu utente e passa l'ID dell'utente.
     *
     * @param event L'evento che ha scatenato l'azione
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
            // Mostra l'errore all'utente
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }}