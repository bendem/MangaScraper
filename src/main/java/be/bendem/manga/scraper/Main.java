package be.bendem.manga.scraper;

import java.nio.file.Paths;
import java.util.Map;

public class Main {

    private final MangaScraper scraper;

    public Main(MangaScraper scraper) {
        this.scraper = scraper;
    }

    public void search(String query) {
        System.out.println("| Search results for '" + query + "'");
        System.out.println('|');

        Map<String, String> search = scraper.search(query, Throwable::printStackTrace);
        if(search.isEmpty()) {
            System.out.println("| <no results>");
            return;
        }

        System.out.println("| " + search.size() + " results found:");
        search
            .entrySet().stream()
            .map(e -> "| " + e.getKey() + ": " + e.getValue())
            .forEach(System.out::println);
    }

    public static void main(String[] args) {
        String url = null;
        String implementation = "MangaEdenScraper";
        String output = "download";
        String search = null;
        Range range = new Range();

        for(int i = 0; i < args.length; ++i) {
            switch(args[i]) {
                case "-r":
                    range = Range.parse(args[++i]);
                    break;
                case "-i":
                    implementation = args[++i];
                    break;
                case "-o":
                    output = args[++i];
                    break;
                case "-s":
                    search = args[++i];
                    break;
                case "-h":
                case "-help":
                case "--help":
                    printHelp();
                    return;
                default:
                    url = args[i];
            }
        }

        if(url == null && search == null) {
            printHelp();
            return;
        }

        if(!implementation.contains(".")) {
            implementation = "be.bendem.manga.scraper.implementations." + implementation;
        }

        Scraper scraperImpl;
        try {
            scraperImpl = Scraper.class.cast(Class.forName(implementation).newInstance());
        } catch(InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Main main = new Main(new MangaScraper(scraperImpl));
        if(search != null) {
            main.search(search);
            return;
        }

        main.scraper.download(
            url, range, Paths.get(output),
            (current, total) -> System.out.println(current + " / " + total),
            (chapter, e) -> {
                if(chapter != null) {
                    System.err.println("Error downloading " + chapter.number + " '" + chapter.name + "'");
                }
                e.printStackTrace();
            }
        );
    }

    private static void printHelp() {
        System.err.println();
        System.err.println("Usage: java -jar jarfile.jar [-i <implementation>] [-r <range>] [-o <output>] <-s <query>|<url>>");
        System.err.println("    <range>          Is either a number (like 1) or two numbers separated with a");
        System.err.println("                     dash (like 1-5). Default value is 0-INFINITY");
        System.err.println();
        System.err.println("    <implementation> Specify the FQN (or internal name) of the class implementing");
        System.err.println("                     Scraper to use");
        System.err.println();
        System.err.println("    <output>         Specify the folder to put the downloads in. Default value is download");
        System.err.println();
        System.err.println("    <query>          A search query");
        System.err.println();
        System.err.println("    <url>            is a valid url for the chosen implementation");
        System.err.println();
    }

}
