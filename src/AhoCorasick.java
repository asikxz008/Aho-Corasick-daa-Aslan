import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AhoCorasick {
    private static final class Node {
        private final int[] next;
        private final List<Integer> patterns = new ArrayList<>();
        private int failure;
        private int output = -1;

        private Node(int alphabetSize) {
            next = new int[alphabetSize];
            Arrays.fill(next, -1);
        }
    }

    private final List<String> patterns;
    private final Map<Character, Integer> alphabet = new HashMap<>();
    private final List<Node> nodes = new ArrayList<>();

    public AhoCorasick(List<String> patterns) {
        this.patterns = List.copyOf(Objects.requireNonNull(patterns));
        for (String pattern : this.patterns) {
            if (pattern.isEmpty()) {
                throw new IllegalArgumentException("patterns must be non-empty");
            }
            for (int i = 0; i < pattern.length(); i++) {
                char character = pattern.charAt(i);
                if (!alphabet.containsKey(character)) {
                    alphabet.put(character, alphabet.size());
                }
            }
        }
        nodes.add(new Node(alphabet.size()));
        for (int index = 0; index < this.patterns.size(); index++) {
            String pattern = this.patterns.get(index);
            int state = 0;
            for (int i = 0; i < pattern.length(); i++) {
                int letter = alphabet.get(pattern.charAt(i));
                Node node = nodes.get(state);
                if (node.next[letter] == -1) {
                    node.next[letter] = nodes.size();
                    nodes.add(new Node(alphabet.size()));
                }
                state = node.next[letter];
            }
            nodes.get(state).patterns.add(index);
        }
        buildFailureLinks();
    }

    private void buildFailureLinks() {
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        Node root = nodes.get(0);
        for (int letter = 0; letter < alphabet.size(); letter++) {
            if (root.next[letter] == -1) {
                root.next[letter] = 0;
            } else {
                queue.add(root.next[letter]);
            }
        }
        while (!queue.isEmpty()) {
            int state = queue.remove();
            Node node = nodes.get(state);
            Node fallback = nodes.get(node.failure);
            for (int letter = 0; letter < alphabet.size(); letter++) {
                int child = node.next[letter];
                if (child == -1) {
                    node.next[letter] = fallback.next[letter];
                } else {
                    Node childNode = nodes.get(child);
                    childNode.failure = fallback.next[letter];
                    Node failureNode = nodes.get(childNode.failure);
                    childNode.output = failureNode.patterns.isEmpty()
                            ? failureNode.output : childNode.failure;
                    queue.add(child);
                }
            }
        }
    }

    public List<Match> findAll(String text) {
        Objects.requireNonNull(text);
        List<Match> matches = new ArrayList<>();
        int state = 0;
        for (int i = 0; i < text.length(); i++) {
            Integer letter = alphabet.get(text.charAt(i));
            if (letter == null) {
                state = 0;
                continue;
            }
            state = nodes.get(state).next[letter];
            for (int output = state; output != -1; output = nodes.get(output).output) {
                for (int index : nodes.get(output).patterns) {
                    matches.add(new Match(index, i + 1 - patterns.get(index).length(), i + 1));
                }
            }
        }
        return matches;
    }
}
