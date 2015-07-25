package be.bendem.manga.scraper.implementations;

import be.bendem.manga.scraper.Chapter;
import be.bendem.manga.scraper.Scraper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Scraper implementation for http://mangafox.me/
 */
public class MangaFoxScraper implements Scraper {

    private static final String SEARCH_URL = "http://mangafox.me/ajax/search.php?term=";
    private static final String MANGA_URL = "http://mangafox.me/manga/";

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> search(String query) throws IOException {
        URL url = new URL(SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8.displayName()));

        JsonArray parsed;
        try(Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            parsed = GSON.fromJson(reader, JsonArray.class);
        }

        return StreamSupport.stream(parsed.spliterator(), false)
            .map(JsonElement::getAsJsonArray)
            .collect(Collectors.toMap(
                arr -> arr.get(1).getAsString(),
                arr -> MANGA_URL + arr.get(2).getAsString(),
                (a, b) -> a // If there are duplicates, just ignore them
            ));
    }

    @Override
    public String getName(InputStream is, String url) throws IOException {
        String title = Scraper.jsoup(is, url).select("title").text();
        return title.substring(0, title.indexOf(" Manga -"));
    }

    @Override
    public List<Chapter> getChapters(InputStream is, String url, boolean bonus) throws IOException {
        String name = getName(is, url);

        return Scraper.jsoup(is, url).select("#chapters > .chlist > li > div").select("h3, h4").stream()
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
    public Map<Integer, String> getImageUrlsForChapter(InputStream is, String url) throws IOException {
        Document document = Scraper.jsoup(is, url);
        return document.select("#top_bar div > .m > option").stream()
            .filter(option -> !option.attr("value").equals("0"))
            .collect(Collectors.toMap(
                option -> Integer.parseInt(option.attr("value")),
                option -> {
                    // Code from org.jsoup.nodes.Node.absUrl(String)
                    try {
                        return new URL(new URL(document.baseUri()), option.attr("value") + ".html").toExternalForm();
                    } catch(MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
            ));
    }

    @Override
    public String getImageUrl(InputStream is, String url) throws IOException {
        return Scraper.jsoup(is, url).select("#image").first().absUrl("src");
    }

}
