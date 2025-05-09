package book_recommender.lab_b;

/**
 * Classe che rappresenta un libro nell'applicazione Book Recommender.
 */
public class Book {
    private int id;  // ID dal database
    private String title;
    private String authors;
    private String category;
    private String publisher;
    private int publishYear;

    /**
     * Costruttore della classe Book con ID (per record dal database).
     */
    public Book(int id, String title, String authors, String category, String publisher, int publishYear) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.category = category;
        this.publisher = publisher;
        this.publishYear = publishYear;
    }

    /**
     * Costruttore della classe Book senza ID (per nuovi record).
     */
    public Book(String title, String authors, String category, String publisher, int publishYear) {
        this(0, title, authors, category, publisher, publishYear);
    }

    /**
     * Costruttore che accetta un array di valori CSV (mantenuto per compatibilità).
     *
     * @param csvValues Array di valori CSV [titolo, autori, categoria, editore, anno]
     */
    public Book(String[] csvValues) {
        this.id = 0;
        if (csvValues.length >= 5) {
            this.title = csvValues[0].trim();
            this.authors = csvValues[1].trim();
            this.category = csvValues[2].trim();
            this.publisher = csvValues[3].trim();

            try {
                this.publishYear = Integer.parseInt(csvValues[4].trim());
            } catch (NumberFormatException e) {
                this.publishYear = 0;
            }
        }
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public String getCategory() {
        return category;
    }

    public String getPublisher() {
        return publisher;
    }

    public int getPublishYear() {
        return publishYear;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce una rappresentazione testuale del libro.
     */
    @Override
    public String toString() {
        return String.format("%s - %s (%d)", title, authors, publishYear);
    }

    /**
     * Verifica l'uguaglianza basata sul titolo e gli autori.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Book book = (Book) obj;
        return title.equals(book.title) && authors.equals(book.authors);
    }

    /**
     * Genera un hash code basato su titolo e autori.
     */
    @Override
    public int hashCode() {
        return 31 * title.hashCode() + authors.hashCode();
    }
}