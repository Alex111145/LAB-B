package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller per la pagina principale (homepage) dell'applicazione Book Recommender.
 * Gestisce la visualizzazione di libri, la ricerca per titolo, autore o autore e anno,
 * e la navigazione alle pagine di login, registrazione e dettaglio libro.
 *
 * <p>La classe implementa l'interfaccia {@link Initializable} per inizializzare
 * i componenti dell'interfaccia utente quando la pagina viene caricata.</p>
 *
 * @author Book Recommender Team
 * @version 1.0
 */
public class HomepageController implements Initializable {

    /** Pulsante per la scheda di ricerca per titolo */
    @FXML private Button titleTabButton;

    /** Pulsante per la scheda di ricerca per autore */
    @FXML private Button authorTabButton;

    /** Pulsante per la scheda di ricerca per autore e anno */
    @FXML private Button authorYearTabButton;

    /** Contenitore per la pagina di ricerca per titolo */
    @FXML private VBox titlePage;

    /** Contenitore per la pagina di ricerca per autore */
    @FXML private VBox authorPage;

    /** Contenitore per la pagina di ricerca per autore e anno */
    @FXML private VBox authorYearPage;

    /** Campo di testo per la ricerca per titolo */
    @FXML private TextField titleSearchField;

    /** Campo di testo per la ricerca per autore */
    @FXML private TextField authorSearchField;

    /** Campo di testo per la ricerca per autore (nella scheda autore e anno) */
    @FXML private TextField authorYearSearchField;

    /** Campo di testo per la ricerca per anno */
    @FXML private TextField yearSearchField;

    /** Contenitore per la lista dei libri nella scheda titolo */
    @FXML private VBox bookListContainer;

    /** Contenitore per la lista dei libri nella scheda autore */
    @FXML private VBox authorBookListContainer;

    /** Contenitore per la lista dei libri nella scheda autore e anno */
    @FXML private VBox authorYearBookListContainer;

    /** Gestore del database per le operazioni di accesso ai dati */
    private DatabaseManager dbManager;

