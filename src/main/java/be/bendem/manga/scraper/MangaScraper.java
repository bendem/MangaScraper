package be.bendem.manga.scraper;

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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO Better error handling, recover and such
 * TODO Write a file with a download summary to allow restarting.
 */
public class MangaScraper {

    private final Scraper impl;

    public MangaScraper(Scraper impl) {
        this.impl = impl;
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

    public void download(String url, Range range, Path output) {
        createDirectory(output);

        String name = getName(url);
        Path mangaFolder = output.resolve(name);
        createDirectory(mangaFolder);

        List<Chapter> chapters = getChapters(url);

        chapters.stream().sorted()
            .filter(chapter -> chapter.number >= range.min && chapter.number <= range.max)
            .forEach(chapter -> downloadChapter(chapter, mangaFolder));
    }

    public String getName(String url) {
        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return impl.getName(document);
    }

    public List<Chapter> getChapters(String url) {
        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return impl.getChapters(document, false);
    }

    public void downloadChapter(Chapter chapter, Path folder) {
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

        impl.getImageUrlsForChapter(document).entrySet()
            .parallelStream()
            //.stream()
            .filter(entry -> !existing.contains(String.valueOf(entry.getKey())))
            .forEach(entry ->
                    downloadImage(entry.getValue(), entry.getKey(), downloadDir)
            );
    }

    private void downloadImage(String url, int number, Path downloadDir) {
        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch(IOException e) {
            // TODO Retry?
            throw new RuntimeException(e);
        }

        String imgSrc = impl.getImageUrl(document);
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

    public Map<String, String> search(String query) {
        return impl.search(query);
    }

}
