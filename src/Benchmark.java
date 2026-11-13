import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class Benchmark {
    private static final int RUNS = 5;
    private static volatile long sink;

    private static final class Measurement {
        private final long build;
        private final long search;
        private final List<Match> matches;

        private Measurement(long build, long search, List<Match> matches) {
            this.build = build;
            this.search = search;
            this.matches = matches;
        }
    }

    public static void main(String[] args) throws IOException {
        StringBuilder summary = new StringBuilder("patterns,text_length,matches,aho_build_ms,"
                + "aho_search_ms,aho_total_ms,kmp_build_ms,kmp_search_ms,kmp_total_ms\n");
        StringBuilder raw = new StringBuilder("patterns,text_length,algorithm,run,build_ns,search_ns,total_ns\n");
        for (int count : new int[]{10, 100, 1_000}) {
            List<String> patterns = patterns(count);
            for (int length : new int[]{1_000, 10_000, 100_000}) {
                String text = text(patterns, length);
                for (int warmup = 0; warmup < 2; warmup++) {
                    verify(measureAho(patterns, text), measureKmp(patterns, text));
                }
                long ahoBuild = 0;
                long ahoSearch = 0;
                long kmpBuild = 0;
                long kmpSearch = 0;
                int matches = 0;
                for (int run = 1; run <= RUNS; run++) {
                    Measurement aho;
                    Measurement kmp;
                    if (run % 2 == 1) {
                        aho = measureAho(patterns, text);
                        kmp = measureKmp(patterns, text);
                    } else {
                        kmp = measureKmp(patterns, text);
                        aho = measureAho(patterns, text);
                    }
                    verify(aho, kmp);
                    matches = aho.matches.size();
                    ahoBuild += aho.build;
                    ahoSearch += aho.search;
                    kmpBuild += kmp.build;
                    kmpSearch += kmp.search;
                    appendRaw(raw, count, length, "Aho-Corasick", run, aho);
                    appendRaw(raw, count, length, "KMP", run, kmp);
                }
                double divisor = RUNS * 1_000_000.0;
                summary.append(String.format(Locale.ROOT,
                        "%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f%n",
                        count, length, matches, ahoBuild / divisor, ahoSearch / divisor,
                        (ahoBuild + ahoSearch) / divisor, kmpBuild / divisor, kmpSearch / divisor,
                        (kmpBuild + kmpSearch) / divisor));
                System.out.printf(Locale.ROOT, "%d patterns, %d chars: AC %.3f ms, KMP %.3f ms, %d matches%n",
                        count, length, (ahoBuild + ahoSearch) / divisor,
                        (kmpBuild + kmpSearch) / divisor, matches);
            }
        }
        Path results = Path.of("results");
        Files.createDirectories(results);
        Files.writeString(results.resolve("averages.csv"), summary, StandardCharsets.UTF_8);
        Files.writeString(results.resolve("runs.csv"), raw, StandardCharsets.UTF_8);
        System.out.println(System.getProperty("java.runtime.version") + " / "
                + System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
    }

    private static Measurement measureAho(List<String> patterns, String text) {
        long start = System.nanoTime();
        AhoCorasick matcher = new AhoCorasick(patterns);
        long built = System.nanoTime();
        List<Match> matches = matcher.findAll(text);
        long searched = System.nanoTime();
        return new Measurement(built - start, searched - built, matches);
    }

    private static Measurement measureKmp(List<String> patterns, String text) {
        long start = System.nanoTime();
        Kmp matcher = new Kmp(patterns);
        long built = System.nanoTime();
        List<Match> matches = matcher.findAll(text);
        long searched = System.nanoTime();
        return new Measurement(built - start, searched - built, matches);
    }

    private static void verify(Measurement aho, Measurement kmp) {
        Comparator<Match> order = Comparator.comparingInt((Match match) -> match.patternIndex)
                .thenComparingInt(match -> match.start).thenComparingInt(match -> match.endExclusive);
        aho.matches.sort(order);
        kmp.matches.sort(order);
        if (!aho.matches.equals(kmp.matches)) {
            throw new AssertionError("Aho-Corasick and KMP results differ");
        }
        long checksum = 0;
        for (Match match : aho.matches) {
            checksum += match.patternIndex + match.start + match.endExclusive;
        }
        sink = checksum;
    }

    private static void appendRaw(StringBuilder csv, int count, int length, String algorithm,
                                  int run, Measurement measurement) {
        csv.append(count).append(',').append(length).append(',').append(algorithm).append(',')
                .append(run).append(',').append(measurement.build).append(',')
                .append(measurement.search).append(',').append(measurement.build + measurement.search)
                .append('\n');
    }

    private static List<String> patterns(int count) {
        Random random = new Random(42L + count);
        Set<String> used = new HashSet<>();
        List<String> patterns = new ArrayList<>();
        while (patterns.size() < count) {
            String pattern = randomString(random, 4 + random.nextInt(9));
            if (used.add(pattern)) {
                patterns.add(pattern);
            }
        }
        return patterns;
    }

    private static String text(List<String> patterns, int length) {
        StringBuilder text = new StringBuilder(randomString(new Random(42L + length), length));
        int planted = Math.min(patterns.size(), length / 25);
        for (int i = 0; i < planted; i++) {
            String pattern = patterns.get(i);
            int start = i * 25;
            text.replace(start, start + pattern.length(), pattern);
        }
        return text.toString();
    }

    private static String randomString(Random random, int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append((char) ('a' + random.nextInt(26)));
        }
        return result.toString();
    }
}
