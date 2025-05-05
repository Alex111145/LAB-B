package book_recommender.lab_b;

/**
 * Classe ausiliaria per memorizzare i consigli personalizzati
 */
public class RecommendedBook {
    public String userId;
    public int sourceBookId;
    public int recommendedBookId;
    public String bookTitle;
    public String authorName;
    public String category;

    /**
     * Costruttore base
     */
    public RecommendedBook(String userId, int sourceBookId, int recommendedBookId) {
        this.userId = userId;
        this.sourceBookId = sourceBookId;
        this.recommendedBookId = recommendedBookId;
        this.bookTitle = "";
        this.authorName = "";
        this.category = "";
    }

    /**
     * Costruttore con titolo
     */
    public RecommendedBook(String userId, String bookTitle) {
        this.userId = userId;
        this.bookTitle = bookTitle;
        this.sourceBookId = 0;
        this.recommendedBookId = 0;
        this.authorName = "";
        this.category = "";
    }

    /**
     * Costruttore completo
     */
    public RecommendedBook(String userId, int sourceBookId, int recommendedBookId,
                           String bookTitle, String authorName, String category) {
        this.userId = userId;
        this.sourceBookId = sourceBookId;
        this.recommendedBookId = recommendedBookId;
        this.bookTitle = bookTitle;
        this.authorName = authorName;
        this.category = category;
    }
}