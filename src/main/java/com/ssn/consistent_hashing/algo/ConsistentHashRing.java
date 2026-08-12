package com.ssn.consistent_hashing.algo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class ConsistentHashRing<T> {
    private final int numberOfReplicas;
    private final SortedMap<Long, T> circle = new ConcurrentSkipListMap<>();

    public ConsistentHashRing(int numberOfReplicas, Collection<T> nodes) {
        this.numberOfReplicas = numberOfReplicas;
        for (T node : nodes) {
            add(node);
        }
    }

    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hash(node.toString() + "-VN" + i), node);
        }
    }

    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(hash(node.toString() + "-VN" + i));
        }
    }

    public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = hash(key.toString());
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            long h = 0;
            for (int i = 0; i < 4; i++) {
                h = (h << 8) | (digest[i] & 0xFF);
            }
            return h & 0xffffffffL;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    public Map<Long, T> getCircle() {
        return Collections.unmodifiableMap(circle);
    }
}