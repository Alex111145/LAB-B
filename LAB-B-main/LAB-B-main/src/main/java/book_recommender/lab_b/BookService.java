package book_recommender.lab_b;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe di servizio per le operazioni di ricerca e recupero dei libri dal database.
 * Fornisce metodi statici per effettuare diverse tipologie di ricerca sui libri.
 * Utilizza il DatabaseManager per gestire le connessioni al database.
 */
public class BookService {
    private static DatabaseManager dbManager;

    /**
     * Blocco statico di inizializzazione.
     * Ottiene l'istanza singleton del DatabaseManager all'avvio della classe.
     */
    static {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            // Gestione silenziosa dell'eccezione - l'applicazione proverà a riconnettersi
            // quando necessario nei metodi di ricerca
        }
    }

    /**
     * Cerca libri nel database basandosi sul titolo.
     * Esegue una ricerca case-insensitive e parziale (contiene).
     *
     * @param title Titolo o parte del titolo da cercare
     * @return Lista di oggetti Book che corrispondono al criterio di ricerca
     */
    public static List<Book> searchBooksByTitle(String title) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, title, authors, category, publisher, publish_year FROM books WHERE LOWER(title) LIKE ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Aggiunge i caratteri jolly % per cercare la stringa in qualsiasi posizione
            pstmt.setString(1, "%" + title.toLowerCase() + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Book book = new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("authors"),
                        rs.getString("category"),
                        rs.getString("publisher"),
                        rs.getInt("publish_year")
                );
                books.add(book);
            }
        } catch (SQLException e) {
            // Gestione silenziosa degli errori di database
            // In un contesto di produzione, sarebbe meglio loggare questi errori
        }

        return books;
    }

    /**
     * Cerca libri nel database basandosi sul nome dell'autore.
     * Esegue una ricerca case-insensitive e parziale (contiene).
     *
     * @param author Nome o parte del nome dell'autore da cercare
     * @return Lista di oggetti Book che corrispondono al criterio di ricerca
     */
    public static List<Book> searchBooksByAuthor(String author) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, title, authors, category, publisher, publish_year FROM books WHERE LOWER(authors) LIKE ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Aggiunge i caratteri jolly % per cercare la stringa in qualsiasi posizione
            pstmt.setString(1, "%" + author.toLowerCase() + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Book book = new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("authors"),
                        rs.getString("category"),
                        rs.getString("publisher"),
                        rs.getInt("publish_year")
                );
                books.add(book);
            }
        } catch (SQLException e) {
            // Gestione silenziosa degli errori di database
        }

        return books;
    }

    /**
     * Cerca libri nel database basandosi sia sul nome dell'autore che sull'anno di pubblicazione.
     * Esegue una ricerca case-insensitive e parziale per l'autore, ma esatta per l'anno.
     *
     * @param author Nome o parte del nome dell'autore da cercare
     * @param year Anno esatto di pubblicazione
     * @return Lista di oggetti Book che corrispondono ai criteri di ricerca
     */
    public static List<Book> searchBooksByAuthorAndYear(String author, int year) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, title, authors, category, publisher, publish_year FROM books WHERE LOWER(authors) LIKE ? AND publish_year = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + author.toLowerCase() + "%");
            pstmt.setInt(2, year);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Book book = new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("authors"),
                        rs.getString("category"),
                        rs.getString("publisher"),
                        rs.getInt("publish_year")
                );
                books.add(book);
            }
        } catch (SQLException e) {
            // Gestione silenziosa degli errori di database
        }

        return books;
    }

    /**
     * Recupera i libri con le valutazioni più alte dal database.
     * Calcola la media di tutte le categorie di valutazione (stile, contenuto, gradevolezza,
     * originalità ed edizione) per determinare il rating complessivo di ogni libro.
     *
     * @param limit Numero massimo di libri da recuperare
     * @return Lista di oggetti Book ordinati per valutazione media decrescente
     */
    public static List<Book> getTopRatedBooks(int limit) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT b.id, b.title, b.authors, b.category, b.publisher, b.publish_year, " +
                "AVG((br.style_rating + br.content_rating + br.pleasantness_rating + " +
                "br.originality_rating + br.edition_rating) / 5.0) as avg_rating " +
                "FROM books b " +
                "JOIN book_ratings br ON b.id = br.book_id " +
                "GROUP BY b.id, b.title, b.authors, b.category, b.publisher, b.publish_year " +
                "HAVING AVG((br.style_rating + br.content_rating + br.pleasantness_rating + " +
                "br.originality_rating + br.edition_rating) / 5.0) > 0 " +
                "ORDER BY avg_rating DESC " +
                "LIMIT ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Book book = new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("authors"),
                        rs.getString("category"),
                        rs.getString("publisher"),
                        rs.getInt("publish_year")
                );
                books.add(book);
            }
        } catch (SQLException e) {
            // Gestione silenziosa degli errori di database
        }

        return books;
    }
}