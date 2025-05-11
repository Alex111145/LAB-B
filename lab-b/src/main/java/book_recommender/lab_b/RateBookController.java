package book_recommender.lab_b;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class RateBookController implements Initializable {

    // Componenti dell'interfaccia utente dichiarati nel file FXML
    @FXML private Label userIdLabel;
    @FXML private Label bookTitleLabel;
    @FXML private Label averageRatingLabel;
    @FXML private Label errorLabel;

    // Aree di testo per i commenti
    @FXML private TextArea styleCommentArea;
    @FXML private TextArea contentCommentArea;
    @FXML private TextArea pleasantnessCommentArea;
    @FXML private TextArea originalityCommentArea;
    @FXML private TextArea editionCommentArea;
    @FXML private TextArea finalCommentArea;

    // Pulsanti
    @FXML private Button submitButton;
    @FXML private Button cancelButton;
    @FXML private Button backButton;

    // StackPane per gestire il click sulle stelle
    @FXML private StackPane styleStar1Container, styleStar2Container, styleStar3Container,
            styleStar4Container, styleStar5Container;
    @FXML private StackPane contentStar1Container, contentStar2Container, contentStar3Container,
            contentStar4Container, contentStar5Container;
    @FXML private StackPane pleasantnessStar1Container, pleasantnessStar2Container, pleasantnessStar3Container,
            pleasantnessStar4Container, pleasantnessStar5Container;
    @FXML private StackPane originalityStar1Container, originalityStar2Container, originalityStar3Container,
            originalityStar4Container, originalityStar5Container;
    @FXML private StackPane editionStar1Container, editionStar2Container, editionStar3Container,
            editionStar4Container, editionStar5Container;

    // Stelle di valutazione (rappresentate come Text)
    @FXML private Text styleStar1, styleStar2, styleStar3, styleStar4, styleStar5;
    @FXML private Text contentStar1, contentStar2, contentStar3, contentStar4, contentStar5;
    @FXML private Text pleasantnessStar1, pleasantnessStar2, pleasantnessStar3, pleasantnessStar4, pleasantnessStar5;
    @FXML private Text originalityStar1, originalityStar2, originalityStar3, originalityStar4, originalityStar5;
    @FXML private Text editionStar1, editionStar2, editionStar3, editionStar4, editionStar5;
    @FXML private Text averageStar1, averageStar2, averageStar3, averageStar4, averageStar5;

    // Dati dell'utente e del libro
    private String userId;
    private String bookTitle;
    private String libraryName;
    private int bookId;

    // Valutazioni assegnate (da 1 a 5)
    private int styleRating = 0;
    private int contentRating = 0;
    private int pleasantnessRating = 0;
    private int originalityRating = 0;
    private int editionRating = 0;

    // Colori per le stelle
    private static final String STAR_ACTIVE_COLOR = "#f2e485";   // Colore giallo per le stelle attive
    private static final String STAR_INACTIVE_COLOR = "#dddddd"; // Colore grigio per le stelle inattive

    private DatabaseManager dbManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
        }

        // Nasconde l'etichetta di errore all'avvio
        errorLabel.setVisible(false);

        // Imposta il controllo di lunghezza massima per i commenti
        setupTextAreaLimits();

        // Inizializza le stelle con colore inattivo
        resetAllStars();

        // Aggiorna la media delle valutazioni
        updateAverageRating();
    }

    private void setupTextAreaLimits() {
        // Imposta un limite di 256 caratteri per ogni area di testo
        int maxChars = 256;

        limitTextArea(styleCommentArea, maxChars);
        limitTextArea(contentCommentArea, maxChars);
        limitTextArea(pleasantnessCommentArea, maxChars);
        limitTextArea(originalityCommentArea, maxChars);
        limitTextArea(editionCommentArea, maxChars);
        limitTextArea(finalCommentArea, maxChars);
    }

    private void limitTextArea(TextArea textArea, int maxChars) {
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > maxChars) {
                textArea.setText(oldValue);
            }
        });
    }

    public void setData(String userId, String bookTitle, String libraryName) {
        this.userId = userId;
        this.bookTitle = bookTitle;
        this.libraryName = libraryName;

        // Aggiorna le etichette nell'interfaccia
        userIdLabel.setText(userId);
        bookTitleLabel.setText(bookTitle);

        // Ottieni l'ID del libro dal database
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT id FROM books WHERE title = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, bookTitle);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    this.bookId = rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting book ID: " + e.getMessage());
        }

        // Controlla se l'utente ha già valutato questo libro
        checkExistingRating();
    }

    private void checkExistingRating() {
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT * FROM book_ratings WHERE user_id = ? AND book_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setInt(2, bookId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    // Carica le valutazioni
                    styleRating = rs.getInt("style_rating");
                    contentRating = rs.getInt("content_rating");
                    pleasantnessRating = rs.getInt("pleasantness_rating");
                    originalityRating = rs.getInt("originality_rating");
                    editionRating = rs.getInt("edition_rating");

                    // Aggiorna le stelle
                    updateStars(styleStar1, styleStar2, styleStar3, styleStar4, styleStar5, styleRating);
                    updateStars(contentStar1, contentStar2, contentStar3, contentStar4, contentStar5, contentRating);
                    updateStars(pleasantnessStar1, pleasantnessStar2, pleasantnessStar3, pleasantnessStar4, pleasantnessStar5, pleasantnessRating);
                    updateStars(originalityStar1, originalityStar2, originalityStar3, originalityStar4, originalityStar5, originalityRating);
                    updateStars(editionStar1, editionStar2, editionStar3, editionStar4, editionStar5, editionRating);

                    // Aggiorna la media
                    updateAverageRating();

                    // Carica i commenti
                    finalCommentArea.setText(rs.getString("general_comment"));
                    styleCommentArea.setText(rs.getString("style_comment"));
                    contentCommentArea.setText(rs.getString("content_comment"));
                    pleasantnessCommentArea.setText(rs.getString("pleasantness_comment"));
                    originalityCommentArea.setText(rs.getString("originality_comment"));
                    editionCommentArea.setText(rs.getString("edition_comment"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking existing rating: " + e.getMessage());

        }
    }

    // Altri metodi rimangono invariati...

    @FXML
    private void handleSubmit(ActionEvent event) {
        // Verifica che tutte le caratteristiche siano state valutate
        if (styleRating == 0 || contentRating == 0 || pleasantnessRating == 0 ||
                originalityRating == 0 || editionRating == 0) {

            // Mostra il messaggio di errore
            errorLabel.setText("Errore: Devi assegnare un voto a tutte le caratteristiche prima di inviare.");
            errorLabel.setVisible(true);
            return;
        }

        // Nascondi eventuali messaggi di errore
        errorLabel.setVisible(false);

        // Salva la valutazione nel database
        if (saveRating()) {
            // Torna al menu utente con successo
            navigateToUserMenu(event);
        } else {
            errorLabel.setText("Errore nel salvataggio della valutazione.");
            errorLabel.setVisible(true);
        }
    }

    private boolean saveRating() {
        try (Connection conn = dbManager.getConnection()) {
            // Usa UPSERT per inserire o aggiornare la valutazione
            String sql = "INSERT INTO book_ratings (user_id, book_id, style_rating, content_rating, " +
                    "pleasantness_rating, originality_rating, edition_rating, general_comment, " +
                    "style_comment, content_comment, pleasantness_comment, originality_comment, edition_comment) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (user_id, book_id) DO UPDATE SET " +
                    "style_rating = EXCLUDED.style_rating, " +
                    "content_rating = EXCLUDED.content_rating, " +
                    "pleasantness_rating = EXCLUDED.pleasantness_rating, " +
                    "originality_rating = EXCLUDED.originality_rating, " +
                    "edition_rating = EXCLUDED.edition_rating, " +
                    "general_comment = EXCLUDED.general_comment, " +
                    "style_comment = EXCLUDED.style_comment, " +
                    "content_comment = EXCLUDED.content_comment, " +
                    "pleasantness_comment = EXCLUDED.pleasantness_comment, " +
                    "originality_comment = EXCLUDED.originality_comment, " +
                    "edition_comment = EXCLUDED.edition_comment";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setInt(2, bookId);
                pstmt.setInt(3, styleRating);
                pstmt.setInt(4, contentRating);
                pstmt.setInt(5, pleasantnessRating);
                pstmt.setInt(6, originalityRating);
                pstmt.setInt(7, editionRating);
                pstmt.setString(8, finalCommentArea.getText());
                pstmt.setString(9, styleCommentArea.getText());
                pstmt.setString(10, contentCommentArea.getText());
                pstmt.setString(11, pleasantnessCommentArea.getText());
                pstmt.setString(12, originalityCommentArea.getText());
                pstmt.setString(13, editionCommentArea.getText());

                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving rating: " + e.getMessage());

            return false;
        }
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

    // Tutti gli altri metodi per la gestione delle stelle rimangono invariati...

    @FXML
    private void handleStyleStarClick(MouseEvent event) {
        StackPane source = (StackPane) event.getSource();

        if (source.equals(styleStar1Container)) {
            styleRating = 1;
        } else if (source.equals(styleStar2Container)) {
            styleRating = 2;
        } else if (source.equals(styleStar3Container)) {
            styleRating = 3;
        } else if (source.equals(styleStar4Container)) {
            styleRating = 4;
        } else if (source.equals(styleStar5Container)) {
            styleRating = 5;
        }

        updateStars(styleStar1, styleStar2, styleStar3, styleStar4, styleStar5, styleRating);
        updateAverageRating();
    }

    @FXML
    private void handleContentStarClick(MouseEvent event) {
        StackPane source = (StackPane) event.getSource();

        if (source.equals(contentStar1Container)) {
            contentRating = 1;
        } else if (source.equals(contentStar2Container)) {
            contentRating = 2;
        } else if (source.equals(contentStar3Container)) {
            contentRating = 3;
        } else if (source.equals(contentStar4Container)) {
            contentRating = 4;
        } else if (source.equals(contentStar5Container)) {
            contentRating = 5;
        }

        updateStars(contentStar1, contentStar2, contentStar3, contentStar4, contentStar5, contentRating);
        updateAverageRating();
    }

    @FXML
    private void handlePleasantnessStarClick(MouseEvent event) {
        StackPane source = (StackPane) event.getSource();

        if (source.equals(pleasantnessStar1Container)) {
            pleasantnessRating = 1;
        } else if (source.equals(pleasantnessStar2Container)) {
            pleasantnessRating = 2;
        } else if (source.equals(pleasantnessStar3Container)) {
            pleasantnessRating = 3;
        } else if (source.equals(pleasantnessStar4Container)) {
            pleasantnessRating = 4;
        } else if (source.equals(pleasantnessStar5Container)) {
            pleasantnessRating = 5;
        }

        updateStars(pleasantnessStar1, pleasantnessStar2, pleasantnessStar3, pleasantnessStar4, pleasantnessStar5, pleasantnessRating);
        updateAverageRating();
    }

    @FXML
    private void handleOriginalityStarClick(MouseEvent event) {
        StackPane source = (StackPane) event.getSource();

        if (source.equals(originalityStar1Container)) {
            originalityRating = 1;
        } else if (source.equals(originalityStar2Container)) {
            originalityRating = 2;
        } else if (source.equals(originalityStar3Container)) {
            originalityRating = 3;
        } else if (source.equals(originalityStar4Container)) {
            originalityRating = 4;
        } else if (source.equals(originalityStar5Container)) {
            originalityRating = 5;
        }

        updateStars(originalityStar1, originalityStar2, originalityStar3, originalityStar4, originalityStar5, originalityRating);
        updateAverageRating();
    }

    @FXML
    private void handleEditionStarClick(MouseEvent event) {
        StackPane source = (StackPane) event.getSource();

        if (source.equals(editionStar1Container)) {
            editionRating = 1;
        } else if (source.equals(editionStar2Container)) {
            editionRating = 2;
        } else if (source.equals(editionStar3Container)) {
            editionRating = 3;
        } else if (source.equals(editionStar4Container)) {
            editionRating = 4;
        } else if (source.equals(editionStar5Container)) {
            editionRating = 5;
        }

        updateStars(editionStar1, editionStar2, editionStar3, editionStar4, editionStar5, editionRating);
        updateAverageRating();
    }

    private void updateStars(Text star1, Text star2, Text star3, Text star4, Text star5, int rating) {
        star1.setFill(javafx.scene.paint.Color.web(rating >= 1 ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR));
        star2.setFill(javafx.scene.paint.Color.web(rating >= 2 ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR));
        star3.setFill(javafx.scene.paint.Color.web(rating >= 3 ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR));
        star4.setFill(javafx.scene.paint.Color.web(rating >= 4 ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR));
        star5.setFill(javafx.scene.paint.Color.web(rating >= 5 ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR));
    }

    private void resetAllStars() {
        updateStars(styleStar1, styleStar2, styleStar3, styleStar4, styleStar5, 0);
        updateStars(contentStar1, contentStar2, contentStar3, contentStar4, contentStar5, 0);
        updateStars(pleasantnessStar1, pleasantnessStar2, pleasantnessStar3, pleasantnessStar4, pleasantnessStar5, 0);
        updateStars(originalityStar1, originalityStar2, originalityStar3, originalityStar4, originalityStar5, 0);
        updateStars(editionStar1, editionStar2, editionStar3, editionStar4, editionStar5, 0);
        updateStars(averageStar1, averageStar2, averageStar3, averageStar4, averageStar5, 0);
    }

    private void updateAverageRating() {
        if (styleRating > 0 || contentRating > 0 || pleasantnessRating > 0 ||
                originalityRating > 0 || editionRating > 0) {

            int count = 0;
            int sum = 0;

            if (styleRating > 0) {
                sum += styleRating;
                count++;
            }

            if (contentRating > 0) {
                sum += contentRating;
                count++;
            }

            if (pleasantnessRating > 0) {
                sum += pleasantnessRating;
                count++;
            }

            if (originalityRating > 0) {
                sum += originalityRating;
                count++;
            }

            if (editionRating > 0) {
                sum += editionRating;
                count++;
            }


                double average = (double) sum / count;
                DecimalFormat df = new DecimalFormat("#.#");
                averageRatingLabel.setText(df.format(average));

                int roundedAverage = (int) Math.round(average);
                updateStars(averageStar1, averageStar2, averageStar3, averageStar4, averageStar5, roundedAverage);

        }}}