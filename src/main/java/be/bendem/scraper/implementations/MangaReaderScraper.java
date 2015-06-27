package be.bendem.scraper.implementations;

import be.bendem.scraper.Chapter;
import be.bendem.scraper.Scraper;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scraper implementation for http://www.mangareader.net/
 */
public class MangaReaderScraper implements Scraper {

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
