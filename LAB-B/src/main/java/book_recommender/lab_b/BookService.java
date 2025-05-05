package book_recommender.lab_b;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookService {
    private static DatabaseManager dbManager;

    static {
        try {
            dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
        }
    }



    public static List<Book> searchBooksByTitle(String title) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, title, authors, category, publisher, publish_year FROM books WHERE LOWER(title) LIKE ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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
            System.err.println("Error searching books by title: " + e.getMessage());
        }

        return books;
    }

    public static List<Book> searchBooksByAuthor(String author) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, title, authors, category, publisher, publish_year FROM books WHERE LOWER(authors) LIKE ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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
            System.err.println("Error searching books by author: " + e.getMessage());
        }

        return books;
    }

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
            System.err.println("Error searching books by author and year: " + e.getMessage());
        }

        return books;
    }

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
            System.err.println("Error getting top rated books: " + e.getMessage());
        }

        return books;
    }


}