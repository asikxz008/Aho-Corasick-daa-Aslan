import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Kmp {
    private final List<String> patterns;
    private final List<int[]> prefixes = new ArrayList<>();

    public Kmp(List<String> patterns) {
        this.patterns = List.copyOf(Objects.requireNonNull(patterns));
        for (String pattern : this.patterns) {
            if (pattern.isEmpty()) {
                throw new IllegalArgumentException("patterns must be non-empty");
            }
            int[] prefix = new int[pattern.length()];
            for (int i = 1, matched = 0; i < pattern.length(); i++) {
                while (matched > 0 && pattern.charAt(i) != pattern.charAt(matched)) {
                    matched = prefix[matched - 1];
                }
                if (pattern.charAt(i) == pattern.charAt(matched)) {
                    matched++;
                }
                prefix[i] = matched;
            }
            prefixes.add(prefix);
        }
    }

    public List<Match> findAll(String text) {
        Objects.requireNonNull(text);
        List<Match> matches = new ArrayList<>();
        for (int index = 0; index < patterns.size(); index++) {
            String pattern = patterns.get(index);
            int[] prefix = prefixes.get(index);
            for (int i = 0, matched = 0; i < text.length(); i++) {
                while (matched > 0 && text.charAt(i) != pattern.charAt(matched)) {
                    matched = prefix[matched - 1];
                }
                if (text.charAt(i) == pattern.charAt(matched)) {
                    matched++;
                }
                if (matched == pattern.length()) {
                    matches.add(new Match(index, i + 1 - matched, i + 1));
                    matched = prefix[matched - 1];
                }
            }
        }
        return matches;
    }
}
