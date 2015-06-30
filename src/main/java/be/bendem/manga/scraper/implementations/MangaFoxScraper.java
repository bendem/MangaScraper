package be.bendem.manga.scraper.implementations;

import be.bendem.manga.scraper.Chapter;
import be.bendem.manga.scraper.Scraper;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scraper implementation for http://mangafox.me/
 */
public class MangaFoxScraper implements Scraper {

    public static final String SEARCH_URL = "http://mangafox.me/ajax/search.php?term=";
    public static final String MANGA_URL = "http://mangafox.me/manga/";

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> search(String query) {
        URL url;
        try {
            url = new URL(SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8.displayName()));
        } catch(MalformedURLException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        List<List<String>> parsed;
        try(Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            parsed = (List<List<String>>) new JSONParser().parse(reader);
        } catch(IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        return parsed.stream()
            .collect(Collectors.toMap(
                arr -> arr.get(1),
                arr -> MANGA_URL + arr.get(2),
                (a, b) -> a // If there are duplicates, just ignore them
            ));
    }

    @Override
    public String getName(Document document) {
        String title = document.select("title").text();
        return title.substring(0, title.indexOf(" Manga -"));
    }

    @Override
    public List<Chapter> getChapters(Document document, boolean bonus) {
        String name = getName(document);

        return document.select("#chapters > .chlist > li > div").select("h3, h4").stream()
            .map(element -> {
                Element link = element.select("a").first();
                String number = link.text().substring(name.length() + 1);
                return new Chapter(
                    number,
                    number + ": " + element.select(".title").text(),
                    link.absUrl("href")
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, String> getImageUrlsForChapter(Document document) {
        return document.select("#top_bar div > .m > option").stream()
            .filter(option -> !option.attr("value").equals("0"))
            .collect(Collectors.toMap(
                option -> Integer.parseInt(option.attr("value")),
                option -> {
                    // Code from org.jsoup.nodes.Node.absUrl(String)
                    try {
                        return new URL(new URL(document.baseUri()), option.attr("value") + ".html").toExternalForm();
                    } catch(MalformedURLException e) {
                        // It'll never happen right? :P
                        throw new RuntimeException(e);
                    }
                }
            ));
    }

    @Override
    public String getImageUrl(Document document) {
        return document.select("#image").first().absUrl("src");
    }

}
