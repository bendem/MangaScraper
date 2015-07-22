package be.bendem.manga.scraper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO Write a file with a download summary to allow restarting.
 */
public class MangaScraper {

    private static final Format CHAPTER_FORMAT = new DecimalFormat("0000.#");

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
    private boolean createDirectory(Path dir) throws IOException {
        if(Files.isDirectory(dir)) {
            return false;
        }

        Files.createDirectory(dir);
        return true;
    }

    public void download(String url, Range range, Path output, BiConsumer<Integer, Integer> progressHandler, BiConsumer<Chapter, IOException> errorHandler) {
        String name;

        try {
            createDirectory(output);
            name = getName(url);
        } catch(IOException e) {
            errorHandler.accept(null, e);
            return;
        }

        Path mangaFolder = output.resolve(name);
        try {
            createDirectory(mangaFolder);
        } catch(IOException e) {
            errorHandler.accept(null, e);
            return;
        }

        List<Chapter> chapters = getChapters(url, e -> errorHandler.accept(null, e));
        if(chapters.isEmpty()) {
            return;
        }

        Wrapper<Integer> i = new Wrapper<>(0);
        List<Chapter> toDownload = chapters.stream().sorted()
            .filter(chapter -> chapter.number >= range.min && chapter.number <= range.max)
            .collect(Collectors.toList());

        toDownload
            .forEach(chapter -> {
                downloadChapter(chapter, mangaFolder, errorHandler);
                progressHandler.accept(++i.value, toDownload.size());
            });
    }

    private String getName(String url) throws IOException {
        return impl.getName(new URL(url).openStream(), url);
    }

    public List<Chapter> getChapters(String url, Consumer<IOException> errorHandler) {
        try {
            return impl.getChapters(new URL(url).openStream(), url, false);
        } catch(IOException e) {
            errorHandler.accept(e);
            return Collections.emptyList();
        }
    }

    public void downloadChapter(Chapter chapter, Path folder, BiConsumer<Chapter, IOException> errorHandler) {
        Path downloadDir = folder.resolve(CHAPTER_FORMAT.format(chapter.number));

        Set<String> existing;
        boolean newDirectory;
        try {
            newDirectory = createDirectory(downloadDir);
        } catch(IOException e) {
            errorHandler.accept(chapter, e);
            return;
        }

        if(newDirectory) {
            existing = Collections.emptySet();
        } else {
            Stream<Path> stream;
            try {
                stream = Files.walk(downloadDir, 1);
            } catch(IOException e) {
                errorHandler.accept(chapter, e);
                return;
            }
            existing = stream
                .filter(path -> !path.equals(downloadDir))
                .map(path -> Utils.stripExtenstion(path.getFileName().toString()))
                .collect(Collectors.toSet());
        }

        InputStream inputStream;
        try {
            inputStream = new URL(chapter.url).openStream();
        } catch(IOException e) {
            errorHandler.accept(chapter, e);
            return;
        }

        Map<Integer, String> imageUrlsForChapter;
        try {
            imageUrlsForChapter = impl.getImageUrlsForChapter(inputStream, chapter.url);
        } catch(IOException e) {
            errorHandler.accept(chapter, e);
            return;
        }

        imageUrlsForChapter.entrySet()
            .parallelStream()
            //.stream()
            .filter(entry -> !existing.contains(String.valueOf(entry.getKey())))
            .forEach(entry ->
                downloadImage(chapter, entry.getValue(), entry.getKey(), downloadDir, errorHandler)
            );
    }

    private void downloadImage(Chapter chapter, String url, int number, Path downloadDir, BiConsumer<Chapter, IOException> errorHandler) {
        InputStream inputStream;
        try {
            inputStream = new URL(url).openStream();
        } catch(IOException e) {
            errorHandler.accept(chapter, e);
            return;
        }

        String imgSrc;
        try {
            imgSrc = impl.getImageUrl(inputStream, url);
        } catch(IOException e) {
            errorHandler.accept(chapter, e);
            return;
        }

        URL imgUrl;
        try {
            imgUrl = new URL(imgSrc);
        } catch(MalformedURLException e) {
            errorHandler.accept(chapter, e);
            return;
        }
        String ext = Utils.getExtension(imgSrc);
        Path imgPath = downloadDir.resolve(Paths.get(number + "." + ext));

        try {
            Files.copy(imgUrl.openStream(), imgPath);
        } catch(IOException e) {
            errorHandler.accept(chapter, e);
        }
    }

    public Map<String, String> search(String query, Consumer<IOException> errorHandler) {
        try {
            return impl.search(query);
        } catch(IOException e) {
            errorHandler.accept(e);
            return Collections.emptyMap();
        }
    }

}
