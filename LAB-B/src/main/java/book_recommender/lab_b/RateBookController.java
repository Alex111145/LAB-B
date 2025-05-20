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
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Controller per la schermata di valutazione dei libri.
 * Gestisce l'interfaccia utente che permette agli utenti di valutare i libri secondo
 * diverse caratteristiche (stile, contenuto, piacevolezza, originalità, edizione)
 * e di aggiungere commenti per ciascuna categoria.
 * Implementa l'interfaccia Initializable per l'inizializzazione dei componenti FXML.
 */
public class RateBookController implements Initializable {

    // Componenti dell'interfaccia utente dichiarati nel file FXML
    /** Label che mostra l'ID dell'utente corrente */
    @FXML private Label userIdLabel;

    /** Label che mostra il titolo del libro da valutare */
    @FXML private Label bookTitleLabel;

    /** Label che mostra la valutazione media del libro */
    @FXML private Label averageRatingLabel;

    /** Label per mostrare messaggi di errore all'utente */
    @FXML private Label errorLabel;

    // Aree di testo per i commenti
    /** Area di testo per il commento relativo allo stile del libro */
    @FXML private TextArea styleCommentArea;

    /** Area di testo per il commento relativo al contenuto del libro */
    @FXML private TextArea contentCommentArea;

    /** Area di testo per il commento relativo alla piacevolezza del libro */
    @FXML private TextArea pleasantnessCommentArea;

    /** Area di testo per il commento relativo all'originalità del libro */
    @FXML private TextArea originalityCommentArea;

    /** Area di testo per il commento relativo all'edizione del libro */
    @FXML private TextArea editionCommentArea;

    /** Area di testo per il commento finale e generale sul libro */
    @FXML private TextArea finalCommentArea;



    // StackPane per gestire il click sulle stelle
    /** Container per la prima stella della valutazione dello stile */
    @FXML private StackPane styleStar1Container, styleStar2Container, styleStar3Container,
            styleStar4Container, styleStar5Container;

    /** Container per le stelle della valutazione del contenuto */
    @FXML private StackPane contentStar1Container, contentStar2Container, contentStar3Container,
            contentStar4Container, contentStar5Container;

    /** Container per le stelle della valutazione della piacevolezza */
    @FXML private StackPane pleasantnessStar1Container, pleasantnessStar2Container, pleasantnessStar3Container,
            pleasantnessStar4Container, pleasantnessStar5Container;

    /** Container per le stelle della valutazione dell'originalità */
    @FXML private StackPane originalityStar1Container, originalityStar2Container, originalityStar3Container,
            originalityStar4Container, originalityStar5Container;

    /** Container per le stelle della valutazione dell'edizione */
    @FXML private StackPane editionStar1Container, editionStar2Container, editionStar3Container,
            editionStar4Container, editionStar5Container;

    // Stelle di valutazione (rappresentate come Text)
    /** Elementi Text che rappresentano le stelle per la valutazione dello stile */
    @FXML private Text styleStar1, styleStar2, styleStar3, styleStar4, styleStar5;

    /** Elementi Text che rappresentano le stelle per la valutazione del contenuto */
    @FXML private Text contentStar1, contentStar2, contentStar3, contentStar4, contentStar5;

    /** Elementi Text che rappresentano le stelle per la valutazione della piacevolezza */
    @FXML private Text pleasantnessStar1, pleasantnessStar2, pleasantnessStar3, pleasantnessStar4, pleasantnessStar5;

    /** Elementi Text che rappresentano le stelle per la valutazione dell'originalità */
    @FXML private Text originalityStar1, originalityStar2, originalityStar3, originalityStar4, originalityStar5;

    /** Elementi Text che rappresentano le stelle per la valutazione dell'edizione */
    @FXML private Text editionStar1, editionStar2, editionStar3, editionStar4, editionStar5;

    /** Elementi Text che rappresentano le stelle per la valutazione media */
    @FXML private Text averageStar1, averageStar2, averageStar3, averageStar4, averageStar5;

    // Dati dell'utente e del libro
    /** ID dell'utente corrente */
    private String userId;

    /** Titolo del libro da valutare */
    private String bookTitle;

    /** Nome della biblioteca a cui appartiene il libro */
    private String libraryName;

    /** ID del libro nel database */
    private int bookId;

    // Valutazioni assegnate (da 1 a 5)
    /** Valutazione assegnata allo stile del libro (0-5) */
    private int styleRating = 0;

    /** Valutazione assegnata al contenuto del libro (0-5) */
    private int contentRating = 0;

