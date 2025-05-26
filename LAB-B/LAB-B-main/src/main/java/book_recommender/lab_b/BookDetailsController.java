package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Separator;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller per la visualizzazione dettagliata di un libro.
 * Gestisce la visualizzazione delle informazioni del libro, le valutazioni, le recensioni degli utenti,
 * e i libri consigliati correlati al libro visualizzato.
 * <p>
 * Questa classe implementa {@link Initializable} per inizializzare i componenti JavaFX
 * e gestisce l'interazione con il database per recuperare e visualizzare i dati.
 * </p>
 *
 * @author Book Recommender Team
 * @version 1.0
 */
public class BookDetailsController implements Initializable {

    /** Label per il titolo del libro */
    @FXML private Label bookTitleLabel;
    /** Label per gli autori del libro */
    @FXML private Label authorsLabel;
    /** Label per la categoria del libro */
    @FXML private Label categoryLabel;
    /** Label per l'editore del libro */
    @FXML private Label publisherLabel;
    /** Label per l'anno di pubblicazione */
    @FXML private Label yearLabel;
    /** Label per la valutazione dello stile */
    @FXML private Label styleRatingLabel;
    /** Label per la valutazione del contenuto */
    @FXML private Label contentRatingLabel;
    /** Label per la valutazione della gradevolezza */
    @FXML private Label pleasantnessRatingLabel;
    /** Label per la valutazione dell'originalità */
    @FXML private Label originalityRatingLabel;
    /** Label per la valutazione dell'edizione */
    @FXML private Label editionRatingLabel;
    /** Label per la valutazione totale */
    @FXML private Label totalRatingLabel;
    /** Label per il numero di utenti che hanno valutato */
    @FXML private Label usersCountLabel;
    /** Label per i commenti generali */
    @FXML private Label generalCommentsLabel;
    /** Label per i libri consigliati */
    @FXML private Label recommendedBooksLabel;
    /** Pulsante per tornare alla pagina precedente */
    @FXML private Button backButton;

    /** Container per le recensioni sullo stile */
    @FXML private VBox styleReviewsBox;
    /** Container per le recensioni sul contenuto */
    @FXML private VBox contentReviewsBox;
    /** Container per le recensioni sulla gradevolezza */
    @FXML private VBox pleasantnessReviewsBox;
    /** Container per le recensioni sull'originalità */
    @FXML private VBox originalityReviewsBox;
    /** Container per le recensioni sull'edizione */
    @FXML private VBox editionReviewsBox;

    /** Riferimenti alle stelle per la valutazione dello stile */
    @FXML private Text styleStar1, styleStar2, styleStar3, styleStar4, styleStar5;
    /** Riferimenti alle stelle per la valutazione del contenuto */
    @FXML private Text contentStar1, contentStar2, contentStar3, contentStar4, contentStar5;
    /** Riferimenti alle stelle per la valutazione della gradevolezza */
    @FXML private Text pleasantnessStar1, pleasantnessStar2, pleasantnessStar3, pleasantnessStar4, pleasantnessStar5;
    /** Riferimenti alle stelle per la valutazione dell'originalità */
    @FXML private Text originalityStar1, originalityStar2, originalityStar3, originalityStar4, originalityStar5;
    /** Riferimenti alle stelle per la valutazione dell'edizione */
    @FXML private Text editionStar1, editionStar2, editionStar3, editionStar4, editionStar5;
    /** Riferimenti alle stelle per la valutazione totale */
    @FXML private Text totalStar1, totalStar2, totalStar3, totalStar4, totalStar5;

    /** Mappa dei colori assegnati a ciascun utente per la visualizzazione coerente */
    private final Map<String, String> userColors = new HashMap<>();

    /** Palette di colori utilizzata per assegnare colori agli utenti */
    private final String[] colorPalette = {
            "#e74c3c", "#3498db", "#2ecc71", "#f39c12", "#9b59b6", "#1abc9c", "#d35400",
            "#27ae60", "#2980b9", "#8e44ad", "#f1c40f", "#16a085", "#2c3e50", "#f39c12",
            "#e67e22", "#95a5a6", "#bdc3c7", "#7f8c8d", "#34495e", "#d5dbdb", "#9a59b6",
            "#f5b041", "#58d68d", "#5dade2", "#f1948a", "#d6eaf8", "#f7b7a3", "#e8daef", "#c39bd3"
    };


    /**
     * Classe interna per rappresentare una recensione di un libro.
     * Contiene l'ID dell'utente, la valutazione e il commento.
     */
    private static class Review {
        /** ID dell'utente che ha lasciato la recensione */
        public String userId;
        /** Valutazione numerica (da 1 a 5) */
        public int rating;
        /** Testo del commento */
        public String comment;

        /**
         * Costruttore per una recensione.
         *
         * @param userId ID dell'utente che ha scritto la recensione
         * @param rating Valutazione numerica del libro (1-5)
         * @param comment Testo del commento
         */
        public Review(String userId, int rating, String comment) {
            this.userId = userId;
            this.rating = rating;
            this.comment = comment;
        }
    }

    /**
     * Classe interna per rappresentare un commento generale sul libro.
     * Contiene l'ID dell'utente e il testo del commento.
     */
    private static class Comment {
        /** ID dell'utente che ha lasciato il commento */
        public String userId;
        /** Testo del commento */
        public String text;

        /**
         * Costruttore per un commento generale.
         *
         * @param userId ID dell'utente che ha scritto il commento
         * @param text Testo del commento
         */
        public Comment(String userId, String text) {
            this.userId = userId;
            this.text = text;
        }
    }

