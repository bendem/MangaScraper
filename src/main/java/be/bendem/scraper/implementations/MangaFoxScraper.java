package be.bendem.scraper.implementations;

import be.bendem.scraper.Chapter;
import be.bendem.scraper.Scraper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MangaFoxScraper implements Scraper {

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
