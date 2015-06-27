package be.bendem.scraper;

import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Map;

public interface Scraper {

    /**
     * Extracts the name of the manga from the provided document.
     *
     * @param document the jsoup document fetched by the application
     * @return the name of the manga
     */
    String getName(Document document);

    /**
     * Extracts the chapter information from the provided document.
     *
     * @param document the jsoup document fetched by the application
     * @param bonus whether to fetch bonus chapters or not (can be ignored if
     *     there is no reliable way to tell if a chapter is a bonus chapter)
     * @return a list of chapter information ordered from first to last
     */
    List<Chapter> getChapters(Document document, boolean bonus);

    /**
     * Extracts the list of urls where the images of the chapter can be found.
     *
     * @param document the document fetched using the url provided by {@link
     *     Scraper#getChapters(Document, boolean)}
     * @return a Map mapping the image index and its url
     */
    Map<Integer, String> getImageUrlsForChapter(Document document);

    /**
     * Extracts the image url from the provided document.
     *
     * @param document the document fetched using the urls provided by {@link
     *     Scraper#getImageUrlsForChapter(Document)}
     * @return the url of the image to download
     */
    String getImageUrl(Document document);

}
