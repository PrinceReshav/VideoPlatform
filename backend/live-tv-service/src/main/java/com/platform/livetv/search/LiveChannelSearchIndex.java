package com.platform.livetv.search;

import com.platform.livetv.entity.LiveChannel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory read-optimized search index for the IPTV catalogue.
 *
 * Uses a Bloom filter as an O(1) negative gate and a trigram inverted index
 * for fast substring candidate generation. Exact substring matching is then
 * applied to eliminate Bloom/trigram false positives. The database remains
 * the source of truth; this index is rebuilt after IPTV synchronization.
 */
public final class LiveChannelSearchIndex {
    private static final int GRAM = 3;
    private static final int BLOOM_BITS = 1 << 20;
    private static final int BLOOM_HASHES = 4;

    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(Snapshot.empty());

    public void rebuild(Collection<LiveChannel> channels) {
        List<Entry> entries = new ArrayList<>();
        if (channels != null) {
            for (LiveChannel channel : channels) {
                if (channel == null || channel.getId() == null || channel.isDeleted()) continue;
                String searchable = normalize(String.join(" ",
                        safe(channel.getName()),
                        safe(channel.getCountry()),
                        safe(channel.getCategory()),
                        channel.getCategories() == null ? "" : String.join(" ", channel.getCategories()),
                        safe(channel.getLanguage()),
                        safe(channel.getTvgName()),
                        safe(channel.getTvgId())));
                entries.add(new Entry(channel.getId(), searchable));
            }
        }

        Map<String, BitSet> grams = new HashMap<>();
        Bloom bloom = new Bloom(BLOOM_BITS, BLOOM_HASHES);
        Map<UUID, Integer> ordinals = new HashMap<>(Math.max(16, entries.size() * 2));

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            ordinals.put(entry.id(), i);
            for (String gram : grams(entry.searchable())) {
                bloom.add(gram);
                grams.computeIfAbsent(gram, ignored -> new BitSet(entries.size())).set(i);
            }
        }

        snapshot.set(new Snapshot(List.copyOf(entries), Collections.unmodifiableMap(grams), bloom, Collections.unmodifiableMap(ordinals)));
    }

    public boolean isBuilt() {
        return !snapshot.get().entries().isEmpty();
    }

    public Set<UUID> matchingIds(String query) {
        Snapshot s = snapshot.get();
        String normalized = normalize(query);
        if (normalized.isBlank()) {
            Set<UUID> all = new HashSet<>(s.entries().size() * 2);
            for (Entry e : s.entries()) all.add(e.id());
            return all;
        }

        BitSet candidates = candidateBits(s, normalized);
        Set<UUID> result = new HashSet<>();
        for (int ordinal = candidates.nextSetBit(0); ordinal >= 0; ordinal = candidates.nextSetBit(ordinal + 1)) {
            Entry entry = s.entries().get(ordinal);
            if (entry.searchable().contains(normalized)) result.add(entry.id());
        }
        return result;
    }

    public Set<UUID> matchingIds(String country, String category, String query) {
        Snapshot s = snapshot.get();
        String normalized = normalize(query);
        BitSet candidates = normalized.isBlank()
                ? allBits(s.entries().size())
                : candidateBits(s, normalized);

        Set<UUID> result = new HashSet<>();
        for (int ordinal = candidates.nextSetBit(0); ordinal >= 0; ordinal = candidates.nextSetBit(ordinal + 1)) {
            Entry entry = s.entries().get(ordinal);
            if (entry.searchable().contains(normalized)) result.add(entry.id());
        }
        return result;
    }

    private BitSet candidateBits(Snapshot s, String query) {
        if (query.length() < GRAM) return allBits(s.entries().size());

        BitSet result = null;
        for (String gram : grams(query)) {
            if (!s.bloom().mightContain(gram)) return new BitSet();
            BitSet bucket = s.grams().get(gram);
            if (bucket == null) return new BitSet();
            if (result == null) result = (BitSet) bucket.clone();
            else result.and(bucket);
            if (result.isEmpty()) return result;
        }
        return result == null ? allBits(s.entries().size()) : result;
    }

    private static BitSet allBits(int size) {
        BitSet bits = new BitSet(size);
        bits.set(0, size);
        return bits;
    }

    private static List<String> grams(String value) {
        if (value.length() < GRAM) return List.of(value);
        List<String> result = new ArrayList<>(value.length() - GRAM + 1);
        for (int i = 0; i <= value.length() - GRAM; i++) result.add(value.substring(i, i + GRAM));
        return result;
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String n = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFKC);
        return n.replaceAll("\\s+", " ");
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private record Entry(UUID id, String searchable) {}
    private record Snapshot(List<Entry> entries, Map<String, BitSet> grams, Bloom bloom, Map<UUID, Integer> ordinals) {
        static Snapshot empty() { return new Snapshot(List.of(), Map.of(), new Bloom(BLOOM_BITS, BLOOM_HASHES), Map.of()); }
    }

    private static final class Bloom {
        private final BitSet bits;
        private final int size;
        private final int hashes;

        Bloom(int size, int hashes) { this.size = size; this.hashes = hashes; this.bits = new BitSet(size); }
        void add(String value) { for (int i = 0; i < hashes; i++) bits.set(index(value, i)); }
        boolean mightContain(String value) {
            for (int i = 0; i < hashes; i++) if (!bits.get(index(value, i))) return false;
            return true;
        }
        private int index(String value, int seed) {
            long h = 0xcbf29ce484222325L ^ (seed * 0x9e3779b97f4a7c15L);
            for (int i = 0; i < value.length(); i++) {
                h ^= value.charAt(i);
                h *= 0x100000001b3L;
            }
            long mixed = h ^ (h >>> 33);
            return (int) Math.floorMod(mixed, size);
        }
    }
}
