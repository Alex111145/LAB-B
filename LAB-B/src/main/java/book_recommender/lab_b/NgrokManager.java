package book_recommender.lab_b;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Classe che gestisce l'installazione e configurazione di ngrok
 * per rendere accessibile il database PostgreSQL da qualsiasi rete
 * senza necessità di configurare il port forwarding sul router.
 *
 * <p>NgrokManager fornisce funzionalità per scaricare, installare e configurare
 * automaticamente ngrok, creando tunnel TCP sicuri per esporre servizi locali
 * come PostgreSQL su Internet. Questo permette l'accesso remoto al database
 * senza la necessità di configurare manualmente il port forwarding sul router
 * o avere un indirizzo IP pubblico.</p>
 *
 * <p>La classe gestisce l'intero ciclo di vita del tunnel, dall'installazione
 * di ngrok fino alla creazione, gestione e chiusura del tunnel.</p>
 *
 * @author book_recommender.lab_b
 * @version 1.0
 */
public class NgrokManager {

    /**
     * Nome della cartella dove verrà installato ngrok.
     * Questa cartella conterrà l'eseguibile di ngrok e i file correlati.
     */
    private static final String NGROK_FOLDER = "ngrok";

    /**
     * Processo che mantiene in esecuzione ngrok.
     * Viene utilizzato per monitorare e terminare il tunnel quando necessario.
     */
    private Process ngrokProcess;

    /**
     * URL pubblico fornito da ngrok per accedere al servizio.
     * Tipicamente nel formato "X.tcp.ngrok.io".
     */
    private String publicUrl;

    /**
     * Porta pubblica assegnata da ngrok per accedere al servizio.
     * Questa porta viene mappata alla porta locale di PostgreSQL.
     */
    private int publicPort;

    /**
     * Stato del tunnel ngrok.
     * True se il tunnel è attivo e funzionante, false altrimenti.
     */
    private boolean tunnelActive = false;

    /**
     * Inizializza e avvia ngrok per il tunneling della porta PostgreSQL.
     * Questo metodo configura tutto ciò che è necessario per esporre
     * PostgreSQL attraverso ngrok, inclusa l'installazione di ngrok
     * se non è già presente nel sistema.
     *
     * @param postgresPort la porta locale su cui è in ascolto PostgreSQL
     * @return true se il tunnel è stato avviato con successo, false altrimenti
     */
    public boolean startNgrokTunnel(int postgresPort) {
        try {
            // Crea la cartella ngrok se non esiste
            createNgrokFolder();

            // Verifica se ngrok è installato, altrimenti lo installa
            if (!isNgrokInstalled()) {
                downloadAndInstallNgrok();
            }

            // Avvia ngrok per creare un tunnel verso la porta PostgreSQL
            startTunnel(postgresPort);

            // Ottiene l'URL pubblico generato da ngrok
            fetchPublicUrl();

            return tunnelActive;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Crea la cartella per contenere ngrok e imposta i permessi appropriati.
     * Se la cartella esiste già, non viene modificata. Su sistemi Unix-like,
     * vengono impostati i permessi corretti (755) per consentire l'esecuzione.
     */
    private void createNgrokFolder() {
        File folder = new File(NGROK_FOLDER);
        if (!folder.exists()) {
            boolean created = folder.mkdir();
            if (created) {
                // Imposta i permessi appropriati sulla cartella
                try {
                    if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                        // Su sistemi Unix-like, imposta i permessi a 755 (rwxr-xr-x)
                        Runtime.getRuntime().exec("chmod 755 " + NGROK_FOLDER);
                    }
                } catch (IOException e) {
                    // Ignora eventuali errori con i permessi
                }
            }
        }
    }

    /**
     * Verifica se ngrok è già installato nel sistema.
     * Controlla l'esistenza dell'eseguibile di ngrok nella
     * cartella designata.
     *
     * @return true se ngrok è già presente nel sistema, false altrimenti
     */
    private boolean isNgrokInstalled() {
        String ngrokExec = getNgrokExecutablePath();
        File ngrokFile = new File(ngrokExec);
        return ngrokFile.exists();
    }

    /**
     * Restituisce il percorso dell'eseguibile ngrok in base al sistema operativo.
     * Su Windows aggiunge l'estensione ".exe" al nome del file.
     *
     * @return il percorso completo dell'eseguibile ngrok, adattato al sistema operativo
     */
    private String getNgrokExecutablePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String ngrokExec = NGROK_FOLDER + File.separator + "ngrok";

        if (os.contains("win")) {
            ngrokExec += ".exe";
        }

        return ngrokExec;
    }