    /** Valutazione assegnata alla piacevolezza del libro (0-5) */
    private int pleasantnessRating = 0;

    /** Valutazione assegnata all'originalità del libro (0-5) */
    private int originalityRating = 0;

    /** Valutazione assegnata all'edizione del libro (0-5) */
    private int editionRating = 0;

    // Colori per le stelle
    /** Codice colore per le stelle attive (giallo) */
    private static final String STAR_ACTIVE_COLOR = "#f2e485";

    /** Codice colore per le stelle inattive (grigio) */
    private static final String STAR_INACTIVE_COLOR = "#dddddd";

    /** Gestore del database per le operazioni di lettura/scrittura */
    private DatabaseManager dbManager;

    /**
     * Inizializza il controller e i suoi componenti.
     * Questo metodo viene chiamato automaticamente dopo il caricamento del file FXML.
     * Inizializza il gestore del database, configura i limiti delle aree di testo,
     * reimposta tutte le stelle e aggiorna la valutazione media.
     *
     * @param location La posizione del file FXML
     * @param resources Le risorse utilizzate per la localizzazione
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Gestione dell'eccezione omessa
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

    /**
     * Configura i limiti di caratteri per tutte le aree di testo.
     * Imposta un limite massimo di 256 caratteri per ogni area di commento.
     */
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

