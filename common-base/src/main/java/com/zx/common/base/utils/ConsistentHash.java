package com.zx.common.base.utils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 无外部依赖的一致性哈希实现，使用 MurmurHash2 (64-bit)
 *
 * @author ZhaoXu
 * @date 2025/1/22 17:42
 */
public class ConsistentHash<T> {
    private final SortedMap<Long, T> ring = new TreeMap<>();
    private final int virtualNodes;

    public ConsistentHash(int virtualNodes, List<T> nodes) {
        this.virtualNodes = virtualNodes;
        for (T node : nodes) {
            addNode(node);
        }
    }

    public void addNode(T node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(node.toString() + "#" + i);
            ring.put(hash, node);
        }
    }

    public void removeNode(T node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(node.toString() + "#" + i);
            ring.remove(hash);
        }
    }

    public T getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        long hash = hash(key);
        SortedMap<Long, T> tailMap = ring.tailMap(hash);
        if (tailMap.isEmpty()) {
            return ring.get(ring.firstKey());
        }
        return tailMap.get(tailMap.firstKey());
    }

    public Set<T> getNodes() {
        return new HashSet<>(ring.values());
    }

    /**
     * MurmurHash2 64-bit
     */
    private static long hash(String key) {
        byte[] data = key.getBytes(StandardCharsets.UTF_8);
        long seed = 0x1234ABCDL;
        long m = 0xc6a4a7935bd1e995L;
        int r = 47;
        long h = seed ^ ((long) data.length * m);

        int length8 = data.length / 8;
        for (int i = 0; i < length8; i++) {
            int i8 = i * 8;
            long k = ((long) data[i8] & 0xff)
                    | (((long) data[i8 + 1] & 0xff) << 8)
                    | (((long) data[i8 + 2] & 0xff) << 16)
                    | (((long) data[i8 + 3] & 0xff) << 24)
                    | (((long) data[i8 + 4] & 0xff) << 32)
                    | (((long) data[i8 + 5] & 0xff) << 40)
                    | (((long) data[i8 + 6] & 0xff) << 48)
                    | (((long) data[i8 + 7] & 0xff) << 56);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h ^= k;
            h *= m;
        }

        int remaining = data.length % 8;
        if (remaining > 0) {
            int offset = length8 * 8;
            switch (remaining) {
                case 7: h ^= ((long) data[offset + 6] & 0xff) << 48;
                case 6: h ^= ((long) data[offset + 5] & 0xff) << 40;
                case 5: h ^= ((long) data[offset + 4] & 0xff) << 32;
                case 4: h ^= ((long) data[offset + 3] & 0xff) << 24;
                case 3: h ^= ((long) data[offset + 2] & 0xff) << 16;
                case 2: h ^= ((long) data[offset + 1] & 0xff) << 8;
                case 1: h ^= ((long) data[offset] & 0xff);
                         h *= m;
            }
        }

        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;
        return h;
    }
}
