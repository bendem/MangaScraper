package be.bendem.manga.scraper.implementations;

import be.bendem.manga.scraper.Chapter;
import be.bendem.manga.scraper.Scraper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import java.util.stream.StreamSupport;

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

        JsonObject parsed;
        try(Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            parsed = GSON.fromJson(reader, JsonObject.class);
        }

        JsonArray listParsed = parsed.getAsJsonArray("manga");
        return StreamSupport.stream(listParsed.spliterator(), false)
            .map(JsonElement::getAsJsonObject)
            .filter(jsonObject -> jsonObject.get("t").getAsString().toLowerCase().contains(lquery))
            .collect(Collectors.toMap(
                manga -> manga.get("t").getAsString(),
                manga -> MANGA_URL + manga.get("i").getAsString(),
                (a, b) -> a,
                () -> new TreeMap<>((result1, result2) -> {
                    if(result1.equalsIgnoreCase(lquery)) {
                        return -1;
                    }
                    if(result2.equalsIgnoreCase(lquery)) {
                        return 1;
                    }
                    if(result1.toLowerCase().startsWith(lquery) && result2.toLowerCase().startsWith(lquery)) {
                        return result1.compareToIgnoreCase(result2);
                    }
                    if(result1.toLowerCase().startsWith(lquery)) {
                        return -1;
                    }
                    if(result2.toLowerCase().startsWith(lquery)) {
                        return 1;
                    }
                    return result1.compareToIgnoreCase(result2);
                })
            ));
    }

    @Override
    public String getName(InputStream inputStream, String url) throws IOException {
        try(Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonObject.class).get("title").getAsString();
        }
    }

    @Override
    public List<Chapter> getChapters(InputStream inputStream, String url, boolean bonus) throws IOException {
        Gson gson = new Gson();
        JsonObject parsed;
        try(Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            parsed = gson.fromJson(reader, JsonObject.class);
        }

        JsonArray chapters = parsed.get("chapters").getAsJsonArray();
        Stream<JsonArray> chapterStream = StreamSupport.stream(chapters.spliterator(), false)
            .map(JsonElement::getAsJsonArray);

        if (!bonus) {
            chapterStream = chapterStream.filter(chapter -> {
                double asDouble = chapter.get(0).getAsDouble();
                return Math.ceil(asDouble) == asDouble;
            });
        }

        return chapterStream
            .map(chapter -> new Chapter(
                chapter.get(0).getAsDouble(),
                chapter.get(2).isJsonNull() ? "" : chapter.get(2).getAsString(),
                CHAPTER_URL + chapter.get(3).getAsString()
            ))
            .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, String> getImageUrlsForChapter(InputStream inputStream, String url) throws IOException {
        JsonObject parsed;
        try(Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            parsed = GSON.fromJson(reader, JsonObject.class);
        }

        Stream<JsonArray> streamParsed = StreamSupport.stream(parsed.get("images").getAsJsonArray().spliterator(), false)
            .map(JsonElement::getAsJsonArray);

        return streamParsed
            .collect(Collectors.toMap(
                image -> image.get(0).getAsInt(),
                image -> IMAGE_URL + image.get(1).getAsString()
            ));
    }

    @Override
    public String getImageUrl(InputStream inputStream, String url) throws IOException {
        return url;
    }

}
