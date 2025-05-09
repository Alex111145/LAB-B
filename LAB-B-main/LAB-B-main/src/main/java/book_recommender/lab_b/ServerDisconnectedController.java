package book_recommender.lab_b;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller per la schermata di disconnessione dal server.
 */
public class ServerDisconnectedController {

    @FXML
    private Button closeButton;

    @FXML
    public void onCloseApplication(ActionEvent event) {
        // Chiudi l'applicazione
        Platform.exit();
        System.exit(0);
    }

    @FXML
    public void initialize() {
        // Inizializzazione
    }
}