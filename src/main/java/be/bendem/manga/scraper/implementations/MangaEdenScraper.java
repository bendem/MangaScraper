package be.bendem.manga.scraper.implementations;

import be.bendem.manga.scraper.Chapter;
import be.bendem.manga.scraper.Scraper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scraper implementation for https://www.mangaeden.com/ using api
 */
public class MangaEdenScraper implements Scraper {

    private static final String LIST_URL = "https://www.mangaeden.com/api/list/0/";
    private static final String MANGA_URL = "https://www.mangaeden.com/api/manga/";
    private static final String CHAPTER_URL = "https://www.mangaeden.com/api/chapter/";
    private static final String IMAGE_URL = "https://cdn.mangaeden.com/mangasimg/";

    @Override
    public Map<String, String> search(String query) throws IOException {
        URL url = new URL(LIST_URL);
        String lquery = query.toLowerCase();

        JSONObject parsed;
        try(Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            parsed = (JSONObject) new JSONParser().parse(reader);
        } catch(ParseException e) {
            throw new RuntimeException(e);
        }

        List<JSONObject> listParsed = (List<JSONObject>) parsed.get("manga");
        return listParsed.stream()
            .filter(jsonObject -> ((String) jsonObject.get("t")).toLowerCase().contains(lquery))
            .collect(Collectors.toMap(
                manga -> (String) manga.get("t"),
                manga -> MANGA_URL + manga.get("i"),
                (a, b) -> a,
                () -> new TreeMap<>((result1, result2) -> {
                    if (result1.equalsIgnoreCase(lquery)) {
                        return -1;
                    }
                    if (result2.equalsIgnoreCase(lquery)) {
                        return 1;
                    }
                    if (result1.toLowerCase().startsWith(lquery)) {
                        return -1;
                    }
                    if (result2.toLowerCase().startsWith(lquery)) {
                        return 1;
                    }
                    return 1;
                })
            ));
    }

    @Override
    public String getName(InputStream inputStream, String url) throws IOException {
        JSONObject parsed;
        try(Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            parsed = (JSONObject) new JSONParser().parse(reader);
        } catch(ParseException e) {
            throw new RuntimeException(e);
        }

        return (String) parsed.get("title");
    }

    @Override
    public List<Chapter> getChapters(InputStream inputStream, String url, boolean bonus) throws IOException {
        JSONObject parsed;
        try(Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            parsed = (JSONObject) new JSONParser().parse(reader);
        } catch(ParseException e) {
            throw new RuntimeException(e);
        }

        Stream<JSONArray> streamParsed = ((List<JSONArray>) parsed.get("chapters")).stream();

        if (!bonus) {
            streamParsed = streamParsed.filter(chapter -> chapter.get(0) instanceof Long);
        }

        return streamParsed
            .map(chapter -> new Chapter(
                ((Number) chapter.get(0)).doubleValue(),
                (String) chapter.get(2),
                CHAPTER_URL + chapter.get(3)
            ))
            .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, String> getImageUrlsForChapter(InputStream inputStream, String url) throws IOException {
        JSONObject parsed;
        try(Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            parsed = (JSONObject) new JSONParser().parse(reader);
        } catch(ParseException e) {
            throw new RuntimeException(e);
        }

        Stream<JSONArray> streamParsed = ((List<JSONArray>) parsed.get("images")).stream();

        return streamParsed
            .collect(Collectors.toMap(
                image -> ((Number) image.get(0)).intValue(),
                image -> IMAGE_URL + image.get(1)
            ));
    }

    @Override
    public String getImageUrl(InputStream inputStream, String url) throws IOException {
        return url;
    }

}
