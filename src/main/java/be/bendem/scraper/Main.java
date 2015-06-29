package be.bendem.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO Better error handling, recover and such
 * TODO Write a file with a download summary to allow restarting.
 */
public class Main {

    private final Scraper scraper;

    public Main(Scraper scraper) {
        this.scraper = scraper;
    }

    /**
     * Creates a directory if it doesn't already exist.
     *
     * @param dir the directory to create
     * @return whether a new directory was actually created or not
     */
    private boolean createDirectory(Path dir) {
        if(Files.isDirectory(dir)) {
            return false;
        }

        try {
            Files.createDirectory(dir);
            return true;
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start(String url, Range range, Path output) {
        createDirectory(output);

        System.out.println("Getting main page");
        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        String name = scraper.getName(document);
        Path mangaFolder = output.resolve(name);
        createDirectory(mangaFolder);

        System.out.println("Getting chapters for '" + name + "'");
        List<Chapter> chapters = scraper.getChapters(document, false);

        System.out.println("Got " + chapters.size() + " chapters, let's crawl that");
        chapters.stream().sorted()
            .filter(chapter -> chapter.number >= range.min && chapter.number <= range.max)
            .forEach(chapter -> crawl(chapter, mangaFolder));

        System.out.println("Done crawling");
    }

    private void crawl(Chapter chapter, Path folder) {
        System.out.println("Downloading chapter '" + chapter.name + "' (" + chapter.url + ")");

        Path downloadDir = folder.resolve(chapter.name);
        Set<String> existing;
        if(createDirectory(downloadDir)) {
            existing = Collections.emptySet();
        } else {
            Stream<Path> stream;
            try {
                stream = Files.walk(downloadDir, 1);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
            existing = stream
                .filter(path -> !path.equals(downloadDir))
                .map(path -> Utils.stripExtenstion(path.getFileName().toString()))
                .collect(Collectors.toSet());
        }

        Document document;
        try {
            document = Jsoup.connect(chapter.url).get();
        } catch(IOException e) {
            // TODO Retry?
            throw new RuntimeException(e);
        }

        scraper.getImageUrlsForChapter(document).entrySet()
            .parallelStream()
            //.stream()
            .filter(entry -> !existing.contains(String.valueOf(entry.getKey())))
            .forEach(entry ->
                    downloadImage(entry.getValue(), entry.getKey(), downloadDir)
            );
    }

    private void downloadImage(String url, int number, Path downloadDir) {
        System.out.println("Downloading " + url);

        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch(IOException e) {
            // TODO Retry?
            throw new RuntimeException(e);
        }

        String imgSrc = scraper.getImageUrl(document);
        URL imgUrl;
        try {
            imgUrl = new URL(imgSrc);
        } catch(MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        String ext = Utils.getExtension(imgSrc);
        Path imgPath = downloadDir.resolve(Paths.get(number + "." + ext));

        try {
            Files.copy(imgUrl.openStream(), imgPath);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void search(String query) {
        System.out.println("| Search results for '" + query + "'");
        System.out.println('|');
        scraper.search(query)
            .entrySet().stream()
            .map(e -> "| " + e.getKey() + ": " + e.getValue())
            .forEach(System.out::println);
    }

    public static void main(String[] args) {
        String url = null;
        String implementation = "MangaReaderScraper";
        String output = "download";
        String search = null;
        Range range = new Range();

        for(int i = 0; i < args.length; ++i) {
            switch(args[i]) {
                case "-r":
                    range = Range.parse(args[++i]);
                    break;
                case "-i":
                    implementation = args[++i];
                    break;
                case "-o":
                    output = args[++i];
                    break;
                case "-s":
                    search = args[++i];
                    break;
                case "-h":
                case "-help":
                case "--help":
                    printHelp();
                    return;
                default:
                    url = args[i];
            }
        }

        if(url == null && search == null) {
            printHelp();
            return;
        }

        if(!implementation.contains(".")) {
            implementation = "be.bendem.scraper.implementations." + implementation;
        }

        Scraper scraper;
        try {
            scraper = Scraper.class.cast(Class.forName(implementation).newInstance());
        } catch(InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Main main = new Main(scraper);
        if(search != null) {
            main.search(search);
            return;
        }

        main.start(url, range, Paths.get(output));
    }

    private static void printHelp() {
        System.err.println();
        System.err.println("Usage: java -jar jarfile.jar [-i <implementation>] [-r <range>] [-o <output>] <-s <query>|<url>>");
        System.err.println("    <range>          Is either a number (like 1) or two numbers separated with a");
        System.err.println("                     dash (like 1-5). Default value is 0-INFINITY");
        System.err.println();
        System.err.println("    <implementation> Specify the FQN (or internal name) of the class implementing");
        System.err.println("                     Scraper to use");
        System.err.println();
        System.err.println("    <output>         Specify the folder to put the downloads in. Default value is download");
        System.err.println();
        System.err.println("    <query>          A search query");
        System.err.println();
        System.err.println("    <url>            is a valid url for the chosen implementation");
        System.err.println();
    }

}
