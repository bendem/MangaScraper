package be.bendem.scraper.implementations;

import be.bendem.scraper.Chapter;
import be.bendem.scraper.Scraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scraper implementation for http://www.mangatown.com/
 */
public class MangaTownScraper implements Scraper {

    @Override
    public Map<String, String> search(String query) {
        Document search;
        try {
            search = Jsoup
                .connect("http://www.mangatown.com/search.php")
                .data("name", query)
                .get();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        return search.select(".search_result > ul > li > .title > a").stream()
            .collect(Collectors.toMap(
                a -> a.attr("title"),
                a -> a.absUrl("href"),
                (a, b) -> a // If there are duplicates, just ignore them
            ));
    }

    @Override
    public String getName(Document document) {
        return document.select(".title-top").text();
    }

    @Override
    public List<Chapter> getChapters(Document document, boolean bonus) {
        String name = getName(document);
        return document.select(".chapter_list > li").stream()
            .map(element -> new Chapter(
                element.select("a").text().substring(name.length() + 1),
                element.select("span").first().text(),
                element.select("a").first().absUrl("href")
            ))
            .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, String> getImageUrlsForChapter(Document document) {
        return document.select("#top_chapter_list + .page_select > select > option").stream()
            .collect(Collectors.toMap(
                option -> Integer.parseInt(option.text()),
                option -> option.absUrl("value")
            ));
    }

    @Override
    public String getImageUrl(Document document) {
        String src = document.select("#image").first().absUrl("src");
        return src.substring(0, src.indexOf('?'));
    }

}
