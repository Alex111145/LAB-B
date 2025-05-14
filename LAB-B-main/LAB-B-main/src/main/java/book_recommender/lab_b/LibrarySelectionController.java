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
 * Controller per la schermata di selezione delle librerie.
 * Permette all'utente di selezionare una libreria tra quelle disponibili
 * per procedere con diverse operazioni (visualizzazione, valutazione,
 * consigli o aggiunta libri).
 */
public class LibrarySelectionController {

    @FXML private Label userIdLabel;
    @FXML private ListView<String> librariesListView;
    @FXML private Label noLibrariesLabel;
    @FXML private Label errorLabel;
    @FXML private Button selectButton;

    private String userId;
    private final List<String> libraries = new ArrayList<>();
    private String operationType = "select";
    private DatabaseManager dbManager;

    /**
     * Costruttore del controller.
     * Inizializza la connessione al database manager.
     */
    public LibrarySelectionController() {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Gestione silenziosa dell'errore - verrà gestito in altri metodi
        }
    }

    /**
     * Inizializza l'interfaccia utente dopo che l'FXML è stato caricato.
     * Nasconde l'etichetta di errore e configura il listener per i tasti.
     */
    public void initialize() {
        errorLabel.setVisible(false);
        noLibrariesLabel.setVisible(false);
        librariesListView.setOnKeyPressed(this::handleKeyPress);
    }

    /**
     * Gestisce la pressione dei tasti nella ListView.
     * Quando l'utente preme ENTER, simula il click sul pulsante Seleziona.
     *
     * @param event L'evento di pressione tasto
     */
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String selectedLibrary = librariesListView.getSelectionModel().getSelectedItem();
            if (selectedLibrary != null) {
                // Simula il click sul pulsante seleziona
                handleSelect(new ActionEvent(event.getSource(), null));
            }
        }
    }

    /**
     * Imposta l'ID utente nel controller e carica le librerie dell'utente.
     * Aggiorna l'interfaccia in base alla presenza o meno di librerie.
     *
     * @param userId ID dell'utente corrente
     */
    public void setUserId(String userId) {
        this.userId = userId;
        userIdLabel.setText(userId);
        loadUserLibraries(userId);

        // Aggiorna UI in base alla presenza di librerie
        if (libraries.isEmpty()) {
            noLibrariesLabel.setVisible(true);
            selectButton.setDisable(true);
        } else {
            noLibrariesLabel.setVisible(false);
            selectButton.setDisable(false);
        }
    }

    /**
     * Versione estesa del metodo setUserId che consente di specificare
     * anche il tipo di operazione che sarà eseguita dopo la selezione.
     * Modifica l'etichetta del pulsante in base all'operazione.
     *
     * @param userId ID dell'utente corrente
     * @param operationType Tipo di operazione da eseguire ("rate", "recommend", "add")
     */
    public void setUserId(String userId, String operationType) {
        this.operationType = operationType;

        // Modifica il testo del pulsante in base all'operazione
        if ("rate".equals(operationType)) {
            selectButton.setText("Avanti");
        } else if ("recommend".equals(operationType)) {
            selectButton.setText("Consiglia");
        } else if ("add".equals(operationType)) {
            selectButton.setText("Avanti");
        }

        // Chiama il metodo base per impostare l'ID e caricare le librerie
        setUserId(userId);
    }

    /**
     * Carica tutte le librerie appartenenti all'utente dal database.
     * Aggiorna la lista di librerie e la ListView.
     *
     * @param userId ID dell'utente di cui caricare le librerie
     */
    private void loadUserLibraries(String userId) {
        libraries.clear();
        librariesListView.getItems().clear();

        String sql = "SELECT library_name FROM libraries WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String libraryName = rs.getString("library_name");
                if (!libraries.contains(libraryName)) {
                    libraries.add(libraryName);
                }
            }

            if (!libraries.isEmpty()) {
                librariesListView.getItems().addAll(libraries);
            }

        } catch (SQLException e) {
            errorLabel.setText("Impossibile caricare le librerie. " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Gestisce l'evento di selezione di una libreria.
     * In base al tipo di operazione, naviga alla schermata appropriata.
     *
     * @param event L'evento di azione generato dal pulsante o dalla tastiera
     */
    @FXML
    public void handleSelect(ActionEvent event) {
        String selectedLibrary = librariesListView.getSelectionModel().getSelectedItem();

        // Verifica che sia stata selezionata una libreria
        if (selectedLibrary == null) {
            errorLabel.setText("Seleziona una libreria prima di procedere.");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setVisible(false);

        // Naviga alla schermata appropriata in base al tipo di operazione
        switch (operationType) {
            case "rate" -> navigateToBookSelection(event, selectedLibrary, "rate");
            case "recommend" -> navigateToBookSelection(event, selectedLibrary, "recommend");
            case "add" -> navigateToAddBooks(event, selectedLibrary);
            case null, default -> navigateToBookSelection(event, selectedLibrary);
        }
    }

    /**
     * Versione base del metodo per navigare alla schermata di selezione libri.
     *
     * @param event L'evento di azione per il cambio di scena
     * @param libraryName Il nome della libreria selezionata
     */
    private void navigateToBookSelection(ActionEvent event, String libraryName) {
        navigateToBookSelection(event, libraryName, null);
    }

    /**
     * Naviga alla schermata di selezione libri con un tipo di operazione specifico.
     * Carica il controller BookSelectionController e passa i dati necessari.
     *
     * @param event L'evento di azione per il cambio di scena
     * @param libraryName Il nome della libreria selezionata
     * @param operationType Il tipo di operazione da eseguire (può essere null)
     */
    private void navigateToBookSelection(ActionEvent event, String libraryName, String operationType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/selezionaLibro.fxml"));
            Parent root = loader.load();

            // Ottiene il controller e passa i dati
            BookSelectionController controller = loader.getController();
            if (operationType != null) {
                controller.setData(userId, libraryName, operationType);
            } else {
                controller.setData(userId, libraryName);
            }

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            // Gestione errori di caricamento vista
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Naviga alla schermata di aggiunta libri per la libreria selezionata.
     * Carica il controller AddBooksToLibraryController e passa i dati necessari.
     *
     * @param event L'evento di azione per il cambio di scena
     * @param libraryName Il nome della libreria selezionata
     */
    private void navigateToAddBooks(ActionEvent event, String libraryName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/aggiungilibro.fxml"));
            Parent root = loader.load();

            // Ottiene il controller e passa i dati
            AddBooksToLibraryController controller = loader.getController();
            controller.setData(userId, libraryName);

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            // Gestione errori di caricamento vista
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Gestisce l'evento di click sul pulsante "Indietro".
     * Torna al menu utente.
     *
     * @param event L'evento di azione generato dal pulsante
     */
    @FXML
    public void handleBack(ActionEvent event) {
        navigateToUserMenu(event);
    }

    /**
     * Gestisce l'evento di click sul pulsante "Annulla".
     * Torna al menu utente.
     *
     * @param event L'evento di azione generato dal pulsante
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        navigateToUserMenu(event);
    }

    /**
     * Naviga al menu utente.
     * Carica il controller UserMenuController e passa l'ID utente.
     *
     * @param event L'evento di azione per il cambio di scena
     */
    private void navigateToUserMenu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/book_recommender/lab_b/userMenu.fxml"));
            Parent root = loader.load();

            // Ottiene il controller e passa i dati
            UserMenuController controller = loader.getController();
            controller.setUserData(userId);

            // Cambia la scena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            // Gestione errori di caricamento vista
            errorLabel.setText("Errore: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
}