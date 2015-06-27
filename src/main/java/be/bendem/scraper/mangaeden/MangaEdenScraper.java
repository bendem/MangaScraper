package be.bendem.scraper.mangaeden;

import be.bendem.scraper.Chapter;
import be.bendem.scraper.Scraper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MangaEdenScraper implements Scraper {

    public String getName(Document document) {
        return document
            .select("#leftContent > table > tbody > tr:first-child > td:first-child > a > span:first-child")
            .text();
    }

    public List<Chapter> getChapters(Document document, boolean bonus) {
        Stream<Element> elemStream = document
            .select("#leftContent > table > tbody > tr > td:first-child > a")
            .stream();

        if(!bonus) {
            elemStream = elemStream.filter(elem -> !elem.select("b").text().contains("."));
        }

        return elemStream
            .map(elem -> new Chapter(elem.absUrl("href"), elem.select("b").text()))
            .collect(Collectors.toList());
    }

    public Map<Integer, String> getImageUrlsFor(Document document) {
        return document.select("#top-in > div.top-title > select:nth-child(5) > option").stream()
            .collect(Collectors.toMap(
                option -> Integer.parseInt(option.text()),
                option -> option.absUrl("value")
            ));
    }

}
