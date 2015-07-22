package be.bendem.manga.scraper;

/**
 * Represents a manga chapter downloadable by the Scraper implementation that
 * built it.
 *
 * Chapters are sorted by their numbers which are represented as doubles
 * because people thought it was clever to have chapter numbers like 1.5.
 */
public class Chapter implements Comparable<Chapter> {

    public final double number;
    public final String name;
    public final String url;

    public Chapter(String number, String name, String url) {
        this(Double.parseDouble(number), name, url);
    }

    public Chapter(double number, String name, String url) {
        this.number = number;
        this.name = name;
        this.url = url;
    }

    @Override
    public int compareTo(Chapter o) {
        return Double.compare(this.number, o.number);
    }

    @Override
    public String toString() {
        return "Chapter{" +
            "number=" + number +
            ", name='" + name + '\'' +
            ", url='" + url + '\'' +
            '}';
    }

}
