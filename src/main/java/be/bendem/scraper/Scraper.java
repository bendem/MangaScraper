package be.bendem.scraper;

import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Map;

public interface Scraper {

    String getName(Document document);

    List<Chapter> getChapters(Document document, boolean bonus);

    Map<Integer, String> getImageUrlsFor(Document document);

}
