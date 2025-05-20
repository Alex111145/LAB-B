package book_recommender.lab_b;

import java.sql.*;
import java.util.Random;

/**
 * Classe Singleton per la gestione delle connessioni al database.
 * Questa classe fornisce un modo centralizzato per accedere al database
 * e può creare automaticamente il database se non esiste.
 * Supporta anche connessioni remote da qualsiasi rete utilizzando ngrok.
 *
 * <p>La classe DatabaseManager segue il pattern Singleton assicurando che solo una
 * connessione al database sia attiva in qualsiasi momento. Gestisce vari scenari di connessione
 * incluse connessioni locali e remote, ed esegue la creazione automatica del database
 * e la configurazione dell'utente quando necessario.</p>
 *
 * <p>Le credenziali di connessione sono predefinite per sicurezza e coerenza tra
 * le istanze dell'applicazione.</p>
 *
 * @author book_recommender.lab_b
 * @version 1.0
 */
public class DatabaseManager {
    /**
     * Host predefinito per la connessione al database locale.
     * Modificato da 0.0.0.0 a localhost per una migliore compatibilità.
     */
    private static final String DEFAULT_HOST = "localhost";

    /**
     * Porta predefinita per la connessione al database PostgreSQL.
     * La porta standard di PostgreSQL è 5432.
     */
    private static final String DEFAULT_PORT = "5432";

    /**
     * Nome predefinito del database per l'applicazione.
     * Questo database verrà creato se non esiste.
     */
    private static final String DEFAULT_DB_NAME = "book_recommender";

    /**
     * URL JDBC per la connessione al database.
     * Questo sarà aggiornato dinamicamente durante l'esecuzione per le connessioni remote.
     */
    private static String DB_URL = "jdbc:postgresql://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/" + DEFAULT_DB_NAME;

    /**
     * Nome utente per l'autenticazione al database.
     * Valore fisso per sicurezza e coerenza.
     */
    private static String DB_USER = "book_admin_8530";

    /**
     * Password per l'autenticazione al database.
     * Valore fisso per sicurezza e coerenza.
     */
    private static String DB_PASSWORD = "CPuc#@r-zbKY";

    /**
     * Istanza Singleton del DatabaseManager.
     */
    private static DatabaseManager instance;

    /**
     * Connessione attiva al database.
     */
    private Connection connection;

    /**
     * Costruttore privato per impedire l'istanziazione diretta (pattern Singleton).
     * Utilizza le impostazioni di connessione locale predefinite.
     *
     * @throws SQLException se si verifica un errore di accesso al database durante la connessione
     */
    private DatabaseManager() throws SQLException {
        // Chiama il costruttore con parametro
        this(false);
    }

