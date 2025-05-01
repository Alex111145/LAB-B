package book_recommender.lab_b;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
 * Controller per la visualizzazione dei dettagli di un libro.
 */
public class BookDetailsController implements Initializable {

    @FXML private Label bookTitleLabel;
    @FXML private Label authorsLabel;
    @FXML private Label categoryLabel;
    @FXML private Label publisherLabel;
    @FXML private Label yearLabel;
    @FXML private Label styleRatingLabel;
    @FXML private Label contentRatingLabel;
    @FXML private Label pleasantnessRatingLabel;
    @FXML private Label originalityRatingLabel;
    @FXML private Label editionRatingLabel;
    @FXML private Label totalRatingLabel;
    @FXML private Label usersCountLabel;
    @FXML private Label generalCommentsLabel;
    @FXML private Label recommendedBooksLabel;
    @FXML private Button backButton;

    // Container per le recensioni per ogni categoria
    @FXML private VBox styleReviewsBox;
    @FXML private VBox contentReviewsBox;
    @FXML private VBox pleasantnessReviewsBox;
    @FXML private VBox originalityReviewsBox;
    @FXML private VBox editionReviewsBox;

    // Riferimenti alle stelle per le valutazioni
    @FXML private Text styleStar1, styleStar2, styleStar3, styleStar4, styleStar5;
    @FXML private Text contentStar1, contentStar2, contentStar3, contentStar4, contentStar5;
    @FXML private Text pleasantnessStar1, pleasantnessStar2, pleasantnessStar3, pleasantnessStar4, pleasantnessStar5;
    @FXML private Text originalityStar1, originalityStar2, originalityStar3, originalityStar4, originalityStar5;
    @FXML private Text editionStar1, editionStar2, editionStar3, editionStar4, editionStar5;
    @FXML private Text totalStar1, totalStar2, totalStar3, totalStar4, totalStar5;

    // Mappa dei colori assegnati a ciascun utente
    private final Map<String, String> userColors = new HashMap<>();
    private final String[] colorPalette = {
            "#e74c3c", "#3498db", "#2ecc71", "#f39c12", "#9b59b6", "#1abc9c", "#d35400",
            "#27ae60", "#2980b9", "#8e44ad", "#f1c40f", "#16a085", "#2c3e50", "#f39c12",
            "#e67e22", "#95a5a6", "#bdc3c7", "#7f8c8d", "#34495e", "#d5dbdb", "#9a59b6",
            "#f5b041", "#58d68d", "#5dade2", "#f1948a", "#d6eaf8", "#f7b7a3", "#e8daef", "#c39bd3"
    };

    public Button getBackButton() {
        return backButton;
    }

    public void setBackButton(Button backButton) {
        this.backButton = backButton;
    }

    // Struttura per memorizzare recensioni
    private static class Review {
        public String userId;
        public int rating;
        public String comment;

        public Review(String userId, int rating, String comment) {
            this.userId = userId;
            this.rating = rating;
            this.comment = comment;
        }
    }

    // Struttura per memorizzare i commenti generali
    private static class Comment {
        public String userId;
        public String text;

        public Comment(String userId, String text) {
            this.userId = userId;
            this.text = text;
        }
    }

    // Struttura per memorizzare i libri consigliati
    private static class RecommendedBook {
        public String userId;
        public String bookTitle;

        public RecommendedBook(String userId, String bookTitle) {
            this.userId = userId;
            this.bookTitle = bookTitle;
        }
    }

    // Liste per memorizzare le recensioni per ogni caratteristica
    private final List<Review> styleReviews = new ArrayList<>();
    private final List<Review> contentReviews = new ArrayList<>();
    private final List<Review> pleasantnessReviews = new ArrayList<>();
    private final List<Review> originalityReviews = new ArrayList<>();
    private final List<Review> editionReviews = new ArrayList<>();

    // Dati del libro corrente
    private Book currentBook;
    private int currentBookId;

    // Mappa per memorizzare le valutazioni del libro
    private final Map<String, Double> ratings = new HashMap<>();

    // Numero di utenti che hanno valutato il libro
    private int numRaters = 0;

