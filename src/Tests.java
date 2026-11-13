import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class Tests {
    private static int checks;

    public static void main(String[] args) {
        verify(List.of("he", "she", "his", "hers"), "ushers",
                List.of(new Match(1, 1, 4), new Match(0, 2, 4), new Match(3, 2, 6)));
        verify(List.of("a", "aa", "aaa"), "aaaa", naive(List.of("a", "aa", "aaa"), "aaaa"));
        verify(List.of("aba", "aba", "ba"), "ababa",
                naive(List.of("aba", "aba", "ba"), "ababa"));
        verify(List.of("abcd", "bcd", "cd", "d"), "xabcd!abcd",
                naive(List.of("abcd", "bcd", "cd", "d"), "xabcd!abcd"));
        verify(List.of("ababaca", "abac", "bac"), "abababacababaca",
                naive(List.of("ababaca", "abac", "bac"), "abababacababaca"));
        verify(List.of("abc", "ABC"), "ABC abc", List.of(new Match(1, 0, 3), new Match(0, 4, 7)));
        verify(List.of("abc", "longpattern"), "xyz", List.of());
        verify(List.of("abc"), "", List.of());
        verify(List.of(), "abc", List.of());
        verify(List.of("\u0430\u0431", "\u03b2", "\uD83D\uDE00", "\uDE00", "\u0000"),
                "\u0430\u0431\u03b2\uD83D\uDE00\u0000",
                naive(List.of("\u0430\u0431", "\u03b2", "\uD83D\uDE00", "\uDE00", "\u0000"),
                        "\u0430\u0431\u03b2\uD83D\uDE00\u0000"));
        AhoCorasick reusable = new AhoCorasick(List.of("ab", "b"));
        check(reusable.findAll("a").isEmpty(), "partial prefix");
        check(reusable.findAll("b").equals(List.of(new Match(1, 0, 1))), "independent searches");
        List<String> mutable = new ArrayList<>(List.of("ab"));
        AhoCorasick aho = new AhoCorasick(mutable);
        Kmp kmp = new Kmp(mutable);
        mutable.set(0, "xx");
        check(aho.findAll("ab").equals(kmp.findAll("ab")), "defensive pattern copy");
        check(aho.findAll("ab").equals(List.of(new Match(0, 0, 2))), "original pattern retained");
        expect(IllegalArgumentException.class, () -> new AhoCorasick(List.of("")));
        expect(IllegalArgumentException.class, () -> new Kmp(List.of("")));
        expect(NullPointerException.class, () -> new AhoCorasick(null));
        expect(NullPointerException.class, () -> new Kmp(null));
        expect(NullPointerException.class, () -> new AhoCorasick(Arrays.asList("a", null)));
        expect(NullPointerException.class, () -> new Kmp(Arrays.asList("a", null)));
        expect(NullPointerException.class, () -> aho.findAll(null));
        expect(NullPointerException.class, () -> kmp.findAll(null));
        Random random = new Random(42);
        for (int round = 0; round < 300; round++) {
            List<String> patterns = new ArrayList<>();
            int count = random.nextInt(13);
            for (int i = 0; i < count; i++) {
                patterns.add(randomString(random, 1 + random.nextInt(9)));
            }
            String text = randomString(random, random.nextInt(81));
            verify(patterns, text, naive(patterns, text));
        }
        System.out.println("All " + checks + " checks passed");
    }

    private static void verify(List<String> patterns, String text, List<Match> expected) {
        check(canonical(new AhoCorasick(patterns).findAll(text)).equals(canonical(expected)),
                "Aho-Corasick: " + patterns + " / " + text);
        check(canonical(new Kmp(patterns).findAll(text)).equals(canonical(expected)),
                "KMP: " + patterns + " / " + text);
    }

    private static List<Match> naive(List<String> patterns, String text) {
        List<Match> result = new ArrayList<>();
        for (int index = 0; index < patterns.size(); index++) {
            String pattern = patterns.get(index);
            for (int start = 0; start + pattern.length() <= text.length(); start++) {
                if (text.startsWith(pattern, start)) {
                    result.add(new Match(index, start, start + pattern.length()));
                }
            }
        }
        return result;
    }

    private static List<Match> canonical(List<Match> matches) {
        List<Match> copy = new ArrayList<>(matches);
        copy.sort(Comparator.comparingInt((Match match) -> match.patternIndex)
                .thenComparingInt(match -> match.start).thenComparingInt(match -> match.endExclusive));
        return copy;
    }

    private static String randomString(Random random, int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append((char) ('a' + random.nextInt(4)));
        }
        return result.toString();
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            check(type.isInstance(error), "exception type");
            return;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
