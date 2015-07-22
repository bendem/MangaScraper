package be.bendem.manga.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
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
     * @param inputStream the jsoup document fetched by the application
     * @return the name of the manga
     */
    String getName(InputStream inputStream, String url) throws IOException;

    /**
     * Extracts the chapter information from the provided document.
     *
     * @param inputStream the jsoup document fetched by the application
     * @param bonus whether to fetch bonus chapters or not (can be ignored if
     *     there is no reliable way to tell if a chapter is a bonus chapter)
     * @return a list of chapter information
     */
    List<Chapter> getChapters(InputStream inputStream, String url, boolean bonus) throws IOException;

    /**
     * Extracts the list of urls where the images of the chapter can be found.
     *
     * @param inputStream the document fetched using the url provided by {@link
     *     Scraper#getChapters(InputStream, String url, boolean)}
     * @return a Map mapping the image index and its url
     */
    Map<Integer, String> getImageUrlsForChapter(InputStream inputStream, String url) throws IOException;

    /**
     * Extracts the image url from the provided document.
     *
     * @param inputStream the document fetched using the urls provided by {@link
     *     Scraper#getImageUrlsForChapter(InputStream, String url)}
     * @return the url of the image to download
     */
    String getImageUrl(InputStream inputStream, String url) throws IOException;

    /**
     * Searches manga.
     *
     * @param query a search query
     * @return a list of urls which can be handled by {@link
     *     Scraper#getChapters(InputStream, String url, boolean)}
     */
    Map<String, String> search(String query) throws IOException;

    static Document jsoup(InputStream is, String baseUri) throws IOException {
        return Jsoup.parse(is, "UTF-8", baseUri);
    }

}
