package be.bendem.manga.scraper.implementations;

import be.bendem.manga.scraper.Chapter;
import be.bendem.manga.scraper.Scraper;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scraper implementation for http://www.mangareader.net/
 */
public class MangaReaderScraper implements Scraper {

    private static final String SEARCH_URL = "http://www.mangareader.net/actions/search/?q=";
    private static final String MANGA_URL = "http://www.mangareader.net";

    @Override
    public Map<String, String> search(String query) throws IOException {
        URL url = new URL(SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8.displayName()));

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                .map(line -> line.split("\\|"))
                .collect(Collectors.toMap(
                    parts -> parts[0],
                    parts -> MANGA_URL + parts[4],
                    (a, b) -> a // If there are duplicates, just ignore them
                ));
        }
    }

    @Override
    public String getName(Document document) {
        return document.select("#mangaproperties .aname").text();
    }

    @Override
    public List<Chapter> getChapters(Document document, boolean bonus) {
        String name = getName(document);

        return document.select("#listing > tbody > tr > td:first-child").stream()
            .map(element -> {
                String[] parts = element.text().split(":");
                return new Chapter(
                    parts[0].substring(name.length() + 1),
                    parts.length == 1 ? "" : parts[1].trim(),
                    element.select("a").first().absUrl("href")
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, String> getImageUrlsForChapter(Document document) {
        return document.select("#pageMenu > option").stream()
            .collect(Collectors.toMap(
                option -> Integer.parseInt(option.text()),
                option -> option.absUrl("value")
            ));
    }

    @Override
    public String getImageUrl(Document document) {
        return document.select("#img").attr("src");
    }

}
