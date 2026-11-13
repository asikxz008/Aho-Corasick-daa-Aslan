import java.util.Objects;

public final class Match {
    public final int patternIndex;
    public final int start;
    public final int endExclusive;

    public Match(int patternIndex, int start, int endExclusive) {
        this.patternIndex = patternIndex;
        this.start = start;
        this.endExclusive = endExclusive;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Match)) {
            return false;
        }
        Match match = (Match) other;
        return patternIndex == match.patternIndex && start == match.start
                && endExclusive == match.endExclusive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(patternIndex, start, endExclusive);
    }

    @Override
    public String toString() {
        return "pattern=" + patternIndex + ", range=[" + start + ", " + endExclusive + ")";
    }
}
