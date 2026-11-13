import java.util.List;

public final class Demo {
    public static void main(String[] args) {
        List<String> patterns = List.of("he", "she", "his", "hers");
        String text = "ushers";
        System.out.println("Patterns: " + patterns);
        System.out.println("Text: " + text);
        for (Match match : new AhoCorasick(patterns).findAll(text)) {
            System.out.println(patterns.get(match.patternIndex) + ": " + match);
        }
    }
}
