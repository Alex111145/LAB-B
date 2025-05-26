package book_recommender.lab_b;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller che gestisce la schermata mostrata quando la connessione al server viene persa.
 * Questa schermata appare quando il client rileva che il server non è più raggiungibile,
 * permettendo all'utente di chiudere l'applicazione in modo ordinato.
 */
public class ServerDisconnectedController {

    @FXML
    public void onCloseApplication(ActionEvent event) {
        // Chiude ordinatamente l'ambiente JavaFX
        Platform.exit();
        // Forza l'uscita del processo Java con codice di uscita 0 (successo)
        System.exit(0);
    }

    /**
     * Metodo di inizializzazione chiamato automaticamente da JavaFX
     * quando il file FXML associato viene caricato.
     * Attualmente non esegue alcuna operazione specifica ma può essere
     * esteso per configurare aspetti iniziali dell'interfaccia utente.
     */
    @FXML
    public void initialize() {
        // Questo metodo è un punto di estensione per eventuali
        // inizializzazioni future dell'interfaccia
    }
}