    /**
     * Scarica e installa ngrok nel sistema.
     * Il processo include il download del pacchetto appropriato per il
     * sistema operativo corrente, l'estrazione e l'impostazione dei
     * permessi di esecuzione.
     *
     * @throws IOException in caso di errori durante il download o l'installazione
     */
    private void downloadAndInstallNgrok() throws IOException {
        // Determina l'URL di download in base al sistema operativo e all'architettura
        String downloadUrl = getNgrokDownloadUrl();

        // Scarica il file zip di ngrok
        String zipFilePath = NGROK_FOLDER + File.separator + "ngrok.zip";
        downloadFile(downloadUrl, zipFilePath);

        // Estrai il file zip
        extractZipFile(zipFilePath);

        // Imposta i permessi di esecuzione su sistemi Unix-like
        setExecutablePermissions();
    }

    /**
     * Determina l'URL di download di ngrok in base al sistema operativo e all'architettura.
     * Seleziona il pacchetto appropriato per Windows, macOS o Linux, e per
     * architetture a 32 o 64 bit (x86 o ARM).
     *
     * @return l'URL completo per il download della versione appropriata di ngrok
     */
    private String getNgrokDownloadUrl() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String url = "https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-";

        if (os.contains("win")) {
            url += "windows";
        } else if (os.contains("mac")) {
            url += "darwin";
        } else {
            url += "linux";
        }

        if (arch.contains("64")) {
            if (arch.contains("arm") || arch.contains("aarch")) {
                url += "-arm64";
            } else {
                url += "-amd64";
            }
        } else {
            url += "-386";
        }

