package be.bendem.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

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
 * TODO Extract Crawl interface and write one for multiple manga reader sites
 * TODO Better error handling, recover and such
 * TODO Write a file with a download summary to allow restarting.
 * TODO Command line option to ignore half chapters
 */
public class MangaEdenScraper {

    private static final Path DOWNLOAD = Paths.get("download");

    public MangaEdenScraper() {
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
        System.out.println("Getting chapters");
        List<Chapter> chapters = getChapters(url);

        System.out.println("Got " + chapters.size() + ", crawling");
        ListIterator<Chapter> it = chapters.listIterator(chapters.size());
        while(it.hasPrevious()) {
            crawl(it.previous());
        }

        System.out.println("Done crawling");
    }

    private void crawl(Chapter chapter) {
        System.out.println("Downloading chapter '" + chapter.name + "' (" + chapter.url + ")");

        Path downloadDir = DOWNLOAD.resolve(chapter.name);
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
        Elements pages = document.select("#top-in > div.top-title > select:nth-child(5) > option");
        pages
            .parallelStream()
            //.stream()
            .filter(option -> !existing.contains(option.text()))
            .forEach(option ->
                downloadImage(option.absUrl("value"), option.text(), downloadDir)
            );
    }

    private void downloadImage(String url, String number, Path downloadDir) {
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
        Path imgPath = downloadDir.resolve(Paths.get(number + '.' + ext));

        try {
            Files.copy(imgUrl.openStream(), imgPath);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private List<Chapter> getChapters(String url) {
        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        return document
            .select("#leftContent > table > tbody > tr > td:first-child > a").stream()
            .filter(elem -> !elem.select("b").text().contains(".")) // Ignore weird half-chapters TODO Make option
            .map(elem -> new Chapter(elem.absUrl("href"), elem.select("b").text()))
            .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        if(args.length < 1) {
            System.err.println("No url given");
            return;
        }
        new MangaEdenScraper().start(args[0]);
    }

}
