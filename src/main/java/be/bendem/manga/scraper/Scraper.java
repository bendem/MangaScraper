package be.bendem.manga.scraper;

import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Map;

/**
 * Keep in mind when implementing this interface that the scraper logic is:
 * + get the summary document
 * + get the manga name out of that
 * + get the manga chapter urls out of that as well
 * + for each chapter url, get the page urls
 * + for each page url, get the image url
 */
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
     * @return a list of chapter information
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

    /**
     * Searches manga.
     *
     * @param query a search query
     * @return a list of urls which can be handled by {@link
     *     Scraper#getChapters(Document, boolean)}
     */
    Map<String, String> search(String query);

}
