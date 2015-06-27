package be.bendem.scraper;

import java.util.Arrays;

public class Utils {

    public static <T> T last(T[] arr) {
        if(arr.length == 0) {
            throw new IllegalArgumentException("No item in the arr");
        }
        return arr[arr.length - 1];
    }

    public static String getExtension(String filename) {
        return last(filename.split("\\."));
    }

    public static String stripExtenstion(String filename) {
        String[] parts = filename.split("\\.");
        return String.join(".", Arrays.copyOf(parts, parts.length - 1));
    }

}
