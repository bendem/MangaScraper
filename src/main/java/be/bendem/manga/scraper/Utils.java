package be.bendem.manga.scraper;

import java.util.Arrays;

public class Utils {

    public static <T> T get(T[] arr, int index) {
        if(index < 0) {
            return arr[arr.length + index];
        }
        return arr[index];
    }

    public static String getExtension(String filename) {
        return get(filename.split("\\."), -1);
    }

    public static String stripExtenstion(String filename) {
        String[] parts = filename.split("\\.");
        return String.join(".", Arrays.copyOf(parts, parts.length - 1));
    }

}
