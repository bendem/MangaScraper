package be.bendem.scraper.implementations;

import be.bendem.scraper.Chapter;
import be.bendem.scraper.Scraper;
import be.bendem.scraper.Utils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scraper implementation for http://www.mangaeden.com/
 */
public class MangaEdenScraper implements Scraper {

    @Override
    public String getName(Document document) {
        return document
            .select("#leftContent > table > tbody > tr:first-child > td:first-child > a > span:first-child")
            .text();
    }

    @Override
    public List<Chapter> getChapters(Document document, boolean bonus) {
        Stream<Element> elemStream = document
            .select("#leftContent > table > tbody > tr > td:first-child > a")
            .stream();

        if(!bonus) {
            elemStream = elemStream.filter(elem -> !elem.select("b").text().contains("."));
        }

        return elemStream
            .map(elem -> new Chapter(
                Utils.get(elem.attr("href").split("/"), -2),
                elem.select("b").text(),
                elem.absUrl("href")
            ))
            .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, String> getImageUrlsForChapter(Document document) {
        return document.select("#top-in > div.top-title > select:nth-child(5) > option").stream()
            .collect(Collectors.toMap(
                option -> Integer.parseInt(option.text()),
                option -> option.absUrl("value")
            ));
    }

    @Override
    public String getImageUrl(Document document) {
        return document.select("#mainImg").first().absUrl("src");
    }

}
