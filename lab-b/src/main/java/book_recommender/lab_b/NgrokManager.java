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
 */
public class NgrokManager {

    private static final String NGROK_FOLDER = "ngrok";
    private Process ngrokProcess;
    private String publicUrl;
    private int publicPort;
    private boolean tunnelActive = false;

    /**
     * Inizializza e avvia ngrok per il tunneling della porta PostgreSQL
     * @param postgresPort la porta locale su cui è in ascolto PostgreSQL
     * @return true se il tunnel è stato avviato con successo
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
            System.err.println("Errore nell'avvio di ngrok: " + e.getMessage());

            return false;
        }
    }

    /**
     * Crea la cartella per contenere ngrok
     */
    /**
     * Crea la cartella per contenere ngrok e imposta i permessi appropriati
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
               }}}
    }

    /**
     * Verifica se ngrok è già installato
     * @return true se ngrok è già presente
     */
    private boolean isNgrokInstalled() {
        String ngrokExec = getNgrokExecutablePath();
        File ngrokFile = new File(ngrokExec);
        return ngrokFile.exists();
    }

    /**
     * Restituisce il percorso dell'eseguibile ngrok in base al sistema operativo
     * @return il percorso dell'eseguibile ngrok
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
     * Scarica e installa ngrok
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
     * Determina l'URL di download di ngrok in base al sistema operativo e all'architettura
     * @return l'URL di download
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
     * Scarica un file da un URL
     * @param url l'URL del file da scaricare
     * @param outputPath il percorso dove salvare il file
     * @throws IOException in caso di errori durante il download
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
     * Estrae un file zip
     *
     * @param zipFilePath il percorso del file zip
     * @throws IOException in caso di errori durante l'estrazione
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
     * Imposta i permessi di esecuzione su sistemi Unix-like
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
                    System.err.println("Impossibile impostare i permessi di esecuzione: " + ioEx.getMessage());
                }
            }
        }
    }

    /**
     * Avvia ngrok per creare un tunnel verso la porta PostgreSQL
     * @param postgresPort la porta locale su cui è in ascolto PostgreSQL
     * @throws IOException in caso di errori durante l'avvio
     */
    private void startTunnel(int postgresPort) throws IOException {
        String ngrokExec = getNgrokExecutablePath();

        // Prima configura l'authtoken
        String authToken = "2wlATlX5JTqCBCzpKZQJ23Iwp5U_6mvLivu3fdFtTaSPqGyrZ"; // Sostituisci con il tuo token effettivo
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
     * Ottiene l'URL pubblico generato da ngrok
     * @throws IOException in caso di errori durante il recupero dell'URL
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
            System.err.println("Errore nella lettura della risposta API di ngrok: " + e.getMessage());
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
     * Arresta il tunnel ngrok
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
     * Ottiene l'URL pubblico per la connessione remota
     * @return l'URL pubblico o null se il tunnel non è attivo
     */
    public String getPublicUrl() {
        if (tunnelActive) {
            return publicUrl;
        }
        return null;
    }

    /**
     * Ottiene la porta pubblica per la connessione remota
     * @return la porta pubblica o -1 se il tunnel non è attivo
     */
    public int getPublicPort() {
        if (tunnelActive) {
            return publicPort;
        }
        return -1;
    }

    /**
     * Ottiene la stringa di connessione JDBC per la connessione remota
     * @return la stringa di connessione JDBC o null se il tunnel non è attivo
     */
    public String getJdbcConnectionString() {
        if (tunnelActive) {
            return "jdbc:postgresql://" + publicUrl + ":" + publicPort + "/book_recommender";
        }
        return null;
    }
}