    /**
     * Imposta un limite di caratteri per una specifica area di testo.
     * Aggiunge un listener che impedisce l'inserimento di testo oltre il limite specificato.
     *
     * @param textArea L'area di testo da limitare
     * @param maxChars Il numero massimo di caratteri consentiti
     */
    private void limitTextArea(TextArea textArea, int maxChars) {
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > maxChars) {
                textArea.setText(oldValue);
            }
        });
    }

    /**
     * Imposta i dati iniziali per il controller.
     * Aggiorna le etichette nell'interfaccia con i dati dell'utente e del libro,
     * recupera l'ID del libro dal database e verifica se l'utente ha già valutato questo libro.
     *
     * @param userId L'ID dell'utente corrente
     * @param bookTitle Il titolo del libro da valutare
     * @param libraryName Il nome della biblioteca a cui appartiene il libro
     */
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
            // Gestione dell'eccezione omessa
        }

        // Controlla se l'utente ha già valutato questo libro
        checkExistingRating();
    }

    /**
     * Verifica se l'utente ha già valutato questo libro in precedenza.
     * Se esiste una valutazione precedente, carica i dati e aggiorna l'interfaccia.
     * Recupera le valutazioni e i commenti dal database per popolare l'interfaccia utente.
     */
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
            // Gestione dell'eccezione omessa
        }
    }

    /**
     * Gestisce l'evento di click sul pulsante "Back".
     * Torna alla schermata principale (homepage).
     *
     * @param event L'evento di click
     */
    @FXML
    public void handleBack(ActionEvent event) {
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

    /**
     * Gestisce l'evento di invio della valutazione.
     * Verifica che tutte le valutazioni e i commenti siano stati inseriti,
     * quindi salva i dati nel database e naviga al menu utente.
     *
     * @param event L'evento di click sul pulsante di invio
     */
    @FXML
    private void handleSubmit(ActionEvent event) {
        // Verifica che tutte le caratteristiche siano state valutate
        if (styleRating == 0 || contentRating == 0 || pleasantnessRating == 0 ||
                originalityRating == 0 || editionRating == 0) {

            // Mostra il messaggio di errore
            errorLabel.setText("Devi assegnare un voto e una descrizione a tutte le caratteristiche prima di inviare.");
            errorLabel.setVisible(true);
            return;
        }

        // Verifica che tutti i campi di commento siano stati compilati
        if (styleCommentArea.getText().trim().isEmpty() ||
                contentCommentArea.getText().trim().isEmpty() ||
                pleasantnessCommentArea.getText().trim().isEmpty() ||
                originalityCommentArea.getText().trim().isEmpty() ||
                editionCommentArea.getText().trim().isEmpty() ||
                finalCommentArea.getText().trim().isEmpty()) {

            // Mostra il messaggio di errore per commenti mancanti
            errorLabel.setText("Errore: Devi valutare e commentare ogni caratteristica prima di inviare.");
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

    /**
     * Salva o aggiorna la valutazione nel database.
     * Utilizza una query UPSERT per inserire una nuova valutazione o aggiornare una esistente.
     *
     * @return true se il salvataggio è avvenuto con successo, false altrimenti
     */
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
                pstmt.setString(8, finalCommentArea.getText().trim());
                pstmt.setString(9, styleCommentArea.getText().trim());
                pstmt.setString(10, contentCommentArea.getText().trim());
                pstmt.setString(11, pleasantnessCommentArea.getText().trim());
                pstmt.setString(12, originalityCommentArea.getText().trim());
                pstmt.setString(13, editionCommentArea.getText().trim());

                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Naviga al menu utente dopo il salvataggio della valutazione.
     * Carica la vista del menu utente e passa i dati dell'utente al controller.
     *
     * @param event L'evento che ha innescato la navigazione
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
            // Gestione dell'errore di navigazione
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Gestisce il click su una stella per la valutazione dello stile.
     * Aggiorna il valore della valutazione e l'aspetto delle stelle.
     *
     * @param event L'evento di click sulla stella
     */
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

    /**
     * Gestisce l'evento di click sul pulsante "Cancel".
     * Annulla l'operazione corrente e torna alla schermata di selezione del libro.
     *
     * @param event L'evento di click sul pulsante di annullamento
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        try {
            // Torna alla schermata di selezione libro
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/selezionaLibro.fxml"));
            Parent root = loader.load();

            BookSelectionController controller = loader.getController();
            controller.setData(userId, libraryName, "rate");

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Gestisce il click su una stella per la valutazione del contenuto.
     * Aggiorna il valore della valutazione e l'aspetto delle stelle.
     *
     * @param event L'evento di click sulla stella
     */
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

    /**
     * Gestisce il click su una stella per la valutazione della piacevolezza.
     * Aggiorna il valore della valutazione e l'aspetto delle stelle.
     *
     * @param event L'evento di click sulla stella
     */
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

    /**
     * Gestisce il click su una stella per la valutazione dell'originalità.
     * Aggiorna il valore della valutazione e l'aspetto delle stelle.
     *
     * @param event L'evento di click sulla stella
     */
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

    /**
     * Gestisce il click su una stella per la valutazione dell'edizione.
     * Aggiorna il valore della valutazione e l'aspetto delle stelle.
     *
     * @param event L'evento di click sulla stella
     */
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

    /**
     * Aggiorna l'aspetto visivo delle stelle in base alla valutazione.
     * Colora le stelle attive in giallo e quelle inattive in grigio.
     *
     * @param star1 La prima stella
     * @param star2 La seconda stella
     * @param star3 La terza stella
     * @param star4 La quarta stella
     * @param star5 La quinta stella
     * @param rating Il valore della valutazione (0-5)
     */
    private void updateStars(Text star1, Text star2, Text star3, Text star4, Text star5, int rating) {
        star1.setFill(javafx.scene.paint.Color.web(rating >= 1 ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR));
        star2.setFill(javafx.scene.paint.Color.web(rating >= 2 ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR));
        star3.setFill(javafx.scene.paint.Color.web(rating >= 3 ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR));
        star4.setFill(javafx.scene.paint.Color.web(rating >= 4 ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR));
        star5.setFill(javafx.scene.paint.Color.web(rating >= 5 ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR));
    }

    /**
     * Reimposta tutte le stelle allo stato inattivo (grigio).
     * Utilizzato durante l'inizializzazione o per azzerare le valutazioni.
     */
    private void resetAllStars() {
        updateStars(styleStar1, styleStar2, styleStar3, styleStar4, styleStar5, 0);
        updateStars(contentStar1, contentStar2, contentStar3, contentStar4, contentStar5, 0);
        updateStars(pleasantnessStar1, pleasantnessStar2, pleasantnessStar3, pleasantnessStar4, pleasantnessStar5, 0);
        updateStars(originalityStar1, originalityStar2, originalityStar3, originalityStar4, originalityStar5, 0);
        updateStars(editionStar1, editionStar2, editionStar3, editionStar4, editionStar5, 0);
        updateStars(averageStar1, averageStar2, averageStar3, averageStar4, averageStar5, 0);
    }

    /**
     * Calcola e aggiorna la valutazione media del libro.
     * Considera solo le categorie che sono state valutate (valore > 0).
     * Aggiorna sia l'etichetta con il valore numerico sia le stelle nella UI.
     */
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

            if (count > 0) {
                double average = (double) sum / count;
                DecimalFormat df = new DecimalFormat("#.#");
                averageRatingLabel.setText(df.format(average));

                int roundedAverage = (int) Math.round(average);
                updateStars(averageStar1, averageStar2, averageStar3, averageStar4, averageStar5, roundedAverage);
            }
        }
    }
}