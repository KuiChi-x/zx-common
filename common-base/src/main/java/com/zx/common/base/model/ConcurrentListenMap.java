package com.zx.common.base.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author ZhaoXu
 * @date 2023/10/19 22:32
 */
public class ConcurrentListenMap<K, V> extends ConcurrentSkipListMap<K, V> {
    private static final long serialVersionUID = 5604359564082262732L;

    List<Consumer<K>> putListeners = new ArrayList<>();

    List<Consumer<K>> removeListeners = new ArrayList<>();

    public void listenPut(Consumer<K> consumer) {
        putListeners.add(consumer);
    }

    public void listenRemove(Consumer<K> consumer) {
        removeListeners.add(consumer);
    }

    @Override
    public V put(K k, V v) {
        V put = super.put(k, v);
        for (Consumer<K> putListener : putListeners) {
            putListener.accept(k);
        }
        return put;
    }

    @Override
    public V remove(Object k) {
        V remove = super.remove(k);
        for (Consumer<K> removeListener : removeListeners) {
            removeListener.accept((K) k);
        }
        return remove;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V v = super.computeIfAbsent(key, mappingFunction);
        if (Objects.equals(v, get(key))) {
            for (Consumer<K> putListener : putListeners) {
                putListener.accept(key);
            }
        }
        return v;
    }
}