    /**
     * Classe interna per rappresentare un libro consigliato.
     * Contiene l'ID dell'utente che ha fatto la raccomandazione e il titolo del libro consigliato.
     */
    private static class RecommendedBook {
        /** ID dell'utente che ha consigliato il libro */
        public String userId;
        /** Titolo del libro consigliato */
        public String bookTitle;

        /**
         * Costruttore per un libro consigliato.
         *
         * @param userId ID dell'utente che ha consigliato il libro
         * @param bookTitle Titolo del libro consigliato
         */
        public RecommendedBook(String userId, String bookTitle) {
            this.userId = userId;
            this.bookTitle = bookTitle;
        }
    }

    /** Liste per memorizzare le recensioni per ogni caratteristica del libro */
    private final List<Review> styleReviews = new ArrayList<>();
    private final List<Review> contentReviews = new ArrayList<>();
    private final List<Review> pleasantnessReviews = new ArrayList<>();
    private final List<Review> originalityReviews = new ArrayList<>();
    private final List<Review> editionReviews = new ArrayList<>();

    /** Dati del libro corrente */
    private Book currentBook;

    /** ID del libro corrente nel database */
    private int currentBookId;

    /** Mappa per memorizzare le valutazioni del libro per ciascuna categoria */
    private final Map<String, Double> ratings = new HashMap<>();

    /** Numero di utenti che hanno valutato il libro */
    private int numRaters = 0;

    /** Manager per la connessione al database */
    private DatabaseManager dbManager;

    /**
     * Inizializza il controller. Questo metodo viene chiamato automaticamente dopo che
     * il file FXML è stato caricato.
     *
     * @param location La posizione utilizzata per risolvere percorsi relativi per l'oggetto root
     * @param resources Le risorse utilizzate per localizzare l'oggetto root
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Silenziosamente ignorato - il database potrebbe non essere disponibile
        }

        // Inizializza i container delle recensioni con spaziatura
        styleReviewsBox.setSpacing(10);
        contentReviewsBox.setSpacing(10);
        pleasantnessReviewsBox.setSpacing(10);
        originalityReviewsBox.setSpacing(10);
        editionReviewsBox.setSpacing(10);
    }

    /**
     * Imposta i dati del libro da visualizzare e carica tutte le informazioni correlate.
     * Cerca il libro nel database e, se trovato, carica le valutazioni e le recensioni.
     * Se il libro non viene trovato, utilizza un libro di esempio con dati predefiniti.
     *
     * @param bookTitle Il titolo del libro da visualizzare
     */
    public void setBookData(String bookTitle) {
        // Cerca il libro nel database
        this.currentBook = findBookByTitle(bookTitle);

        if (currentBook != null) {
            // Carica le valutazioni e le recensioni del libro
            loadRatingsAndReviews(currentBookId);

            // Popola l'interfaccia con i dati del libro
            updateUI();
        } else {
            // Crea un libro di esempio se non trovato
            this.currentBook = createExampleBook(bookTitle);

            // Utilizza valutazioni di esempio
            setupExampleRatings();

            // Popola l'interfaccia con i dati del libro di esempio
            updateUI();
        }
    }

    /**
     * Crea un oggetto libro di esempio quando il libro richiesto non viene trovato nel database.
     * Utilizza valori predefiniti per gli attributi mancanti.
     *
     * @param title Il titolo del libro da creare
     * @return Un oggetto Book con valori predefiniti
     */
    private Book createExampleBook(String title) {
        return new Book(
                title,
                "Autore sconosciuto",
                "Non categorizzato",
                "Editore sconosciuto",
                2000
        );
    }

    /**
     * Inizializza le valutazioni di esempio con valori predefiniti (zero) e pulisce
     * tutte le liste di recensioni. Utilizzato quando il libro non è presente nel database.
     */
    private void setupExampleRatings() {
        ratings.put("style", 0.0);
        ratings.put("content", 0.0);
        ratings.put("pleasantness", 0.0);
        ratings.put("originality", 0.0);
        ratings.put("edition", 0.0);
        ratings.put("total", 0.0);
        numRaters = 0;

        // Pulisci le liste di recensioni
        styleReviews.clear();
        contentReviews.clear();
        pleasantnessReviews.clear();
        originalityReviews.clear();
        editionReviews.clear();
    }