    private DatabaseManager dbManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
        }

        // Inizializza i container delle recensioni
        styleReviewsBox.setSpacing(10);
        contentReviewsBox.setSpacing(10);
        pleasantnessReviewsBox.setSpacing(10);
        originalityReviewsBox.setSpacing(10);
        editionReviewsBox.setSpacing(10);
    }

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

    private Book createExampleBook(String title) {
        return new Book(
                title,
                "Autore sconosciuto",
                "Non categorizzato",
                "Editore sconosciuto",
                2000
        );
    }

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
            System.err.println("Error finding book: " + e.getMessage());
        }

        return null;
    }

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

                    if (styleComment != null && !styleComment.isEmpty()) {
                        styleReviews.add(new Review(userId, style, styleComment));
                    } else if (generalComment != null && !generalComment.isEmpty()) {
                        styleReviews.add(new Review(userId, style, generalComment));
                    }

                    if (contentComment != null && !contentComment.isEmpty()) {
                        contentReviews.add(new Review(userId, content, contentComment));
                    } else if (generalComment != null && !generalComment.isEmpty()) {
                        contentReviews.add(new Review(userId, content, generalComment));
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
            System.err.println("Error loading ratings and reviews: " + e.getMessage());
            setupExampleRatings();
        }
    }

    private void updateUI() {
        // Imposta i dati del libro
        bookTitleLabel.setText(currentBook.getTitle());
        authorsLabel.setText(currentBook.getAuthors());
        categoryLabel.setText(currentBook.getCategory());
        publisherLabel.setText(currentBook.getPublisher());
        yearLabel.setText(String.valueOf(currentBook.getPublishYear()));

        // Imposta le valutazioni
        styleRatingLabel.setText(String.valueOf(ratings.get("style")));
        contentRatingLabel.setText(String.valueOf(ratings.get("content")));
        pleasantnessRatingLabel.setText(String.valueOf(ratings.get("pleasantness")));
        originalityRatingLabel.setText(String.valueOf(ratings.get("originality")));
        editionRatingLabel.setText(String.valueOf(ratings.get("edition")));
        totalRatingLabel.setText(String.valueOf(ratings.get("total")));

        // Aggiorna il numero di valutatori
        usersCountLabel.setText(String.valueOf(numRaters));

        // Colora le stelle in base alle valutazioni
        updateStars(styleStar1, styleStar2, styleStar3, styleStar4, styleStar5, ratings.get("style"));
        updateStars(contentStar1, contentStar2, contentStar3, contentStar4, contentStar5, ratings.get("content"));
        updateStars(pleasantnessStar1, pleasantnessStar2, pleasantnessStar3, pleasantnessStar4, pleasantnessStar5, ratings.get("pleasantness"));
        updateStars(originalityStar1, originalityStar2, originalityStar3, originalityStar4, originalityStar5, ratings.get("originality"));
        updateStars(editionStar1, editionStar2, editionStar3, editionStar4, editionStar5, ratings.get("edition"));
        updateStars(totalStar1, totalStar2, totalStar3, totalStar4, totalStar5, ratings.get("total"));

        // Genera un commento generale basato sulle valutazioni
        generateGeneralComment();

        // Aggiorna i container delle recensioni
        updateReviewContainers();

        // Genera libri consigliati basati sulla categoria del libro attuale
        generateRecommendedBooks();
    }

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

            // Aggiungi stelle per la valutazione
            HBox starsBox = new HBox(2);
            for (int i = 1; i <= 5; i++) {
                Text star = new Text("★");
                star.setStyle("-fx-font-size: 14px;");
                star.setFill(i <= review.rating ? Color.web("#ffc107") : Color.web("#dddddd"));
                starsBox.getChildren().add(star);
            }

            headerBox.getChildren().addAll(userLabel, starsBox);

            // Corpo della recensione
            Label commentLabel = new Label(review.comment);
            commentLabel.setWrapText(true);
            commentLabel.setStyle("-fx-padding: 5 0 0 0;");

            reviewBox.getChildren().addAll(headerBox, commentLabel);
            container.getChildren().add(reviewBox);
        }
    }

    private void updateStars(Text star1, Text star2, Text star3, Text star4, Text star5, double rating) {
        Color goldColor = Color.web("#ffc107");
        Color grayColor = Color.web("#dddddd");

        star1.setFill(rating >= 1 ? goldColor : grayColor);
        star2.setFill(rating >= 2 ? goldColor : grayColor);
        star3.setFill(rating >= 3 ? goldColor : grayColor);
        star4.setFill(rating >= 4 ? goldColor : grayColor);
        star5.setFill(rating >= 5 ? goldColor : grayColor);
    }

    private void generateGeneralComment() {
        // Pulisci prima il container dei commenti generali
        generalCommentsLabel.setText("");

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
        headerLabel.setText("Commento generale");
        commentsContainer.getChildren().add(headerLabel);

        // Aggiungi il sommario delle valutazioni
        Label summaryLabel = new Label();
        if (numRaters > 0) {
            double totalRating = ratings.get("total");
            summaryLabel.setText("Questo libro ha una valutazione media di " + totalRating + " stelle basata su " + numRaters + " valutazioni.");
        } else {
            summaryLabel.setText("Questo libro non ha ancora ricevuto valutazioni.");
        }
        summaryLabel.setWrapText(true);
        commentsContainer.getChildren().add(summaryLabel);

        // Aggiungi i commenti degli utenti
        if (numRaters > 0) {
            // Recupera i commenti dal database
            List<Comment> userComments = getUserComments(currentBookId);

            if (!userComments.isEmpty()) {
                Label reviewsHeader = new Label("Recensioni degli utenti:");
                reviewsHeader.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
                commentsContainer.getChildren().add(reviewsHeader);

                // Aggiungi ogni commento colorato al container
                for (Comment comment : userComments) {
                    // Crea un box per ogni commento
                    VBox commentBox = new VBox(5);
                    commentBox.setStyle("-fx-padding: 5; -fx-background-color: #f8f8f8; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

                    // Usa il colore assegnato all'utente
                    String userColor = userColors.getOrDefault(comment.userId, "#333333");

                    // Crea etichette separate per utente e commento
                    Label userLabel = new Label(comment.userId + ":");
                    userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + userColor + ";");

                    Label textLabel = new Label(comment.text);
                    textLabel.setWrapText(true);
                    textLabel.setStyle("-fx-font-style: italic;");

                    commentBox.getChildren().addAll(userLabel, textLabel);
                    commentsContainer.getChildren().add(commentBox);
                }
            }
        }
    }

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
            System.err.println("Error getting user comments: " + e.getMessage());
        }

        return comments;
    }

    private void generateRecommendedBooks() {
        // Prima cerca se esistono consigli personalizzati dal database
        boolean foundCustomRecommendations = findCustomRecommendations();

        // Se non sono stati trovati consigli personalizzati, genera consigli basati sulla categoria
        if (!foundCustomRecommendations) {
            findSimilarBooks();
        }
    }

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
            System.err.println("Error getting custom recommendations: " + e.getMessage());
        }

        // Se ci sono consigli personalizzati, crea box visivi per ognuno
        if (!customRecommendations.isEmpty()) {
            VBox recommendationsContainer = new VBox(10);
            recommendationsContainer.setPadding(new Insets(10));

            Label headerLabel = new Label("Libri consigliati specificamente per questo libro:");
            headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            recommendationsContainer.getChildren().add(headerLabel);

            for (RecommendedBook recommendation : customRecommendations) {
                // Crea un box per ogni consiglio, simile alle recensioni
                VBox recommendationBox = new VBox(5);
                recommendationBox.setPadding(new Insets(10));
                recommendationBox.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #e0e0e0; -fx-border-radius: 5px;");

                // Usa il colore assegnato all'utente
                String userColor = userColors.getOrDefault(recommendation.userId, "#333333");

                Label userLabel = new Label(recommendation.userId);
                userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + userColor + ";");

                // Cerca il libro nel database per ottenere l'autore
                Book recommendedBook = findBookByTitle(recommendation.bookTitle);

                Label bookLabel = new Label("\"" + recommendation.bookTitle + "\"");
                bookLabel.setStyle("-fx-font-style: italic;");
                bookLabel.setWrapText(true);

                // Aggiungi l'autore se il libro è stato trovato
                if (recommendedBook != null) {
                    Label authorLabel = new Label(recommendedBook.getAuthors());
                    authorLabel.setWrapText(true);
                    recommendationBox.getChildren().addAll(userLabel, bookLabel, authorLabel);
                } else {
                    recommendationBox.getChildren().addAll(userLabel, bookLabel);
                }

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
            System.err.println("Error finding similar books: " + e.getMessage());
        }

        // Se ci sono libri simili, mostrali
        if (!similarBooks.isEmpty()) {
            VBox similarBooksContainer = new VBox(10);
            similarBooksContainer.setPadding(new Insets(10));

            Label headerLabel = new Label("Libri consigliati basati sulla categoria:");
            headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            similarBooksContainer.getChildren().add(headerLabel);

            for (Book book : similarBooks) {
                VBox bookBox = new VBox(5);
                bookBox.setPadding(new Insets(10));
                bookBox.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #e0e0e0; -fx-border-radius: 5px;");

                Label titleLabel = new Label("\"" + book.getTitle() + "\"");
                titleLabel.setStyle("-fx-font-weight: bold;");
                titleLabel.setWrapText(true);

                Label authorLabel = new Label(book.getAuthors());
                authorLabel.setWrapText(true);

                bookBox.getChildren().addAll(titleLabel, authorLabel);
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
     * Gestisce il click sul pulsante "Torna indietro".
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
            System.err.println("Errore nel caricamento della homepage: " + e.getMessage());
            e.printStackTrace();
        }
    }
}