        url += ".zip";
        return url;
    }

    /**
     * Scarica un file da un URL specifico.
     * Utilizza i canali NIO per un download efficiente.
     *
     * @param url l'URL del file da scaricare
     * @param outputPath il percorso locale dove salvare il file
     * @throws IOException in caso di errori durante il download o la scrittura del file
     */
    private void downloadFile(String url, String outputPath) throws IOException {
        URL website = new URL(url);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(outputPath);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }

    /**
     * Estrae un file zip nella cartella di ngrok.
     * Decomprime tutti i file contenuti nell'archivio ZIP nella
     * cartella di destinazione.
     *
     * @param zipFilePath il percorso completo del file zip da estrarre
     * @throws IOException in caso di errori durante la lettura del file zip o la scrittura dei file estratti
     */
    private void extractZipFile(String zipFilePath) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry zipEntry = zis.getNextEntry();

        while (zipEntry != null) {
            File newFile = new File(NgrokManager.NGROK_FOLDER, zipEntry.getName());
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }

    /**
     * Imposta i permessi di esecuzione sull'eseguibile di ngrok nei sistemi Unix-like.
     * Utilizza le API POSIX per impostare i permessi corretti, con un fallback
     * al comando chmod se le API non sono disponibili.
     */
    private void setExecutablePermissions() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            try {
                Path ngrokPath = Paths.get(getNgrokExecutablePath());
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                perms.add(PosixFilePermission.OWNER_WRITE);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_READ);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_READ);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);

                Files.setPosixFilePermissions(ngrokPath, perms);
            } catch (Exception e) {
                // Fallback se setPosixFilePermissions non è supportato
                try {
                    Runtime.getRuntime().exec("chmod +x " + getNgrokExecutablePath());
                } catch (IOException ioEx) {
                    // Ignora eventuali errori con i permessi
                }
            }
        }
    }

    /**
     * Avvia ngrok per creare un tunnel verso la porta PostgreSQL.
     * Configura l'authtoken di ngrok e poi avvia il processo di tunneling
     * per la porta specificata.
     *
     * @param postgresPort la porta locale su cui è in ascolto PostgreSQL
     * @throws IOException in caso di errori durante l'avvio del processo o la configurazione dell'authtoken
     */
    private void startTunnel(int postgresPort) throws IOException {
        String ngrokExec = getNgrokExecutablePath();

        // Prima configura l'authtoken
        String authToken = "2wlATlX5JTqCBCzpKZQJ23Iwp5U_6mvLivu3fdFtTaSPqGyrZ";
        ProcessBuilder authPb = new ProcessBuilder(ngrokExec, "config", "add-authtoken", authToken);
        Process authProcess = authPb.start();
        try {
            int exitCode = authProcess.waitFor();
            if (exitCode != 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(authProcess.getErrorStream()));
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
                throw new IOException("Impossibile configurare l'authtoken di ngrok: " + error);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante la configurazione dell'authtoken di ngrok", e);
        }

        // Ora avvia il tunnel
        ProcessBuilder pb = new ProcessBuilder(ngrokExec, "tcp", String.valueOf(postgresPort));
        pb.redirectErrorStream(true);
        ngrokProcess = pb.start();

        // Attendi un po' per essere sicuri che ngrok si avvii
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verifica che il processo sia in esecuzione
        if (!ngrokProcess.isAlive()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(ngrokProcess.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            throw new IOException("Ngrok non è stato avviato correttamente: " + output);
        }

        tunnelActive = true;
    }

    /**
     * Ottiene l'URL pubblico e la porta generati da ngrok.
     * Interroga l'API locale di ngrok per ottenere le informazioni sul tunnel attivo
     * e le analizza utilizzando espressioni regolari per estrarre l'host e la porta.
     *
     * @throws IOException in caso di errori durante la comunicazione con l'API di ngrok o l'analisi della risposta
     */
    private void fetchPublicUrl() throws IOException {
        // Attendi un momento in più per assicurarsi che l'API di ngrok sia pronta
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Utilizza l'API locale di ngrok per ottenere le informazioni sul tunnel
        URL url = new URL("http://127.0.0.1:4040/api/tunnels");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream())) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        } catch (IOException e) {
            throw e;
        }

        String jsonResponse = response.toString();

        // Prova diverse espressioni regolari per trovare l'URL pubblico
        // Pattern per il formato: "public_url":"tcp://0.tcp.ngrok.io:12345"
        Pattern pattern1 = Pattern.compile("\"public_url\":\"tcp://([^:]+):(\\d+)\"");
        Matcher matcher1 = pattern1.matcher(jsonResponse);

        if (matcher1.find()) {
            publicUrl = matcher1.group(1);
            publicPort = Integer.parseInt(matcher1.group(2));
            return;
        }

        // Pattern alternativo per il formato: "public_url":"tcp://0.tcp.eu.ngrok.io:12345"
        Pattern pattern2 = Pattern.compile("\"public_url\":\"tcp://([\\w.-]+.ngrok.io):(\\d+)\"");
        Matcher matcher2 = pattern2.matcher(jsonResponse);

        if (matcher2.find()) {
            publicUrl = matcher2.group(1);
            publicPort = Integer.parseInt(matcher2.group(2));
            return;
        }

        // Tentativo generico di trovare qualsiasi URL ngrok
        Pattern pattern3 = Pattern.compile("\"public_url\":\"tcp://([^\"]+):(\\d+)\"");
        Matcher matcher3 = pattern3.matcher(jsonResponse);

        if (matcher3.find()) {
            publicUrl = matcher3.group(1);
            publicPort = Integer.parseInt(matcher3.group(2));
            return;
        }

        throw new IOException("Impossibile trovare l'URL pubblico nella risposta di ngrok: " + jsonResponse);
    }

    /**
     * Arresta il tunnel ngrok.
     * Termina il processo ngrok in modo pulito, aspettando fino a 5 secondi
     * prima di terminarlo forzatamente se necessario.
     */
    public void stopTunnel() {
        if (ngrokProcess != null && ngrokProcess.isAlive()) {
            ngrokProcess.destroy();
            try {
                if (!ngrokProcess.waitFor(5, TimeUnit.SECONDS)) {
                    ngrokProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                ngrokProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            tunnelActive = false;
        }
    }

    /**
     * Ottiene l'host pubblico per la connessione remota (senza il protocollo 'tcp://').
     * Questo è l'indirizzo host a cui ci si può connettere dall'esterno.
     *
     * @return l'host pubblico o null se il tunnel non è attivo
     */
    public String getPublicUrl() {
        if (tunnelActive) {
            return publicUrl;
        }
        return null;
    }

    /**
     * Ottiene la porta pubblica per la connessione remota.
     * Questa è la porta a cui ci si può connettere dall'esterno.
     *
     * @return la porta pubblica o -1 se il tunnel non è attivo
     */
    public int getPublicPort() {
        if (tunnelActive) {
            return publicPort;
        }
        return -1;
    }

    /**
     * Ottiene la stringa di connessione JDBC completa per la connessione remota.
     * Questa stringa può essere utilizzata direttamente per configurare
     * una connessione JDBC al database PostgreSQL attraverso il tunnel ngrok.
     *
     * @return la stringa di connessione JDBC completa o null se il tunnel non è attivo
     */
    public String getJdbcConnectionString() {
        if (tunnelActive) {
            return "jdbc:postgresql://" + publicUrl + ":" + publicPort + "/book_recommender";
        }
        return null;
    }
}