    /**
     * Cerca un libro nel database in base al titolo.
     * Se trovato, memorizza l'ID del libro e restituisce un oggetto Book con i dettagli.
     *
     * @param title Il titolo del libro da cercare
     * @return Un oggetto Book se trovato, altrimenti null
     */
    private Book findBookByTitle(String title) {
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT id, title, authors, category, publisher, publish_year FROM books WHERE title = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, title);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    this.currentBookId = rs.getInt("id");
                    return new Book(
                            rs.getString("title"),
                            rs.getString("authors"),
                            rs.getString("category"),
                            rs.getString("publisher"),
                            rs.getInt("publish_year")
                    );
                }
            }
        } catch (SQLException e) {
            // Gestione errore silenziosa
        }

        return null;
    }

    /**
     * Carica dal database le valutazioni e le recensioni associate al libro specificato.
     * Calcola la media delle valutazioni e organizza le recensioni per categoria.
     *
     * @param bookId L'ID del libro di cui caricare valutazioni e recensioni
     */
    private void loadRatingsAndReviews(int bookId) {
        // Inizializza le valutazioni con valori predefiniti
        ratings.put("style", 0.0);
        ratings.put("content", 0.0);
        ratings.put("pleasantness", 0.0);
        ratings.put("originality", 0.0);
        ratings.put("edition", 0.0);
        ratings.put("total", 0.0);

        // Pulisci le liste di recensioni
        styleReviews.clear();
        contentReviews.clear();
        pleasantnessReviews.clear();
        originalityReviews.clear();
        editionReviews.clear();

        try (Connection conn = dbManager.getConnection()) {
            // Query per ottenere tutte le valutazioni per questo libro
            String sql = "SELECT user_id, style_rating, content_rating, pleasantness_rating, " +
                    "originality_rating, edition_rating, general_comment, style_comment, " +
                    "content_comment, pleasantness_comment, originality_comment, edition_comment " +
                    "FROM book_ratings WHERE book_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, bookId);
                ResultSet rs = pstmt.executeQuery();

                int count = 0;
                double styleSum = 0, contentSum = 0, pleasantnessSum = 0, originalitySum = 0, editionSum = 0;
                int colorIndex = 0;

                while (rs.next()) {
                    String userId = rs.getString("user_id");
                    int style = rs.getInt("style_rating");
                    int content = rs.getInt("content_rating");
                    int pleasantness = rs.getInt("pleasantness_rating");
                    int originality = rs.getInt("originality_rating");
                    int edition = rs.getInt("edition_rating");

                    // Assegna un colore all'utente se non ne ha già uno
                    if (!userColors.containsKey(userId)) {
                        userColors.put(userId, colorPalette[colorIndex % colorPalette.length]);
                        colorIndex++;
                    }

                    // Somma le valutazioni per il calcolo della media
                    styleSum += style;
                    contentSum += content;
                    pleasantnessSum += pleasantness;
                    originalitySum += originality;
                    editionSum += edition;
                    count++;

                    // Aggiungi recensioni specifiche se disponibili
                    String generalComment = rs.getString("general_comment");
                    String styleComment = rs.getString("style_comment");
                    String contentComment = rs.getString("content_comment");
                    String pleasantnessComment = rs.getString("pleasantness_comment");
                    String originalityComment = rs.getString("originality_comment");
                    String editionComment = rs.getString("edition_comment");

                    // Aggiungi recensioni per lo stile
                    if (styleComment != null && !styleComment.isEmpty()) {
                        styleReviews.add(new Review(userId, style, styleComment));
                    } else if (generalComment != null && !generalComment.isEmpty()) {
                        styleReviews.add(new Review(userId, style, generalComment));
                    }

                    // Aggiungi recensioni per il contenuto
                    if (contentComment != null && !contentComment.isEmpty()) {
                        contentReviews.add(new Review(userId, content, contentComment));
                    } else if (generalComment != null && !generalComment.isEmpty()) {
                        contentReviews.add(new Review(userId, content, generalComment));
                    }

                    // Aggiungi recensioni per la gradevolezza
                    if (pleasantnessComment != null && !pleasantnessComment.isEmpty()) {
                        pleasantnessReviews.add(new Review(userId, pleasantness, pleasantnessComment));
                    } else if (generalComment != null && !generalComment.isEmpty()) {
                        pleasantnessReviews.add(new Review(userId, pleasantness, generalComment));
                    }

                    // Aggiungi recensioni per l'originalità
                    if (originalityComment != null && !originalityComment.isEmpty()) {
                        originalityReviews.add(new Review(userId, originality, originalityComment));
                    } else if (generalComment != null && !generalComment.isEmpty()) {
                        originalityReviews.add(new Review(userId, originality, generalComment));
                    }

                    // Aggiungi recensioni per l'edizione
                    if (editionComment != null && !editionComment.isEmpty()) {
                        editionReviews.add(new Review(userId, edition, editionComment));
                    } else if (generalComment != null && !generalComment.isEmpty()) {
                        editionReviews.add(new Review(userId, edition, generalComment));
                    }
                }

                // Se ci sono valutazioni, calcola la media
                if (count > 0) {
                    ratings.put("style", Math.round(styleSum / count * 10) / 10.0);
                    ratings.put("content", Math.round(contentSum / count * 10) / 10.0);
                    ratings.put("pleasantness", Math.round(pleasantnessSum / count * 10) / 10.0);
                    ratings.put("originality", Math.round(originalitySum / count * 10) / 10.0);
                    ratings.put("edition", Math.round(editionSum / count * 10) / 10.0);

                    // Calcola la media totale
                    double total = (styleSum + contentSum + pleasantnessSum + originalitySum + editionSum) / (5 * count);
                    ratings.put("total", Math.round(total * 10) / 10.0);

                    // Aggiorna il numero di valutatori
                    numRaters = count;
                } else {
                    // Se non ci sono valutazioni, imposta tutto a 0
                    setupExampleRatings();
                }
            }
        } catch (SQLException e) {
            // In caso di errore, imposta valutazioni di esempio
            setupExampleRatings();
        }
    }

    /**
     * Aggiorna l'aspetto di una singola stella in base al valore della valutazione.
     * Gestisce le mezze stelle per visualizzare valutazioni con decimali.
     *
     * @param star Il controllo Text della stella da aggiornare
     * @param rating Il valore della valutazione
     * @param position La posizione della stella (da 1 a 5)
     */
    private void updateSingleStar(Text star, double rating, int position) {
        Color goldStarColor = Color.web("#FFD700");  // Colore giallo oro per le stelle
        Color grayStarColor = Color.web("#dddddd");  // Colore grigio per stelle vuote

        // Calcola la parte decimale del rating per questa posizione
        double decimalPart = rating - (position - 1);

        if (decimalPart >= 1.0) {
            // Stella piena
            star.setText("★");
            star.setFill(goldStarColor);
        } else if (decimalPart >= 0.5) {
            // Mezza stella - per valori tra 0.5 e 0.9
            star.setText("☆");
            star.setFill(goldStarColor);
        } else if (decimalPart > 0) {
            // Stella vuota con colore dorato per valori tra 0.1 e 0.4
            star.setText("★");
            star.setFill(grayStarColor);
        } else {
            // Stella vuota con colore grigio
            star.setText("★");
            star.setFill(grayStarColor);
        }
    }

    /**
     * Aggiorna l'aspetto di un gruppo di stelle per visualizzare una valutazione.
     * Utilizza il metodo updateSingleStar per aggiornare ogni stella individualmente.
     *
     * @param star1 Prima stella del gruppo
     * @param star2 Seconda stella del gruppo
     * @param star3 Terza stella del gruppo
     * @param star4 Quarta stella del gruppo
     * @param star5 Quinta stella del gruppo
     * @param rating Valore della valutazione da visualizzare
     */
    private void updateStars(Text star1, Text star2, Text star3, Text star4, Text star5, double rating) {
        // Aggiorna ogni stella in base alla posizione
        updateSingleStar(star1, rating, 1);
        updateSingleStar(star2, rating, 2);
        updateSingleStar(star3, rating, 3);
        updateSingleStar(star4, rating, 4);
        updateSingleStar(star5, rating, 5);
    }

    /**
     * Aggiorna tutte le stelle nel riepilogo valutazioni.
     * Richiama il metodo updateStars per ciascun gruppo di stelle.
     */
    private void updateAllRatingStars() {
        // Aggiorna tutte le stelle per ciascuna categoria
        updateStars(styleStar1, styleStar2, styleStar3, styleStar4, styleStar5, ratings.get("style"));
        updateStars(contentStar1, contentStar2, contentStar3, contentStar4, contentStar5, ratings.get("content"));
        updateStars(pleasantnessStar1, pleasantnessStar2, pleasantnessStar3, pleasantnessStar4, pleasantnessStar5,
                ratings.get("pleasantness"));
        updateStars(originalityStar1, originalityStar2, originalityStar3, originalityStar4, originalityStar5,
                ratings.get("originality"));
        updateStars(editionStar1, editionStar2, editionStar3, editionStar4, editionStar5, ratings.get("edition"));
        updateStars(totalStar1, totalStar2, totalStar3, totalStar4, totalStar5, ratings.get("total"));
    }

    /**
     * Aggiunge stelle a un HBox per visualizzare la valutazione nelle recensioni individuali.
     *
     * @param starsBox Il contenitore HBox in cui aggiungere le stelle
     * @param rating Il valore della valutazione da visualizzare
     */
    private void addStarsToReviewHeader(HBox starsBox, double rating) {
        // Colori per le stelle
        final Color goldStarColor = Color.web("#FFD700"); // Giallo oro per le stelle piene e mezze stelle
        final Color grayStarColor = Color.web("#dddddd"); // Grigio per stelle vuote

        for (int i = 1; i <= 5; i++) {
            Text star = new Text();
            star.setStyle("-fx-font-size: 14px;");

            // Calcola la parte decimale del rating per questa posizione
            double decimalPart = rating - (i - 1);

            if (decimalPart >= 1.0) {
                // Stella piena
                star.setText("★");
                star.setFill(goldStarColor);
            } else if (decimalPart >= 0.5) {
                // Mezza stella
                star.setText("☆");
                star.setFill(goldStarColor);
            } else if (decimalPart > 0) {
                // Stella vuota ma colorata
                star.setText("★");
                star.setFill(grayStarColor);
            } else {
                // Stella vuota
                star.setText("★");
                star.setFill(grayStarColor);
            }

            starsBox.getChildren().add(star);
        }
    }

    /**
     * Aggiunge le recensioni degli utenti al container specificato.
     * Visualizza tutte le recensioni per una specifica caratteristica del libro, mostrando
     * l'utente che ha scritto la recensione, la valutazione in stelle e il testo completo del commento.
     * Se non ci sono recensioni disponibili, viene mostrato un messaggio appropriato.
     *
     * @param reviews La lista di recensioni da visualizzare
     * @param container Il contenitore VBox in cui aggiungere le recensioni
     */
    private void addReviewsToContainer(List<Review> reviews, VBox container) {
        if (reviews.isEmpty()) {
            Label noReviewsLabel = new Label("Nessuna recensione disponibile.");
            noReviewsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #777777;");
            container.getChildren().add(noReviewsLabel);
            return;
        }

        for (Review review : reviews) {
            // Crea un box per la recensione
            VBox reviewBox = new VBox(5);
            reviewBox.setPadding(new Insets(10));
            reviewBox.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #e0e0e0; -fx-border-radius: 5px;");

            // Intestazione con utente e valutazione
            HBox headerBox = new HBox(10);
            headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Usa il colore assegnato all'utente
            String userColor = userColors.getOrDefault(review.userId, "#333333");

            Label userLabel = new Label(review.userId);
            userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + userColor + ";");

            // Aggiungi stelle per la valutazione con supporto per mezze stelle
            HBox starsBox = new HBox(2);

            // Utilizza il metodo per aggiungere le stelle
            addStarsToReviewHeader(starsBox, review.rating);

            // Aggiungi username e stelle all'header
            headerBox.getChildren().addAll(userLabel, starsBox);

            // Corpo della recensione - mostra sempre il commento completo
            Label commentLabel = new Label(review.comment);
            commentLabel.setWrapText(true);
            commentLabel.setStyle("-fx-padding: 5 0 0 0;");

            // Aggiungi tutto al container della recensione
            reviewBox.getChildren().addAll(headerBox, commentLabel);
            container.getChildren().add(reviewBox);
        }
    }

    /**
     * Genera e visualizza i commenti generali degli utenti sul libro.
     * Questo metodo crea un contenitore che mostra tutti i commenti generali lasciati dagli utenti,
     * con l'indicazione dell'autore del commento e la sua valutazione media del libro.
     * Ogni commento è colorato in base all'utente per una facile identificazione visiva.
     * Se non ci sono commenti disponibili, viene mostrato un messaggio appropriato.
     */
    private void generateGeneralComment() {
        // Pulisci prima il container dei commenti generali
        generalCommentsLabel.setText("");

        // Definisci i colori per le stelle
        final Color goldStarColor = Color.web("#FFD700"); // Giallo oro per stelle piene e mezze stelle
        final Color grayStarColor = Color.web("#dddddd"); // Grigio per stelle vuote

        // Crea un container per i commenti se non esiste già
        VBox commentsContainer;
        if (generalCommentsLabel.getParent() instanceof VBox) {
            commentsContainer = (VBox) generalCommentsLabel.getParent();
            commentsContainer.getChildren().clear();
        } else {
            // Crea un nuovo container e sostituisci l'etichetta originale
            commentsContainer = new VBox(10);
            commentsContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Sostituisci l'etichetta originale con il container nel layout
            VBox parentContainer = (VBox) generalCommentsLabel.getParent();
            int index = parentContainer.getChildren().indexOf(generalCommentsLabel);
            if (index >= 0) {
                parentContainer.getChildren().set(index, commentsContainer);
            }
        }

        // Aggiungi l'intestazione
        Label headerLabel = new Label();
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        headerLabel.setText("Commenti degli utenti:");
        commentsContainer.getChildren().add(headerLabel);

        // Aggiungi il sommario delle valutazioni
        Label summaryLabel = new Label();
        if (numRaters <= 0) {
            summaryLabel.setText("Nessuna recensione disponibile per questo libro.");
        }
        summaryLabel.setWrapText(true);
        commentsContainer.getChildren().add(summaryLabel);

        // Aggiungi i commenti degli utenti
        if (numRaters > 0) {
            // Recupera i commenti generali dal database
            List<Comment> userComments = getUserComments(currentBookId);

            if (!userComments.isEmpty()) {
                // Aggiungi ogni commento colorato al container
                for (Comment comment : userComments) {
                    // Ottieni la valutazione media dell'utente per questo libro
                    double userRating = getUserRatingForBook(comment.userId, currentBookId);

                    // Crea un box per ogni commento
                    VBox commentBox = new VBox(5);
                    commentBox.setPadding(new Insets(10));
                    commentBox.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #e0e0e0; -fx-border-radius: 5px;");

                    // Usa il colore assegnato all'utente
                    String userColor = userColors.getOrDefault(comment.userId, "#333333");

                    // Crea un HBox per contenere l'ID utente e le stelle
                    HBox userHeader = new HBox(10);
                    userHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    // Crea etichetta per l'utente
                    Label userLabel = new Label(comment.userId);
                    userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + userColor + ";");
                    userHeader.getChildren().add(userLabel);

                    // Aggiungi stelle per la valutazione con supporto per mezze stelle
                    if (userRating > 0) {
                        HBox starsBox = new HBox(2);
                        addStarsToReviewHeader(starsBox, userRating);
                        userHeader.getChildren().add(starsBox);
                    }

                    // Crea etichetta per il commento - mostra sempre il commento completo
                    Label textLabel = new Label(comment.text);
                    textLabel.setWrapText(true);
                    textLabel.setStyle("-fx-font-style: italic;");

                    commentBox.getChildren().addAll(userHeader, textLabel);
                    commentsContainer.getChildren().add(commentBox);
                }
            } else {
                Label noCommentsLabel = new Label("Nessun commento disponibile.");
                noCommentsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #777777;");
                commentsContainer.getChildren().add(noCommentsLabel);
            }
        }
    }

    /**
     * Aggiorna l'interfaccia utente con tutti i dati del libro corrente.
     * Questo metodo popola tutti i componenti dell'interfaccia con i dati del libro,
     * inclusi titolo, autori, categoria, editore, anno di pubblicazione, valutazioni,
     * recensioni e libri consigliati.
     * <p>
     * Viene chiamato dopo che i dati del libro sono stati caricati dal database
     * o dopo che è stato creato un libro di esempio.
     * </p>
     */
    private void updateUI() {
        // Imposta i dati del libro
        bookTitleLabel.setText(currentBook.getTitle());
        authorsLabel.setText(currentBook.getAuthors());
        categoryLabel.setText(currentBook.getCategory());
        publisherLabel.setText(currentBook.getPublisher());
        yearLabel.setText(String.valueOf(currentBook.getPublishYear()));

        // Imposta le valutazioni numeriche
        styleRatingLabel.setText(String.valueOf(ratings.get("style")));
        contentRatingLabel.setText(String.valueOf(ratings.get("content")));
        pleasantnessRatingLabel.setText(String.valueOf(ratings.get("pleasantness")));
        originalityRatingLabel.setText(String.valueOf(ratings.get("originality")));
        editionRatingLabel.setText(String.valueOf(ratings.get("edition")));
        totalRatingLabel.setText(String.valueOf(ratings.get("total")));

        // Aggiorna il numero di valutatori
        usersCountLabel.setText(String.valueOf(numRaters));

        // Aggiorna tutte le stelle
        updateAllRatingStars();

        // Genera un commento generale basato sulle valutazioni
        generateGeneralComment();

        // Aggiorna i container delle recensioni
        updateReviewContainers();

        // Genera libri consigliati basati sulla categoria del libro attuale
        generateRecommendedBooks();
    }

    /**
     * Aggiorna i container delle recensioni con i dati più recenti.
     * Questo metodo pulisce i container esistenti e li popola con le recensioni
     * specifiche per ogni caratteristica del libro (stile, contenuto, gradevolezza,
     * originalità ed edizione).
     * <p>
     * Per ogni container, viene aggiunta un'intestazione appropriata e poi
     * vengono visualizzate tutte le recensioni disponibili per quella caratteristica.
     * </p>
     */
    private void updateReviewContainers() {
        // Pulisci i container
        styleReviewsBox.getChildren().clear();
        styleReviewsBox.getChildren().add(new Label("Recensioni sullo Stile"));
        styleReviewsBox.getChildren().getFirst().setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        contentReviewsBox.getChildren().clear();
        contentReviewsBox.getChildren().add(new Label("Recensioni sul Contenuto"));
        contentReviewsBox.getChildren().getFirst().setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        pleasantnessReviewsBox.getChildren().clear();
        pleasantnessReviewsBox.getChildren().add(new Label("Recensioni sulla Gradevolezza"));
        pleasantnessReviewsBox.getChildren().getFirst().setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        originalityReviewsBox.getChildren().clear();
        originalityReviewsBox.getChildren().add(new Label("Recensioni sull'Originalità"));
        originalityReviewsBox.getChildren().getFirst().setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        editionReviewsBox.getChildren().clear();
        editionReviewsBox.getChildren().add(new Label("Recensioni sull'Edizione"));
        editionReviewsBox.getChildren().getFirst().setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Aggiungi le recensioni per ogni categoria
        addReviewsToContainer(styleReviews, styleReviewsBox);
        addReviewsToContainer(contentReviews, contentReviewsBox);
        addReviewsToContainer(pleasantnessReviews, pleasantnessReviewsBox);
        addReviewsToContainer(originalityReviews, originalityReviewsBox);
        addReviewsToContainer(editionReviews, editionReviewsBox);
    }

    /**
     * Calcola e restituisce la valutazione media assegnata da un utente specifico a un libro.
     * Interroga il database per ottenere il valore di "average_rating" registrato
     * per la combinazione utente-libro specificata.
     *
     * @param userId L'identificativo dell'utente di cui si vuole conoscere la valutazione
     * @param bookId L'identificativo del libro valutato
     * @return La valutazione media dell'utente per il libro, o 0.0 se non trovata
     */
    private double getUserRatingForBook(String userId, int bookId) {
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT average_rating FROM book_ratings WHERE user_id = ? AND book_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setInt(2, bookId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return rs.getDouble("average_rating");
                }
            }
        } catch (SQLException e) {
            // Gestione errore
        }
        return 0.0; // Valore predefinito se non trovato
    }

    /**
     * Recupera dal database tutti i commenti generali lasciati dagli utenti per un libro specifico.
     * Considera solo i commenti non vuoti e non nulli per il libro specificato.
     *
     * @param bookId L'identificativo del libro di cui recuperare i commenti
     * @return Una lista di oggetti Comment contenenti l'ID utente e il testo del commento
     */
    private List<Comment> getUserComments(int bookId) {
        List<Comment> comments = new ArrayList<>();

        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT user_id, general_comment FROM book_ratings WHERE book_id = ? AND general_comment IS NOT NULL AND general_comment != ''";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, bookId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String userId = rs.getString("user_id");
                    String generalComment = rs.getString("general_comment");
                    comments.add(new Comment(userId, generalComment));
                }
            }
        } catch (SQLException e) {
            // Gestione silenziosa dell'errore
        }

        return comments;
    }

    /**
     * Genera e visualizza i libri consigliati per il libro corrente.
     * Il metodo cerca prima consigli personalizzati inseriti dagli utenti nel database.
     * Se non ne trova, genera automaticamente consigli basati sulla categoria del libro attuale,
     * mostrando altri libri della stessa categoria.
     */
    private void generateRecommendedBooks() {
        // Prima cerca se esistono consigli personalizzati dal database
        boolean foundCustomRecommendations = findCustomRecommendations();

        // Se non sono stati trovati consigli personalizzati, genera consigli basati sulla categoria
        if (!foundCustomRecommendations) {
            findSimilarBooks();
        }
    }
    /**
     * Cerca nel database consigli personalizzati inseriti dagli utenti per il libro corrente.
     * Per ogni consiglio trovato, crea un elemento visivo che mostra l'utente che ha fatto
     * la raccomandazione, il titolo del libro consigliato, e un pulsante per visualizzare i dettagli.
     * I titoli lunghi vengono troncati per mantenere una visualizzazione coerente.
     *
     * <p>Interroga il database per trovare le raccomandazioni associate al libro corrente
     * e le visualizza nell'interfaccia utente. Per ogni libro consigliato, vengono mostrati
     * il titolo (troncato se necessario), l'autore (se disponibile) e un pulsante "Visualizza"
     * per navigare ai dettagli completi del libro.</p>
     *
     * @return true se sono stati trovati consigli personalizzati, false altrimenti
     */
    private boolean findCustomRecommendations() {
        List<RecommendedBook> customRecommendations = new ArrayList<>();

        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT r.user_id, b.title FROM book_recommendations r " +
                    "JOIN books b ON r.recommended_book_id = b.id " +
                    "WHERE r.source_book_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, currentBookId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String userId = rs.getString("user_id");
                    String recommendedBookTitle = rs.getString("title");
                    customRecommendations.add(new RecommendedBook(userId, recommendedBookTitle));
                }
            }
        } catch (SQLException e) {
            // Gestione silenziosa dell'errore
        }

        // Se ci sono consigli personalizzati, crea box visivi per ognuno
        if (!customRecommendations.isEmpty()) {
            VBox recommendationsContainer = new VBox(10);
            recommendationsContainer.setPadding(new Insets(10));

            Label headerLabel = new Label("Libri consigliati dagli utenti:");
            headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            recommendationsContainer.getChildren().add(headerLabel);

            // Per tenere traccia del prossimo indice di colore da usare
            int nextColorIndex = 0;
            for (String key : userColors.keySet()) {
                int colorIndex = java.util.Arrays.asList(colorPalette).indexOf(userColors.get(key));
                if (colorIndex >= 0 && colorIndex + 1 > nextColorIndex) {
                    nextColorIndex = colorIndex + 1;
                }
            }

            for (RecommendedBook recommendation : customRecommendations) {
                // Crea un box per ogni consiglio, simile alle recensioni
                VBox recommendationBox = new VBox(5);
                recommendationBox.setPadding(new Insets(10));
                recommendationBox.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #e0e0e0; -fx-border-radius: 5px;");

                // Controlla se l'utente ha già un colore assegnato,
                // altrimenti assegna un nuovo colore dalla palette
                String userColor;
                if (!userColors.containsKey(recommendation.userId)) {
                    userColor = colorPalette[nextColorIndex % colorPalette.length];
                    userColors.put(recommendation.userId, userColor);
                    nextColorIndex++;
                } else {
                    userColor = userColors.get(recommendation.userId);
                }

                // Crea un HBox per informazioni e bottone
                HBox mainBox = new HBox();
                mainBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                mainBox.setSpacing(15);

                // Contenitore per le informazioni - imposta una larghezza massima
                VBox infoBox = new VBox(5);
                infoBox.setMaxWidth(520); // Limita la larghezza massima
                HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                // Aggiungi prefisso "Consigliato da:" all'ID utente
                Label userLabel = new Label(recommendation.userId);
                userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + userColor + ";");

                // Cerca il libro nel database per ottenere l'autore
                Book recommendedBook = findBookByTitle(recommendation.bookTitle);

                // Tronca il titolo se necessario
                String displayTitle = recommendation.bookTitle;
                String originalTitle = recommendation.bookTitle;
                if (displayTitle.length() > 60) {
                    displayTitle = displayTitle.substring(0, 57) + "...";
                }

                // Imposta il titolo del libro in grassetto
                Label bookLabel = new Label("\"" + displayTitle + "\"");
                bookLabel.setStyle("-fx-font-style: italic; -fx-font-weight: bold;");
                bookLabel.setWrapText(true);
                bookLabel.setUserData(originalTitle); // Salva il titolo originale come metadato

                // Aggiungi l'autore se il libro è stato trovato
                if (recommendedBook != null) {
                    Label authorLabel = new Label("Autore: " + recommendedBook.getAuthors());
                    authorLabel.setWrapText(true);
                    infoBox.getChildren().addAll(userLabel, bookLabel, authorLabel);
                } else {
                    infoBox.getChildren().addAll(userLabel, bookLabel);
                }

                // Aggiungi bottone "Visualizza" simile a quello in homepage
                Button viewButton = new Button("Visualizza");
                viewButton.setStyle("-fx-text-fill: white; -fx-background-color: #75B965; -fx-background-radius: 40px; -fx-padding: 8px 15px;");
                viewButton.setPrefWidth(100); // Imposta una larghezza fissa
                viewButton.setMinWidth(100);  // Imposta larghezza minima
                viewButton.setMaxWidth(100);  // Imposta larghezza massima
                viewButton.setOnAction(e -> visualizzalibro(e, originalTitle));

                // Crea un contenitore per il bottone e centralo verticalmente
                VBox buttonBox = new VBox();
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
                buttonBox.setPrefWidth(120); // Spazio fisso per il contenitore del bottone
                buttonBox.setMinWidth(120);
                buttonBox.getChildren().add(viewButton);

                // Aggiungi infoBox e buttonBox al mainBox
                mainBox.getChildren().addAll(infoBox, buttonBox);

                // Aggiungi il mainBox al recommendation box
                recommendationBox.getChildren().add(mainBox);
                recommendationsContainer.getChildren().add(recommendationBox);
            }

            // Sostituisci l'etichetta delle raccomandazioni con il container
            if (recommendedBooksLabel.getParent() instanceof VBox parent) {
                int index = parent.getChildren().indexOf(recommendedBooksLabel);
                if (index >= 0) {
                    parent.getChildren().set(index, recommendationsContainer);
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Cerca nel database libri simili al libro corrente basandosi sulla categoria.
     * Se trovati, crea elementi visivi per mostrarli come consigli automatici,
     * ciascuno con un pulsante "Visualizza" per accedere ai dettagli del libro.
     * I titoli lunghi vengono troncati per garantire una visualizzazione uniforme.
     *
     * <p>Esegue una query sul database per trovare altri libri della stessa categoria
     * del libro corrente, escludendo il libro stesso. Per ogni libro trovato, viene
     * creato un elemento grafico contenente il titolo troncato, l'autore e un pulsante
     * di dimensione fissa che permette all'utente di navigare direttamente ai dettagli
     * completi del libro.</p>
     *
     * <p>Se non vengono trovati libri simili, viene visualizzato un messaggio informativo.</p>
     */
    private void findSimilarBooks() {
        List<Book> similarBooks = new ArrayList<>();

        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT title, authors, category, publisher, publish_year FROM books " +
                    "WHERE category = ? AND id != ? LIMIT 3";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, currentBook.getCategory());
                pstmt.setInt(2, currentBookId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Book book = new Book(
                            rs.getString("title"),
                            rs.getString("authors"),
                            rs.getString("category"),
                            rs.getString("publisher"),
                            rs.getInt("publish_year")
                    );
                    similarBooks.add(book);
                }
            }
        } catch (SQLException e) {
            // Gestione silenziosa dell'errore
        }

        // Se ci sono libri simili, mostrali
        if (!similarBooks.isEmpty()) {
            VBox similarBooksContainer = new VBox(10);
            similarBooksContainer.setPadding(new Insets(10));

            Label headerLabel = new Label("Libri consigliati automaticamente secondo la categoria:");
            headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            similarBooksContainer.getChildren().add(headerLabel);

            for (Book book : similarBooks) {
                VBox bookBox = new VBox(5);
                bookBox.setPadding(new Insets(10));
                bookBox.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #e0e0e0; -fx-border-radius: 5px;");

                // Crea un HBox per informazioni e bottone
                HBox mainBox = new HBox();
                mainBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                mainBox.setSpacing(15);

                // Contenitore per le informazioni - imposta una larghezza massima
                VBox infoBox = new VBox(5);
                infoBox.setMaxWidth(520); // Limita la larghezza massima
                HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                // Tronca il titolo se necessario
                String displayTitle = book.getTitle();
                String originalTitle = book.getTitle();
                if (displayTitle.length() > 60) {
                    displayTitle = displayTitle.substring(0, 57) + "...";
                }

                // Il titolo è già in grassetto, ma aggiungiamo anche lo stile corsivo per coerenza
                Label titleLabel = new Label("\"" + displayTitle + "\"");
                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-style: italic;");
                titleLabel.setWrapText(true);
                titleLabel.setUserData(originalTitle); // Salva il titolo originale come metadato

                Label authorLabel = new Label("Autore: " + book.getAuthors());
                authorLabel.setWrapText(true);

                infoBox.getChildren().addAll(titleLabel, authorLabel);

                // Aggiungi bottone "Visualizza" simile a quello in homepage
                Button viewButton = new Button("Visualizza");
                viewButton.setStyle("-fx-text-fill: white; -fx-background-color: #75B965; -fx-background-radius: 40px; -fx-padding: 8px 15px;");
                viewButton.setPrefWidth(100); // Imposta una larghezza fissa
                viewButton.setMinWidth(100);  // Imposta larghezza minima
                viewButton.setMaxWidth(100);  // Imposta larghezza massima
                viewButton.setOnAction(e -> visualizzalibro(e, originalTitle));

                // Crea un contenitore per il bottone e centralo verticalmente
                VBox buttonBox = new VBox();
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
                buttonBox.setPrefWidth(120); // Spazio fisso per il contenitore del bottone
                buttonBox.setMinWidth(120);
                buttonBox.getChildren().add(viewButton);

                // Aggiungi infoBox e buttonBox al mainBox
                mainBox.getChildren().addAll(infoBox, buttonBox);

                // Aggiungi il mainBox al bookBox
                bookBox.getChildren().add(mainBox);
                similarBooksContainer.getChildren().add(bookBox);
            }

            // Sostituisci l'etichetta delle raccomandazioni con il container
            if (recommendedBooksLabel.getParent() instanceof VBox parent) {
                int index = parent.getChildren().indexOf(recommendedBooksLabel);
                if (index >= 0) {
                    parent.getChildren().set(index, similarBooksContainer);
                }
            }
        } else {
            recommendedBooksLabel.setText("Nessun libro consigliato disponibile.");
        }
    }

    /**
     * Naviga alla pagina di dettaglio del libro specificato.
     * Questo metodo carica la vista stampadettaglinologin.fxml e imposta il titolo del libro
     * selezionato per visualizzarne i dettagli completi, permettendo all'utente di esplorare
     * le informazioni del libro consigliato senza tornare alla homepage.
     *
     * <p>Gestisce la navigazione tra le viste dell'applicazione, caricando la pagina di
     * dettaglio del libro e configurando il controller corrispondente con i dati appropriati.
     * Utilizza lo stesso meccanismo di navigazione presente nella homepage per mantenere
     * un'esperienza utente coerente.</p>
     *
     * @param event L'evento di azione generato dal click sul pulsante "Visualizza"
     * @param bookTitle Il titolo del libro di cui visualizzare i dettagli
     */
    public void visualizzalibro(ActionEvent event, String bookTitle) {
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
            // Gestione silenziosa dell'errore
            e.printStackTrace();
        }
    }

    /**
     * Gestisce l'evento di click sul pulsante "Torna indietro".
     * Carica la schermata principale dell'applicazione (homepage)
     * e la visualizza, sostituendo la schermata corrente.
     *
     * @param event L'evento di azione generato dal click sul pulsante
     */
    @FXML
    public void handleBack(ActionEvent event) {
        // Torna alla homepage
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/homepage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}