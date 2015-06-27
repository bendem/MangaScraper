package be.bendem.scraper;

import be.bendem.scraper.mangaeden.MangaEdenScraper;
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
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO Better error handling, recover and such
 * TODO Write a file with a download summary to allow restarting.
 * TODO Command line option to ignore half chapters
 */
public class Main {

    private static final Path DOWNLOAD = Paths.get("download");

    private final Scraper scraper;

    public Main(Scraper scraper) {
        this.scraper = scraper;

        if(Files.isDirectory(DOWNLOAD)) {
            return;
        }

        try {
            Files.createDirectory(DOWNLOAD);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void start(String url) {
        System.out.println("Getting main page");
        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        String name = scraper.getName(document);
        Path mangaFolder = DOWNLOAD.resolve(name);
        if(!Files.isDirectory(mangaFolder)) {
            try {
                Files.createDirectory(mangaFolder);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("Getting chapters for '" + name + "'");
        List<Chapter> chapters = scraper.getChapters(document, false);

        System.out.println("Got " + chapters.size() + " chapters, let's crawl that");
        ListIterator<Chapter> it = chapters.listIterator(chapters.size());
        while(it.hasPrevious()) {
            crawl(it.previous(), mangaFolder);
        }

        System.out.println("Done crawling");
    }

    private void crawl(Chapter chapter, Path folder) {
        System.out.println("Downloading chapter '" + chapter.name + "' (" + chapter.url + ")");

        Path downloadDir = folder.resolve(chapter.name);
        Set<String> existing;
        if(!Files.isDirectory(downloadDir)) {
            try {
                Files.createDirectory(downloadDir);
            } catch(IOException e) {
                e.printStackTrace();
                return;
            }
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

        scraper.getImageUrlsFor(document).entrySet()
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

        String imgSrc = document.select("#mainImg").first().absUrl("src");
        URL imgUrl;
        try {
            imgUrl = new URL(imgSrc);
        } catch(MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        String ext = Utils.last(imgSrc.split("\\."));
        Path imgPath = downloadDir.resolve(Paths.get(number + "." + ext));

        try {
            Files.copy(imgUrl.openStream(), imgPath);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if(args.length < 1) {
            System.err.println("No url given");
            return;
        }

        new Main(new MangaEdenScraper()).start(args[0]);
    }

}