    /**
     * Costruttore privato con parametro di tipo di connessione.
     * Supporta sia connessioni locali che remote.
     *
     * @param isRemote flag per indicare se questa è una connessione remota
     * @throws SQLException se si verifica un errore di accesso al database durante la connessione
     */
    private DatabaseManager(boolean isRemote) throws SQLException {

        if (isRemote) {
            // Per le connessioni remote, ci affidiamo ai valori DB_URL, DB_USER e DB_PASSWORD
            // che sono stati impostati esternamente prima di chiamare questo costruttore
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            } catch (SQLException e) {
                throw new SQLException("Impossibile connettersi al database remoto: " + e.getMessage(), e);
            }
        } else {
            // Per connessioni locali, prova diverse combinazioni host
            initializeLocalConnection();
        }
    }

    /**
     * Inizializza una connessione locale al database con gestione delle credenziali.
     * Questo metodo tenta di connettersi utilizzando più indirizzi host (localhost, 127.0.0.1)
     * e gestisce la creazione del database e dell'utente se non esistono.
     *
     * @throws SQLException se non è possibile stabilire una connessione con nessun host configurato
     */
    private void initializeLocalConnection() throws SQLException {
        // Array di possibili host da provare in sequenza
        String[] hostsToTry = {"localhost", "127.0.0.1"};
        SQLException lastException = null;

        // Prova ciascun host in sequenza
        for (String host : hostsToTry) {
            String currentUrl = "jdbc:postgresql://" + host + ":" + DEFAULT_PORT + "/" + DEFAULT_DB_NAME;
            try {
                // Prova a connettersi direttamente al database
                connection = DriverManager.getConnection(currentUrl, DB_USER, DB_PASSWORD);

                // Connessione riuscita, aggiorna l'URL del database
                DB_URL = currentUrl;
                return;
            } catch (SQLException e) {
                lastException = e;

                try {
                    // Prova a connettersi al database postgres per creare il nostro database e utente
                    Connection postgresConn;
                    String POSTGRES_URL = "jdbc:postgresql://" + host + ":" + DEFAULT_PORT + "/postgres";

                    try {
                        // Prima, prova a connettersi come superuser postgres (default comune)
                        postgresConn = DriverManager.getConnection(POSTGRES_URL, "postgres", "postgres");

                    } catch (SQLException postgresError) {
                        // Se non riesci a connetterti come postgres, prova con l'utente OS corrente
                        String osUser = System.getProperty("user.name");
                        try {
                            postgresConn = DriverManager.getConnection(POSTGRES_URL, osUser, "");

                        } catch (SQLException osUserError) {
                            // Se entrambi falliscono, passa all'host successivo
                            continue;
                        }
                    }

                    // Se siamo qui, abbiamo una connessione al database postgres
                    if (postgresConn != null) {
                        // Aggiorna l'URL del database e imposta le autorizzazioni
                        DB_URL = currentUrl;

                        // Aggiungi autorizzazione in pg_hba.conf attraverso SQL
                        setupPgHbaAccess(postgresConn);

                        // Crea l'utente se non esiste già
                        createUserIfNotExists(postgresConn);

                        // Verifica se il database esiste già
                        boolean dbExists;
                        try (Statement checkStmt = postgresConn.createStatement()) {
                            ResultSet rs = checkStmt.executeQuery(
                                    "SELECT 1 FROM pg_database WHERE datname = '" + DEFAULT_DB_NAME + "'");
                            dbExists = rs.next();
                        }

                        if (!dbExists) {
                            // Crea il database con il nuovo utente come proprietario
                            try (Statement createStmt = postgresConn.createStatement()) {
                                createStmt.execute("CREATE DATABASE " + DEFAULT_DB_NAME + " WITH OWNER = " + DB_USER);
                            }
                        } else {
                            // Cambia la proprietà del database se necessario
                            try (Statement grantStmt = postgresConn.createStatement()) {
                                grantStmt.execute("ALTER DATABASE " + DEFAULT_DB_NAME + " OWNER TO " + DB_USER);
                            } catch (SQLException grantError) {
                                // Ignora errori di modifica proprietà se non hai permessi sufficienti
                            }
                        }

                        postgresConn.close();

                        // Ora prova a connetterti al database con il nostro utente
                        try {
                            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                            return; // Connessione riuscita, esci dal metodo
                        } catch (SQLException connError) {
                            // Se non riusciamo a connetterci con le nuove credenziali, passa all'host successivo
                        }
                    }
                } catch (SQLException postgresError) {
                    // Registra l'errore e passa all'host successivo
                }
            }
        }

        // Se arriviamo qui, nessun host ha funzionato
        throw new SQLException("Impossibile connettersi a PostgreSQL su nessun host. Ultimo errore: " +
                (lastException != null ? lastException.getMessage() : "Sconosciuto"));
    }

    /**
     * Configura l'accesso in pg_hba.conf tramite SQL (se possibile).
     * Questo metodo tenta di configurare PostgreSQL per accettare connessioni da
     * tutti gli indirizzi IP, facilitando l'accesso remoto.
     *
     * @param conn La connessione al database postgres
     */
    private void setupPgHbaAccess(Connection conn) {
        try {
            // Questo è un modo specifico di PostgreSQL per ricaricare la configurazione
            try (Statement stmt = conn.createStatement()) {
                // Imposta listen_addresses a '*' per accettare connessioni da tutte le interfacce
                stmt.execute("ALTER SYSTEM SET listen_addresses = '*'");

                // Aggiungi trust per connessioni locali sia IPv4 che IPv6
                // Nota: Questo non è l'approccio più sicuro ma funziona per lo sviluppo

                // Controlla se abbiamo il permesso di ricaricare
                stmt.execute("SELECT pg_reload_conf()");
            }
        } catch (SQLException e) {
            // Ignora errori - potremmo non avere i permessi necessari
        }
    }

    /**
     * Crea un'istanza remota con parametri di connessione specifici.
     * Questo metodo consente di configurare una connessione a un database remoto
     * con URL e credenziali specifiche.
     *
     * @param jdbcUrl L'URL JDBC completo per la connessione al database
     * @param username Il nome utente per la connessione al database (fisso a book_admin_8530)
     * @param password La password per la connessione al database (fisso a CPuc#@r-zbKY)
     * @return Un'istanza di DatabaseManager configurata per una connessione remota
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public static synchronized DatabaseManager createRemoteInstance(String jdbcUrl, String username, String password) throws SQLException {
        // Se esiste già un'istanza, chiudila
        if (instance != null) {
            instance.closeConnection();
            instance = null;
        }

        // Imposta i parametri di connessione
        DB_URL = jdbcUrl;
        // Usiamo comunque i parametri passati, anche se dovrebbero essere i valori fissi
        DB_USER = username;
        DB_PASSWORD = password;

        // Crea una nuova istanza con isRemote = true
        instance = new DatabaseManager(true);
        return instance;
    }

    /**
     * Crea un utente del database se non esiste già.
     * Questo metodo verifica se l'utente esiste nel database PostgreSQL e,
     * se necessario, lo crea con i permessi appropriati.
     *
     * @param conn La connessione al database postgres
     * @throws SQLException se si verifica un errore durante la creazione dell'utente
     */
    private void createUserIfNotExists(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Controlla se esiste un utente
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_roles WHERE rolname = '" + DB_USER + "'");
            if (!rs.next()) {
                // L'utente non esiste, crealo
                stmt.execute("CREATE USER " + DB_USER + " WITH PASSWORD '" + DB_PASSWORD + "'");
                stmt.execute("ALTER USER " + DB_USER + " WITH LOGIN CREATEDB NOSUPERUSER INHERIT");

                // Prova a creare un utente di sola lettura
                try {
                    stmt.execute("CREATE ROLE book_reader WITH LOGIN PASSWORD 'reader2024' NOSUPERUSER INHERIT NOCREATEROLE NOREPLICATION");
                    stmt.execute("GRANT CONNECT ON DATABASE " + DEFAULT_DB_NAME + " TO book_reader");
                } catch (SQLException e) {
                    // Ignora se non possiamo creare l'utente book_reader
                }
            } else {
                // L'utente esiste, aggiorna la password
                stmt.execute("ALTER USER " + DB_USER + " WITH PASSWORD '" + DB_PASSWORD + "'");
                stmt.execute("ALTER USER " + DB_USER + " WITH CREATEDB");
            }

            // Importante: Concedi tutti i privilegi all'utente
            stmt.execute("ALTER USER " + DB_USER + " CONNECTION LIMIT -1"); // Nessun limite di connessione

            // Dobbiamo concedere privilegi DOPO che il database è stato creato
            try {
                // Concedi privilegi sul database template (sarà ereditato dai nuovi database)
                stmt.execute("GRANT ALL PRIVILEGES ON DATABASE postgres TO " + DB_USER);
            } catch (SQLException e) {
                // Ignora - potremmo non avere i permessi necessari
            }
        }
    }

    /**
     * Aggiorna il conteggio dei client connessi nel database.
     * Questo crea una tabella per tracciare le connessioni dei client se non esiste.
     *
     * @param clientId Un identificatore univoco per il client
     * @param isConnecting true se un client si sta connettendo, false se si sta disconnettendo
     * @throws SQLException se si verifica un errore del database
     */
    public void updateClientConnection(String clientId, boolean isConnecting) throws SQLException {
        Connection conn = getConnection();

        // Assicurati che la tabella active_clients esista
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS active_clients (" +
                            "    client_id VARCHAR(50) PRIMARY KEY," +
                            "    connect_time TIMESTAMP NOT NULL" +
                            ")"
            );
        }

        if (isConnecting) {
            // Aggiungi client alla tabella active_clients
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO active_clients (client_id, connect_time) VALUES (?, NOW()) " +
                            "ON CONFLICT (client_id) DO UPDATE SET connect_time = NOW()")) {
                pstmt.setString(1, clientId);
                pstmt.executeUpdate();
            }
        } else {
            // Rimuovi client dalla tabella active_clients
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM active_clients WHERE client_id = ?")) {
                pstmt.setString(1, clientId);
                pstmt.executeUpdate();
            }
        }
    }

    /**
     * Ottiene il conteggio attuale dei client connessi.
     *
     * @return Il numero di client attivi
     * @throws SQLException se si verifica un errore del database
     */
    public int getConnectedClientCount() throws SQLException {
        Connection conn = getConnection();
        int count = 0;

        // Controlla se la tabella esiste
        try {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM active_clients WHERE client_id NOT LIKE 'user_%';");
                if (rs.next()) {
                    count = rs.getInt(1);
                    return count;
                }
            }
        } catch (SQLException e) {
            // La tabella potrebbe non esistere ancora
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS active_clients (" +
                                "    client_id VARCHAR(50) PRIMARY KEY," +
                                "    connect_time TIMESTAMP NOT NULL" +
                                ")"
                );
            }
        }

        return count;
    }

    /**
     * Ottiene l'istanza singleton del DatabaseManager.
     * Crea l'istanza se non esiste.
     *
     * @return l'istanza singleton
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Ottiene una connessione al database.
     * Se la connessione è chiusa, ne crea una nuova.
     *
     * @return una connessione al database
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
        return connection;
    }

    /**
     * Chiude la connessione al database.
     * Questo metodo dovrebbe essere chiamato quando l'applicazione termina
     * per rilasciare le risorse del database.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Ignora errori durante la chiusura
            }
        }
    }

    /**
     * Restituisce l'utente del database corrente.
     *
     * @return Il nome utente utilizzato per le connessioni al database
     */
    public String getDbUser() {
        return DB_USER;
    }

    /**
     * Restituisce la password del database corrente.
     *
     * @return La password utilizzata per le connessioni al database
     */
    public String getDbPassword() {
        return DB_PASSWORD;
    }

    /**
     * Restituisce la porta predefinita utilizzata per le connessioni al database.
     *
     * @return La porta predefinita del database come stringa
     */
    public static String getDefaultPort() {
        return DEFAULT_PORT;
    }
}