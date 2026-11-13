# Aho-Corasick and KMP

Sabyr Aslan · SE-2528

## Run

Requires JDK 11 or newer; no external dependencies. Run from the repository root:

```text
javac --release 11 -d out src/*.java
java -cp out Tests
java -cp out Demo
java -cp out Benchmark
```

`Demo` searches `ushers` for `he`, `she`, `his`, and `hers`. It finds `she` at `[1,4)`, `he` at `[2,4)`, and `hers` at `[2,6)`. Indices are zero-based Java UTF-16 offsets; the end is exclusive. Matching is case-sensitive, includes overlaps, and treats duplicate patterns as separate pattern IDs. Empty pattern sets and texts are supported; empty patterns and null inputs are rejected.

## Implementation and complexity

`AhoCorasick` inserts all patterns into one trie, sharing common prefixes. Each terminal node stores its pattern IDs. Breadth-first construction sets a failure link to the longest proper suffix that is also a trie prefix. Root children fail to the root. Missing transitions are completed from the failure state's transitions, so searching performs one transition per text character; characters absent from the pattern alphabet reset the state to the root.

An output link points to the nearest terminal state on the failure chain. After every transition, the search reports the current terminal's patterns and follows output links to report suffix patterns too. This finds `he` when `she` ends without copying output lists between nodes. Each match stores its pattern ID and text range.

Let `M` be the total pattern length, `k` the pattern count, `S` the number of trie states (`S <= M+1`), `sigma` the number of distinct UTF-16 characters in the patterns, `n` the text length, and `z` the number of reported matches. Alphabet-map operations use expected constant time.

| Algorithm | Construction | Search | Stored matcher space |
|---|---|---|---|
| Aho-Corasick | O(M + S sigma) | O(n + z) expected | O(S(sigma+1) + k) |
| KMP separately for every pattern | O(M) | O(kn + z) | O(M + k) |

Both return a list requiring O(z) additional space. Aho-Corasick uses a completed transition array per state over the compressed alphabet; for a fixed small alphabet construction and storage are linear in M. A large alphabet increases its memory cost. KMP stores one prefix-function array per pattern, reusing the longest valid prefix after mismatches and matches.

## Experiments

All nine combinations of 10, 100, and 1,000 patterns with texts of 1,000, 10,000, and 100,000 characters are measured. Unique patterns contain 4-12 lowercase English letters, with seed `42 + patternCount`. Base texts use seed `42 + textLength`; the first `min(patternCount, textLength/25)` patterns are planted at offsets `0,25,50,...` to ensure matches. Remaining positions are random. Each pair uses identical data for both algorithms.

Each case has two untimed warm-ups and five measured runs using `System.nanoTime()`. Algorithm order alternates between measured runs. Construction and search are timed separately; total includes both. Search includes creating all match objects. Generation, result comparison, and CSV writing are outside timing. Every run checks exact agreement of pattern IDs and positions after sorting the results outside timing.

Recorded on Windows 11 amd64, JDK 25.0.1, compiled with `--release 11`. Values below are mean milliseconds. [averages.csv](results/averages.csv) includes construction, search, and total means; [runs.csv](results/runs.csv) contains all 90 individual algorithm measurements in nanoseconds. `Benchmark` recreates both files.

| Patterns | Text length | AC build | AC search | AC total | KMP build | KMP search | KMP total |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 1,000 | 0.117 | 0.116 | 0.233 | 0.008 | 0.328 | 0.336 |
| 10 | 10,000 | 0.112 | 0.459 | 0.571 | 0.010 | 1.408 | 1.419 |
| 10 | 100,000 | 0.098 | 1.024 | 1.122 | 0.009 | 2.092 | 2.101 |
| 100 | 1,000 | 0.279 | 0.013 | 0.292 | 0.036 | 0.203 | 0.239 |
| 100 | 10,000 | 0.266 | 0.114 | 0.380 | 0.047 | 2.178 | 2.225 |
| 100 | 100,000 | 0.206 | 1.149 | 1.355 | 0.042 | 21.029 | 21.072 |
| 1,000 | 1,000 | 0.816 | 0.015 | 0.831 | 0.251 | 2.076 | 2.327 |
| 1,000 | 10,000 | 0.596 | 0.123 | 0.719 | 0.126 | 20.924 | 21.050 |
| 1,000 | 100,000 | 0.761 | 1.320 | 2.081 | 0.055 | 211.233 | 211.288 |

## Discussion

Aho-Corasick scans the text once for the entire pattern set, while separate KMP searches scan it k times. At 1,000 patterns and 100,000 characters, the measured totals were 2.081 ms versus 211.288 ms, approximately 102 times faster for Aho-Corasick. Construction can outweigh this advantage on short inputs: KMP was faster overall for 100 patterns and 1,000 characters. Reusing a built automaton avoids rebuilding it for later texts.

KMP has simpler preprocessing and lower memory requirements, making it useful for a single pattern or small workloads. Aho-Corasick benefits larger fixed pattern sets, but its transition table costs more memory, changing patterns requires rebuilding this implementation, and reporting many matches still costs O(z). Random lowercase inputs cover only one workload; shared-prefix patterns, alphabet size, and match density affect performance. JVM compilation, allocation, and timer noise affect these short runs, so the measurements do not establish universal speed ratios.

## Tests

`Tests` performs 632 checks, including classic suffix matches, overlapping occurrences, duplicate patterns, long failure chains, case sensitivity, empty inputs, absent characters, UTF-16 text, invalid arguments, and independent repeated searches. It compares both algorithms with an independent naive search on 300 reproducible random cases.
