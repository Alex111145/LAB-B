# Book Recommender - Guida all'avvio

Questa applicazione è composta da due componenti principali:
1. Un **server** che gestisce il database e le connessioni
2. Un **client** che fornisce l'interfaccia utente per interagire con il sistema

## Requisiti

- **Java 21 o superiore**
- **Maven** correttamente installato e configurato
- **JavaFX** (sarà automaticamente utilizzato da Maven)

## Informazione importante
**L'applicazione è già compilata e pronta all'uso.
** Non è necessario ricompilarla prima dell'esecuzione.

## Avvio dell'applicazione

### Su macOS/Linux

1. Apri il Terminale e naviga fino alla cartella principale del progetto:
   ```bash
   cd /percorso/al/progetto/LAB-B-main
   ```

2. Avvia il server:
   ```bash
   ./bin/start-server.sh
   ```

3. In una nuova finestra del Terminale, avvia il client:
   ```bash
   ./bin/start-client.sh
   ```

### Su Windows

1. Apri il Prompt dei comandi (cmd) e naviga fino alla cartella principale del progetto:
   ```cmd
   cd C:\percorso\al\progetto\LAB-B-main
   ```

2. Avvia il server:
   ```cmd
   bin\start-server.bat
   ```

3. In una nuova finestra del Prompt dei comandi, avvia il client:
   ```cmd
   bin\start-client.bat
   ```


## Risoluzione dei problemi

### Problemi comuni su macOS/Linux

1. **"Permission denied" durante l'esecuzione degli script**
   - Soluzione: Rendi eseguibili gli script con `chmod +x bin/*.sh`

2. **"mvn: command not found"**
   - Soluzione: Assicurati che Maven sia installato e nel PATH
   - Prova a installare Maven con:
     ```bash
     # Su macOS con Homebrew
     brew install maven
     
     # Su Ubuntu/Debian
     sudo apt install maven
     ```

3. **Problemi di rendering JavaFX**
   - Soluzione: Gli script sono già configurati per usare il renderer software per migliore compatibilità

### Problemi comuni su Windows

1. **"'mvn' non è riconosciuto come comando interno o esterno"**
   - Soluzione: Assicurati che Maven sia installato e nel PATH
   - Verifica l'installazione eseguendo `mvn -version` in un Prompt dei comandi

2. **Problemi di rendering JavaFX**
   - Soluzione: Gli script sono già configurati per usare il renderer software per migliore compatibilità

3. **La finestra dell'applicazione non si apre**
   - Soluzione: Controlla il Prompt dei comandi per eventuali messaggi di errore
   - Verifica di avere Java 21 installato eseguendo `java -version`

### Altre soluzioni

Se l'applicazione non si avvia correttamente:

1. Verifica che Java 21 sia installato e configurato correttamente
2. Assicurati che non ci siano altri server in esecuzione sulla stessa porta
3. Prova a chiudere e riavviare completamente l'applicazione
4. Se necessario, puoi ricompilare manualmente il progetto con Maven, ma in genere non è necessario poiché l'applicazione è già compilata

## Struttura del progetto

```

LAB-B-main/
├── bin/
│   ├── start-client.sh    # Script per avviare il client (macOS/Linux)
│   ├── start-client.bat   # Script per avviare il client (Windows)
│   ├── start-server.sh    # Script per avviare il server (macOS/Linux)
│   └── start-server.bat   # Script per avviare il server (Windows)
├── src/                   # Codice sorgente
├── target/                # Cartella con i file già compilati
└── pom.xml                # File di configurazione Maven
```
