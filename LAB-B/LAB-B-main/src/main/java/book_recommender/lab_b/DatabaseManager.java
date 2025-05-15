package book_recommender.lab_b;

import java.sql.*;
import java.util.Random;

/**
 * Singleton class for managing database connections.
 * This class provides a centralized way to access the database
 * and can automatically create the database if it doesn't exist.
 * It also supports remote connections from any network using ngrok.
 */
public class DatabaseManager {
    // Database connection settings - default values
    private static final String DEFAULT_HOST = "localhost"; // Modificato da 0.0.0.0 a localhost
    private static final String DEFAULT_PORT = "5432";
    private static final String DEFAULT_DB_NAME = "book_recommender";

    // Connection strings - these will be updated at runtime for remote connections
    private static String DB_URL = "jdbc:postgresql://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/" + DEFAULT_DB_NAME;

    // User credentials - fixed values
    private static String DB_USER = "book_admin_8530";
    private static String DB_PASSWORD = "CPuc#@r-zbKY";

    private static DatabaseManager instance;
    private Connection connection;

    /**
     * Private constructor standard
     * @throws SQLException if a database access error occurs
     */
    private DatabaseManager() throws SQLException {
        // Call the constructor with parameter
        this(false);
    }

    /**
     * Private constructor with parameters for a connection type
     * @param isRemote flag to indicate if this is a remote connection
     * @throws SQLException if a database access error occurs
     */
    private DatabaseManager(boolean isRemote) throws SQLException {

        if (isRemote) {
            // For remote connections, we rely on the DB_URL, DB_USER, and DB_PASSWORD
            // values that have been set externally before calling this constructor
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            } catch (SQLException e) {
                throw new SQLException("Failed to connect to remote database: " + e.getMessage(), e);
            }
        } else {
            // Per connessioni locali, prova diverse combinazioni host
            initializeLocalConnection();
        }
    }

    /**
     * Initializes a local connection with credential management
     * @throws SQLException if a database access error occurs
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
     * Setup access in pg_hba.conf through SQL (if possible)
     */
    private void setupPgHbaAccess(Connection conn) {
        try {
            // This is a PostgreSQL-specific way to reload the configuration
            try (Statement stmt = conn.createStatement()) {
                // Set listen_addresses to '*' to accept connections from all interfaces
                stmt.execute("ALTER SYSTEM SET listen_addresses = '*'");

                // Add trust for both IPv4 and IPv6 local connections
                // Note: This is not the most secure approach but works for development

                // Check if we have permission to reload

                    stmt.execute("SELECT pg_reload_conf()");


            }
        } catch (SQLException e) {
        }
    }

    /**
     * Creates a remote instance with specific connection parameters
     * @param jdbcUrl The full JDBC URL for connecting to the database
     * @param username The username for the database connection (fixed to book_admin_8530)
     * @param password The password for the database connection (fixed to CPuc#@r-zbKY)
     * @return A DatabaseManager instance configured for a remote connection
     * @throws SQLException if a database access error occurs
     */
    public static synchronized DatabaseManager createRemoteInstance(String jdbcUrl, String username, String password) throws SQLException {
        // If there's an existing instance, close it
        if (instance != null) {
            instance.closeConnection();
            instance = null;
        }

        // Set the connection parameters
        DB_URL = jdbcUrl;
        // We still use the parameters passed, even though they should be the fixed values
        DB_USER = username;
        DB_PASSWORD = password;

        // Create a new instance with isRemote = true
        instance = new DatabaseManager(true);
        return instance;
    }

    /**
     * Creates a database user if it doesn't already exist
     */
    private void createUserIfNotExists(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Check if a user exists
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_roles WHERE rolname = '" + DB_USER + "'");
            if (!rs.next()) {
                // User doesn't exist, create it
                stmt.execute("CREATE USER " + DB_USER + " WITH PASSWORD '" + DB_PASSWORD + "'");
                stmt.execute("ALTER USER " + DB_USER + " WITH LOGIN CREATEDB NOSUPERUSER INHERIT");

                // Try to create a read-only user
                try {
                    stmt.execute("CREATE ROLE book_reader WITH LOGIN PASSWORD 'reader2024' NOSUPERUSER INHERIT NOCREATEROLE NOREPLICATION");
                    stmt.execute("GRANT CONNECT ON DATABASE " + DEFAULT_DB_NAME + " TO book_reader");
               } catch (SQLException e) {
                    // Ignore if we can't create book_reader user
               }
            } else {
                // User exists, update password
                stmt.execute("ALTER USER " + DB_USER + " WITH PASSWORD '" + DB_PASSWORD + "'");
                stmt.execute("ALTER USER " + DB_USER + " WITH CREATEDB");

           }

            // Importante: Concedi tutti i privilegi all'utente
            stmt.execute("ALTER USER " + DB_USER + " CONNECTION LIMIT -1"); // No connection limit

            // We need to grant privileges AFTER the database is created
            try {
                // Grant privileges on the template database (will be inherited by new databases)
                stmt.execute("GRANT ALL PRIVILEGES ON DATABASE postgres TO " + DB_USER);
            } catch (SQLException e) {
            }
        }
    }

    /**
     * Updates the connected client count in the database.
     * This creates a table to track client connections if it doesn't exist.
     *
     * @param clientId A unique identifier for the client
     * @param isConnecting true if a client is connecting, false if disconnecting
     * @throws SQLException if database error occurs
     */
    public void updateClientConnection(String clientId, boolean isConnecting) throws SQLException {
        Connection conn = getConnection();

        // Ensure the active_clients table exists
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS active_clients (" +
                            "    client_id VARCHAR(50) PRIMARY KEY," +
                            "    connect_time TIMESTAMP NOT NULL" +
                            ")"
            );
        }

        if (isConnecting) {
            // Add client to active_clients table
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO active_clients (client_id, connect_time) VALUES (?, NOW()) " +
                            "ON CONFLICT (client_id) DO UPDATE SET connect_time = NOW()")) {
                pstmt.setString(1, clientId);
                pstmt.executeUpdate();
            }
        } else {
            // Remove client from active_clients table
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM active_clients WHERE client_id = ?")) {
                pstmt.setString(1, clientId);
                pstmt.executeUpdate();
            }
        }
    }

    /**
     * Gets the current count of connected clients
     *
     * @return The number of active clients
     * @throws SQLException if database error occurs
     */
    public int getConnectedClientCount() throws SQLException {
        Connection conn = getConnection();
        int count = 0;

        // Check if the table exists
        try {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM active_clients");
                if (rs.next()) {
                    count = rs.getInt(1);
                    return count;
                }
            }
        } catch (SQLException e) {
            // Table might not exist yet
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
     * Gets the singleton instance of the DatabaseManager.
     * Creates the instance if it doesn't exist.
     *
     * @return the singleton instance
     * @throws SQLException if a database access error occurs
     */
    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Gets a connection to the database.
     * If the connection is closed, it creates a new one.
     *
     * @return a connection to the database
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * Returns the current database user
     */
    public String getDbUser() {
        return DB_USER;
    }

    /**
     * Returns the current database password
     */
    public String getDbPassword() {
        return DB_PASSWORD;
    }

    /**
     * Returns the default port used for database connections
     */
    public static String getDefaultPort() {
        return DEFAULT_PORT;
    }
}