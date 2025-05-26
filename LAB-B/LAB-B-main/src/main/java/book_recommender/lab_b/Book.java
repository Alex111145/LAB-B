package book_recommender.lab_b;

/**
 * Classe che rappresenta un'entità libro all'interno dell'applicazione.
 * Contiene tutti i metadati relativi a un libro e supporta diverse modalità di costruzione.
 */
public class Book {
    private int id;  // ID univoco nel database
    private String title;
    private String authors;
    private String category;
    private String publisher;
    private int publishYear;

    /**
     * Costruttore completo che inizializza un libro con tutte le proprietà incluso l'ID.
     * Questo costruttore è usato principalmente quando si recuperano libri esistenti dal database.
     *
     * @param id ID del libro nel database
     * @param title Titolo del libro
     * @param authors Autori del libro
     * @param category Categoria del libro
     * @param publisher Editore del libro
     * @param publishYear Anno di pubblicazione
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
     * Costruttore per nuovi libri che non hanno ancora un ID database.
     * Utilizza il costruttore completo impostando l'ID a 0, che verrà aggiornato
     * dopo l'inserimento nel database.
     *
     * @param title Titolo del libro
     * @param authors Autori del libro
     * @param category Categoria del libro
     * @param publisher Editore del libro
     * @param publishYear Anno di pubblicazione
     */
    public Book(String title, String authors, String category, String publisher, int publishYear) {
        this(0, title, authors, category, publisher, publishYear);
    }

    /**
     * Costruttore specializzato per creare un libro da un array di valori CSV.
     * Supporta l'importazione di dati da file CSV o altre fonti esterne.
     * L'array deve contenere almeno 5 elementi: [titolo, autori, categoria, editore, anno].
     *
     * @param csvValues Array di stringhe contenenti i valori delle proprietà del libro
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
                // Se l'anno non è un numero valido, imposta a 0
                this.publishYear = 0;
            }
        }
    }

    /**
     * Restituisce l'ID del libro nel database.
     *
     * @return ID univoco del libro, o 0 se non ancora salvato
     */
    public int getId() {
        return id;
    }

    /**
     * Restituisce il titolo del libro.
     *
     * @return Titolo del libro
     */
    public String getTitle() {
        return title;
    }

    /**
     * Restituisce gli autori del libro.
     *
     * @return Autori del libro
     */
    public String getAuthors() {
        return authors;
    }

    /**
     * Restituisce la categoria del libro.
     *
     * @return Categoria del libro
     */
    public String getCategory() {
        return category;
    }

    /**
     * Restituisce l'editore del libro.
     *
     * @return Editore del libro
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * Restituisce l'anno di pubblicazione del libro.
     *
     * @return Anno di pubblicazione o 0 se non disponibile
     */
    public int getPublishYear() {
        return publishYear;
    }

    /**
     * Imposta l'ID del libro dopo il salvataggio nel database.
     *
     * @param id Nuovo ID assegnato dal database
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce una rappresentazione testuale del libro nel formato "Titolo - Autori (Anno)".
     * Utile per la visualizzazione in liste e log.
     *
     * @return Stringa formattata con le informazioni principali del libro
     */
    @Override
    public String toString() {
        return String.format("%s - %s (%d)", title, authors, publishYear);
    }

    /**
     * Verifica se due libri sono uguali confrontando titolo e autori.
     * Due libri sono considerati uguali se hanno lo stesso titolo e gli stessi autori,
     * indipendentemente dagli altri attributi.
     *
     * @param obj Oggetto da confrontare con questo libro
     * @return true se i libri sono uguali, false altrimenti
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Book book = (Book) obj;
        return title.equals(book.title) && authors.equals(book.authors);
    }

    /**
     * Genera un codice hash basato su titolo e autori.
     * Coerente con il metodo equals() per garantire il corretto funzionamento
     * nelle collezioni come HashMap e HashSet.
     *
     * @return Valore hash calcolato
     */
    @Override
    public int hashCode() {
        return 31 * title.hashCode() + authors.hashCode();
    }
}