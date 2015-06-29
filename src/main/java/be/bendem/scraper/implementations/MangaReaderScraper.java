package be.bendem.scraper.implementations;

import be.bendem.scraper.Chapter;
import be.bendem.scraper.Scraper;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scraper implementation for http://www.mangareader.net/
 */
public class MangaReaderScraper implements Scraper {

    private static final String SEARCH_URL = "http://www.mangareader.net/actions/search/?q=";
    private static final String MANGA_URL = "http://www.mangareader.net/";

    @Override
    public Map<String, String> search(String query) {
        URL url;
        try {
            url = new URL(SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8.displayName()));
        } catch(MalformedURLException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        return reader.lines()
            .map(line -> line.split("\\|"))
            .collect(Collectors.toMap(
                parts -> parts[0],
                parts -> MANGA_URL + parts[4]
            ));
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
