package be.bendem.scraper;

public class Range {

    public final double min;
    public final double max;

    public Range() {
        this(0, Double.POSITIVE_INFINITY);
    }

    public Range(double val) {
        this(val, val);
    }

    public Range(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public static Range parse(String str) {
        String[] parts = str.split("-");
        switch(parts.length) {
            case 0: return new Range();
            case 1: return new Range(Double.parseDouble(parts[0]));
            case 2: return new Range(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
            default:
                throw new IllegalArgumentException("Invalid range provided: " + str);
        }
    }

}