    /**
     * Metodo chiamato automaticamente dopo che il file FXML è stato caricato.
     * Inizializza i componenti dell'interfaccia utente, configura gli handler
     * per gli eventi e carica i libri con valutazione più alta.
     *
     * @param location L'URL del file FXML
     * @param resources Il ResourceBundle contenente le risorse localizzate
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Gestione dell'eccezione silenziosa
        }

        // Imposta la pagina titolo come visibile di default
        if (titlePage != null) titlePage.setVisible(true);
        if (authorPage != null) authorPage.setVisible(false);
        if (authorYearPage != null) authorYearPage.setVisible(false);

        // Configura gli handler per l'evento keypress sui campi di ricerca
        setupEnterKeyHandlers();

        // Carica i 3 libri più votati nella homepage
        loadTopRatedBooks();
    }

    /**
     * Configura gli handler per la pressione del tasto Invio nei campi di ricerca.
     * Quando l'utente preme Invio in un campo di ricerca, viene avviata
     * automaticamente la ricerca dei libri.
     */
    private void setupEnterKeyHandlers() {
        if (titleSearchField != null) {
            titleSearchField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    cercalibro(new ActionEvent());
                }
            });
        }

        if (authorSearchField != null) {
            authorSearchField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    cercalibro(new ActionEvent());
                }
            });
        }

        if (authorYearSearchField != null) {
            authorYearSearchField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    cercalibro(new ActionEvent());
                }
            });
        }

        if (yearSearchField != null) {
            yearSearchField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    cercalibro(new ActionEvent());
                }
            });
        }
    }

    /**
     * Carica i libri con la valutazione media più alta nella homepage.
     * Visualizza i tre libri con la media di valutazione più alta in tutte
     * le schede di ricerca.
     *
     * <p>Se non ci sono libri con valutazione positiva, viene mostrato un
     * messaggio appropriato.</p>
     */
    private void loadTopRatedBooks() {
        try {
            // Ottieni i libri con valutazione media più alta, limitando a 3
            List<Book> topRatedBooks = BookService.getTopRatedBooks(3);

            // Pulisci i contenitori
            clearBookContainers();

            // Aggiungi l'intestazione "Top 3 libri per valutazione" a tutti i contenitori
            Label titleHeader = new Label("Top 3 libri per valutazione: ");
            titleHeader.setStyle("-fx-font-family: 'Times New Roman'; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: red; -fx-padding: 10px 0 15px 0;");

            Label authorHeader = new Label("Top 3 libri per valutazione: ");
            authorHeader.setStyle("-fx-font-family: 'Times New Roman'; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: red; -fx-padding: 10px 0 15px 0;");

            Label authorYearHeader = new Label("Top 3 libri per valutazione: ");
            authorYearHeader.setStyle("-fx-font-family: 'Times New Roman'; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: red; -fx-padding: 10px 0 15px 0;");

            if (bookListContainer != null) {
                bookListContainer.getChildren().add(titleHeader);
            }

            if (authorBookListContainer != null) {
                authorBookListContainer.getChildren().add(authorHeader);
            }

            if (authorYearBookListContainer != null) {
                authorYearBookListContainer.getChildren().add(authorYearHeader);
            }

            // Se non ci sono libri con valutazione positiva, mostra un messaggio
            if (topRatedBooks.isEmpty()) {
                Label noRatingsLabel = new Label("Nessun libro con valutazioni positive disponibili.");
                noRatingsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555; -fx-padding: 20px;");

                if (bookListContainer != null) {
                    bookListContainer.getChildren().add(noRatingsLabel);
                }

                if (authorBookListContainer != null) {
                    authorBookListContainer.getChildren().add(new Label("Nessun libro con valutazioni positive disponibili."));
                }

                if (authorYearBookListContainer != null) {
                    authorYearBookListContainer.getChildren().add(new Label("Nessun libro con valutazioni positive disponibili."));
                }

                return;
            }

            // Aggiungi i libri ai contenitori
            for (Book book : topRatedBooks) {
                if (bookListContainer != null) {
                    addBookToContainer(book, bookListContainer);
                }

                if (authorBookListContainer != null) {
                    addBookToContainer(book, authorBookListContainer);
                }

                if (authorYearBookListContainer != null) {
                    addBookToContainer(book, authorYearBookListContainer);
                }
            }
        } catch (Exception e) {
            // Gestione dell'eccezione silenziosa
        }
    }

    /**
     * Pulisce i contenitori dei libri, rimuovendo tutti gli elementi
     * precedentemente visualizzati.
     */
    private void clearBookContainers() {
        if (bookListContainer != null) {
            bookListContainer.getChildren().clear();
        }

        if (authorBookListContainer != null) {
            authorBookListContainer.getChildren().clear();
        }

        if (authorYearBookListContainer != null) {
            authorYearBookListContainer.getChildren().clear();
        }
    }

    /**
     * Aggiunge un libro al contenitore specificato con tutte le informazioni.
     * Crea un'interfaccia utente per visualizzare le informazioni del libro
     * in forma di card con titolo, autore, categoria, editore, anno di pubblicazione
     * e un pulsante per visualizzare i dettagli.
     *
     * @param book Il libro da aggiungere al contenitore
     * @param container Il contenitore VBox dove aggiungere il libro
     */
    private void addBookToContainer(Book book, VBox container) {
        if (container == null) return;

        try {
            // Crea il contenitore principale del libro
            VBox bookCard = new VBox(10);
            bookCard.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 5px; -fx-padding: 15px;");
            bookCard.setPrefWidth(container.getWidth() - 30);

            // Crea un HBox principale che conterrà la colonna di informazioni e il pulsante
            HBox mainBox = new HBox();
            mainBox.setAlignment(Pos.CENTER_LEFT);
            mainBox.setSpacing(15);

            // Crea una VBox per titolo e informazioni libro (colonna di sinistra)
            VBox infoColumn = new VBox(10);
            HBox.setHgrow(infoColumn, Priority.ALWAYS);

            // Formatta il titolo del libro
            String originalTitle = book.getTitle();
            String formattedTitle;

            // Tronca il titolo se troppo lungo
            int maxLength = 40;
            if (originalTitle.length() > maxLength) {
                formattedTitle = originalTitle.substring(0, maxLength) + "...";
            } else {
                formattedTitle = originalTitle;
            }

            // Titolo del libro
            Label titleLabel = new Label(formattedTitle);
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4054B2;");
            titleLabel.setUserData(originalTitle);
            titleLabel.setWrapText(true);

            // Informazioni sul libro
            VBox infoBox = new VBox(5);
            infoBox.setPadding(new Insets(10, 0, 0, 0));

            // Autore
            HBox authorBox = new HBox(5);
            Label authorLabelTitle = new Label("Autore:");
            authorLabelTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");
            Label authorLabelValue = new Label(book.getAuthors());
            authorLabelValue.setStyle("-fx-text-fill: #333333;");
            authorBox.getChildren().addAll(authorLabelTitle, authorLabelValue);

            // Categoria
            HBox categoryBox = new HBox(5);
            Label categoryLabelTitle = new Label("Categoria:");
            categoryLabelTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");
            Label categoryLabelValue = new Label(book.getCategory());
            categoryLabelValue.setStyle("-fx-text-fill: #333333;");
            categoryBox.getChildren().addAll(categoryLabelTitle, categoryLabelValue);

            // Editore
            HBox publisherBox = new HBox(5);
            Label publisherLabelTitle = new Label("Editore:");
            publisherLabelTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");
            Label publisherLabelValue = new Label(book.getPublisher());
            publisherLabelValue.setStyle("-fx-text-fill: #333333;");
            publisherBox.getChildren().addAll(publisherLabelTitle, publisherLabelValue);

            // Anno di pubblicazione
            HBox yearBox = new HBox(5);
            Label yearLabelTitle = new Label("Anno:");
            yearLabelTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");
            Label yearLabelValue = new Label(String.valueOf(book.getPublishYear()));
            yearLabelValue.setStyle("-fx-text-fill: #333333;");
            yearBox.getChildren().addAll(yearLabelTitle, yearLabelValue);

            // Aggiungi tutte le informazioni al box delle info
            infoBox.getChildren().addAll(authorBox, categoryBox, publisherBox, yearBox);

            // Aggiungi titolo e info alla colonna di sinistra
            infoColumn.getChildren().addAll(titleLabel, infoBox);

            // Crea un VBox per allineare verticalmente il pulsante
            VBox buttonColumn = new VBox();
            buttonColumn.setAlignment(Pos.CENTER);

            // Pulsante "Visualizza"
            Button viewButton = new Button("Visualizza");
            viewButton.setStyle("-fx-text-fill: white; -fx-background-color: #75B965; -fx-background-radius: 40px; -fx-padding: 8px 15px;");
            viewButton.setOnAction(event -> visualizzalibro(event, originalTitle));

            // Aggiungi il pulsante alla colonna di destra
            buttonColumn.getChildren().add(viewButton);

            // Aggiungi le due colonne al box principale
            mainBox.getChildren().addAll(infoColumn, buttonColumn);

            // Aggiungi il box principale alla scheda del libro
            bookCard.getChildren().add(mainBox);

            // Aggiungi la carta del libro al contenitore principale
            container.getChildren().add(bookCard);

        } catch (Exception e) {
            // In caso di errore, crea un elemento semplificato
            Label errorLabel = new Label("Errore nel caricamento del libro: " + book.getTitle());
            errorLabel.setStyle("-fx-text-fill: red;");
            container.getChildren().add(errorLabel);
        }
    }

    /**
     * Gestisce il click sul pulsante "Login".
     * Reindirizza l'utente alla pagina di login.
     *
     * @param event L'evento che ha triggerato l'azione
     */
    @FXML
    public void entrainlogin(ActionEvent event) {
        try {
            String fxmlFile = "/book_recommender/lab_b/login.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));

            if (getClass().getResource(fxmlFile) == null) {
                throw new IOException("File FXML non trovato: " + fxmlFile);
            }

            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            // Gestione dell'eccezione silenziosa
        }
    }

    /**
     * Gestisce il click sul pulsante "Registrazione".
     * Reindirizza l'utente alla pagina di registrazione.
     *
     * @param event L'evento che ha triggerato l'azione
     */
    @FXML
    public void entrainregistrazione(ActionEvent event) {
        try {
            String fxmlFile = "/book_recommender/lab_b/registrazione.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));

            if (getClass().getResource(fxmlFile) == null) {
                throw new IOException("File FXML non trovato: " + fxmlFile);
            }

            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            // Gestione dell'eccezione silenziosa
        }
    }

    /**
     * Gestisce il click sulla scheda di ricerca per titolo.
     * Mostra la pagina di ricerca per titolo e aggiorna lo stile dei pulsanti.
     *
     * @param event L'evento che ha triggerato l'azione
     */
    @FXML
    public void onTitleTabClicked(ActionEvent event) {
        titlePage.setVisible(true);
        authorPage.setVisible(false);
        authorYearPage.setVisible(false);

        // Aggiorna lo stile dei pulsanti
        titleTabButton.setStyle("-fx-background-color: #4E90E2; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 0;");
        authorTabButton.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 14px; -fx-background-radius: 0;");
        authorYearTabButton.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 14px; -fx-background-radius: 0;");
    }

    /**
     * Gestisce il click sulla scheda di ricerca per autore.
     * Mostra la pagina di ricerca per autore e aggiorna lo stile dei pulsanti.
     *
     * @param event L'evento che ha triggerato l'azione
     */
    @FXML
    public void onAuthorTabClicked(ActionEvent event) {
        titlePage.setVisible(false);
        authorPage.setVisible(true);
        authorYearPage.setVisible(false);

        // Aggiorna lo stile dei pulsanti
        titleTabButton.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 14px; -fx-background-radius: 0;");
        authorTabButton.setStyle("-fx-background-color: #4E90E2; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 0;");
        authorYearTabButton.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 14px; -fx-background-radius: 0;");
    }

    /**
     * Gestisce il click sulla scheda di ricerca per autore e anno.
     * Mostra la pagina di ricerca per autore e anno e aggiorna lo stile dei pulsanti.
     *
     * @param event L'evento che ha triggerato l'azione
     */
    @FXML
    public void onAuthorYearTabClicked(ActionEvent event) {
        titlePage.setVisible(false);
        authorPage.setVisible(false);
        authorYearPage.setVisible(true);

        // Aggiorna lo stile dei pulsanti
        titleTabButton.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 14px; -fx-background-radius: 0;");
        authorTabButton.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 14px; -fx-background-radius: 0;");
        authorYearTabButton.setStyle("-fx-background-color: #4E90E2; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 0;");
    }

    /**
     * Gestisce la ricerca dei libri in base alla scheda attualmente visibile.
     * Esegue una ricerca diversa in base alla scheda selezionata (titolo, autore o autore e anno).
     * Se i campi di ricerca sono vuoti, vengono mostrati i libri più votati.
     *
     * @param event L'evento che ha triggerato l'azione
     */
    @FXML
    public void cercalibro(ActionEvent event) {
        // Determina quale tab è attualmente visibile
        if (titlePage != null && titlePage.isVisible()) {
            // Ricerca per titolo
            String searchTitle = titleSearchField.getText().trim();
            if (!searchTitle.isEmpty()) {
                List<Book> results = BookService.searchBooksByTitle(searchTitle);
                displaySearchResults(results, bookListContainer);
            } else {
                loadTopRatedBooks();
            }
        } else if (authorPage != null && authorPage.isVisible()) {
            // Ricerca per autore
            String searchAuthor = authorSearchField.getText().trim();
            if (!searchAuthor.isEmpty()) {
                List<Book> results = BookService.searchBooksByAuthor(searchAuthor);
                displaySearchResults(results, authorBookListContainer);
            } else {
                loadTopRatedBooks();
            }
        } else if (authorYearPage != null && authorYearPage.isVisible()) {
            // Ricerca per autore e anno
            String searchAuthor = authorYearSearchField.getText().trim();
            String yearString = yearSearchField.getText().trim();

            if (!searchAuthor.isEmpty() && !yearString.isEmpty()) {
                try {
                    int year = Integer.parseInt(yearString);
                    List<Book> results = BookService.searchBooksByAuthorAndYear(searchAuthor, year);
                    displaySearchResults(results, authorYearBookListContainer);
                } catch (NumberFormatException e) {
                    // Gestione dell'eccezione silenziosa
                }
            } else {
                loadTopRatedBooks();
            }
        }
    }

    /**
     * Mostra i risultati della ricerca nel contenitore specificato.
     * Se non ci sono risultati, mostra un messaggio appropriato.
     *
     * @param books La lista dei libri da visualizzare
     * @param container Il contenitore VBox dove visualizzare i risultati
     */
    private void displaySearchResults(List<Book> books, VBox container) {
        if (container == null) return;

        // Pulisci il contenitore
        container.getChildren().clear();

        if (books.isEmpty()) {
            // Se non ci sono risultati, mostra un messaggio
            Label noResultsLabel = new Label("Nessun libro trovato.");
            noResultsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555; -fx-padding: 20px;");
            container.getChildren().add(noResultsLabel);
        } else {
            // Se ci sono risultati, aggiungi l'intestazione "Risultati della ricerca"
            Label searchResultsHeader = new Label("Risultati della ricerca: ");
            searchResultsHeader.setStyle("-fx-font-family: 'Times New Roman'; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: red; -fx-padding: 10px 0 15px 0;");
            container.getChildren().add(searchResultsHeader);

            // Aggiungi i libri al contenitore
            for (Book book : books) {
                addBookToContainer(book, container);
            }
        }
    }

    /**
     * Versione sovraccarica del metodo per gestire la visualizzazione dei dettagli di un libro.
     * Chiama il metodo navigateToBookDetails per navigare alla pagina di dettaglio del libro.
     *
     * @param event L'evento che ha triggerato l'azione
     * @param bookTitle Il titolo del libro di cui visualizzare i dettagli
     */
    public void visualizzalibro(ActionEvent event, String bookTitle) {
        navigateToBookDetails(event, bookTitle);
    }

    /**
     * Naviga alla pagina di dettaglio del libro.
     * Carica la vista stampadettaglinologin.fxml e imposta il titolo del libro
     * per visualizzarne i dettagli.
     *
     * @param event L'evento che ha triggerato l'azione
     * @param bookTitle Il titolo del libro di cui visualizzare i dettagli
     */
    private void navigateToBookDetails(ActionEvent event, String bookTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/stampadettaglinologin.fxml"));
            Parent root = loader.load();

            BookDetailsController controller = loader.getController();
            controller.setBookData(bookTitle);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            // Gestione dell'eccezione silenziosa
        }
    }
}