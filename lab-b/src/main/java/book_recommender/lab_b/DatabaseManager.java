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
    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final String DEFAULT_PORT = "5432";
    private static final String DEFAULT_DB_NAME = "book_recommender";

    // Connection strings - these will be updated at runtime for remote connections
    private static String DB_URL = "jdbc:postgresql://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/" + DEFAULT_DB_NAME;

    // User credentials will be generated dynamically or loaded from saved settings
    private static String DB_USER = "postgres"; // Fallback default
    private static String DB_PASSWORD = "postgres"; // Fallback default

    // File to store credentials
    private static final String CREDENTIALS_FILE = "db_credentials.properties";

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
                System.out.println("Successfully connected to remote database: " + DB_URL);
            } catch (SQLException e) {
                throw new SQLException("Failed to connect to remote database: " + e.getMessage(), e);
            }
        } else {
            // For local connections, use the standard approach with credential loading
            initializeLocalConnection();
        }
    }

    /**
     * Initializes a local connection with credential management
     * @throws SQLException if a database access error occurs
     */
    private void initializeLocalConnection() throws SQLException {
        // Try to load or create credentials
        loadOrCreateCredentials();

        try {
            // Try to connect directly to the database PostgreSQL
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Successfully connected to database using existing credentials");
        } catch (SQLException e) {
            try {
                // Try to connect to the postgres database to create our database and user
                Connection postgresConn;

                String POSTGRES_URL = "jdbc:postgresql://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/postgres";
                try {
                    // First, try to connect as superuser postgres (common default)
                    postgresConn = DriverManager.getConnection(POSTGRES_URL, "postgres", "postgres");
                    System.out.println("Connected to PostgreSQL as postgres user");
                } catch (SQLException postgresError) {
                    // If we can't connect as postgres, we'll try with the current OS user
                    String osUser = System.getProperty("user.name");
                    try {
                        postgresConn = DriverManager.getConnection(POSTGRES_URL, osUser, "");
                        System.out.println("Connected to PostgreSQL as OS user: " + osUser);
                    } catch (SQLException osUserError) {
                        // If all else fails, throw the original error
                        throw new SQLException("Impossibile connettersi a PostgreSQL: " + e.getMessage() +
                                "\nTentativo postgres fallito: " + postgresError.getMessage() +
                                "\nTentativo con utente OS fallito: " + osUserError.getMessage());
                    }
                }

                // Now we have a connection to the postgres database
                if (postgresConn != null) {
                    // Create the user if it doesn't exist
                    createUserIfNotExists(postgresConn);

                    // Verify if the database exists already
                    boolean dbExists;
                    try (Statement checkStmt = postgresConn.createStatement()) {
                        ResultSet rs = checkStmt.executeQuery(
                                "SELECT 1 FROM pg_database WHERE datname = '" + DEFAULT_DB_NAME + "'");
                        dbExists = rs.next();
                    }

                    if (!dbExists) {
                        // Create the database with the new user as an owner
                        try (Statement createStmt = postgresConn.createStatement()) {
                            createStmt.execute("CREATE DATABASE " + DEFAULT_DB_NAME + " WITH OWNER = " + DB_USER);
                            System.out.println("Database " + DEFAULT_DB_NAME + " created with owner " + DB_USER);
                        }
                    } else {
                        System.out.println("Database " + DEFAULT_DB_NAME + " already exists");

                        // Change ownership of the database if needed
                        try (Statement grantStmt = postgresConn.createStatement()) {
                            grantStmt.execute("ALTER DATABASE " + DEFAULT_DB_NAME + " OWNER TO " + DB_USER);
                            System.out.println("Changed owner of " + DEFAULT_DB_NAME + " to " + DB_USER);
                        } catch (SQLException grantError) {
                            System.err.println("Warning: Could not change database owner: " + grantError.getMessage());
                        }
                    }

                    postgresConn.close();

                    // Now try to connect to the database with our user
                    try {
                        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                        System.out.println("Successfully connected to database " + DEFAULT_DB_NAME + " as user " + DB_USER);
                    } catch (SQLException connError) {
                        throw new SQLException("Created user and database, but couldn't connect with new credentials: " + connError.getMessage());
                    }
                }
            } catch (SQLException postgresError) {
                throw new SQLException("Impossibile connettersi a PostgreSQL: " + postgresError.getMessage());
            }
        }
    }

    /**
     * Creates a remote instance with specific connection parameters
     * @param jdbcUrl The full JDBC URL for connecting to the database
     * @param username The username for the database connection
     * @param password The password for the database connection
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

                // Grant privileges for remote access
                stmt.execute("GRANT ALL PRIVILEGES ON DATABASE " + DEFAULT_DB_NAME + " TO " + DB_USER);
                stmt.execute("ALTER USER " + DB_USER + " CONNECTION LIMIT -1"); // No connection limit

                System.out.println("Created PostgreSQL user: " + DB_USER);

                // Try to create a read-only user
                try {
                    stmt.execute("CREATE ROLE book_reader WITH LOGIN PASSWORD 'reader2024' NOSUPERUSER INHERIT NOCREATEROLE NOREPLICATION");
                    stmt.execute("GRANT CONNECT ON DATABASE " + DEFAULT_DB_NAME + " TO book_reader");
                    System.out.println("Created read-only PostgreSQL user: book_reader");
                } catch (SQLException e) {
                    // Ignore if we can't create book_reader user
                    System.out.println("Could not create read-only user book_reader: " + e.getMessage());
                }
            } else {
                // User exists, update password
                stmt.execute("ALTER USER " + DB_USER + " WITH PASSWORD '" + DB_PASSWORD + "'");
                stmt.execute("ALTER USER " + DB_USER + " WITH CREATEDB");

                // Grant privileges for remote access
                stmt.execute("GRANT ALL PRIVILEGES ON DATABASE " + DEFAULT_DB_NAME + " TO " + DB_USER);
                stmt.execute("ALTER USER " + DB_USER + " CONNECTION LIMIT -1"); // No connection limit

                System.out.println("Updated existing PostgreSQL user: " + DB_USER);
            }
        }
    }

    /**
     * Loads existing credentials from a file or creates new ones
     */
    private void loadOrCreateCredentials() {
        java.util.Properties props = new java.util.Properties();
        java.io.File credFile = new java.io.File(CREDENTIALS_FILE);

        if (credFile.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(credFile)) {
                props.load(fis);
                DB_USER = props.getProperty("user", "book_admin");
                DB_PASSWORD = props.getProperty("password", "BookRec2024!");
                System.out.println("Loaded existing credentials for user: " + DB_USER);
            } catch (java.io.IOException e) {
                System.err.println("Error loading credentials file: " + e.getMessage());
                generateNewCredentials();
            }
        } else {
            generateNewCredentials();
        }
    }

    /**
     * Generates new random credentials and saves them to file
     */
    private void generateNewCredentials() {
        // Generate a username based on book_admin + random number
        Random rand = new Random();
        DB_USER = "book_admin_" + (1000 + rand.nextInt(9000)); // 4-digit number

        // Generate a random strong password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(rand.nextInt(chars.length())));
        }
        DB_PASSWORD = password.toString();

        // Save the credentials to a file
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", DB_USER);
        props.setProperty("password", DB_PASSWORD);

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(CREDENTIALS_FILE)) {
            props.store(fos, "Database credentials");
            System.out.println("Generated and saved new credentials for user: " + DB_USER);
        } catch (java.io.IOException e) {
            System.err.println("Error saving credentials file: " + e.getMessage());
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
                System.err.println("Error closing database connection: " + e.getMessage());
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