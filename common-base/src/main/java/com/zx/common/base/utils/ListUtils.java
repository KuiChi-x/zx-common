package com.zx.common.base.utils;

import com.zx.common.base.model.PageVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author: zhaoxu
 * @description:
 */
public class ListUtils {
    /**
     * 列表去重
     * @param list
     * @return
     * @param <T>
     */
    public static <T> List<T> distinct(List<T> list) {
        return Optional.ofNullable(list)
                .orElse(Collections.emptyList())
                .stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 分页
     *
     * @param data     数据
     * @param current  页数
     * @param pageSize 每页条数
     * @return map
     */
    public static <E> PageVO<E> toPage(List<E> data, int current, int pageSize) {
        PageVO<E> pageVO = new PageVO<>();
        data = Optional.ofNullable(data).orElse(Collections.emptyList());
        pageVO.setTotal((long) data.size());

        List<E> pageList = data.stream()
                .skip((long) (current - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        //分页需要的参数
        pageVO.setList(pageList);
        return pageVO;
    }

    /**
     * 查找列表2中列表1没有的项
     *
     * @param list1 列表1 1,2,3,4
     * @param list2 列表2 2,3,4,5
     * @param <E>
     * @return 差异列表 5
     */
    public static <E> List<E> getListDiff(List<E> list1, List<E> list2) {
        if (isEmpty(list2)) {
            return new ArrayList<>();
        }
        if (isEmpty(list1)) {
            return list2;
        }

        // 先把列表2全部加进来
        Set<E> diffSet = new HashSet<>(list2);

        //调用方法保留共有元素
        Set<E> setOfCommonElements = new HashSet<>(list2);
        setOfCommonElements.retainAll(list1);

        //移除共有的
        diffSet.removeAll(setOfCommonElements);

        return new ArrayList<>(diffSet);
    }

    /**
     * 判断列表为空
     *
     * @param list
     * @param <E>
     * @return
     */
    public static <E> Boolean isEmpty(List<E> list) {
        return list == null || list.isEmpty();
    }

    /**
     * 判断列表不为空
     *
     * @param list
     * @param <E>
     * @return
     */
    public static <E> Boolean isNotEmpty(List<E> list) {
        return !isEmpty(list);
    }

    /**
     * 分片
     * @param list
     * @param size
     * @return
     * @param <T>
     */
    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }

        int listSize = list.size();
        int partitions = (int) Math.ceil((double) listSize / size);

        List<List<T>> result = new ArrayList<>(partitions);

        for (int i = 0; i < listSize; i += size) {
            int end = Math.min(i + size, listSize);
            result.add(new ArrayList<>(list.subList(i, end)));
        }

        return result;
    }

    /**
     * 从集合中随机选择一个元素
     * @param collection
     * @return
     * @param <T>
     */
    public static <T> T getRandomElement(Collection<T> collection) {
        if (ObjectsUtils.isEmpty(collection)) {
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt(collection.size());

        int count = 0;
        for (T element : collection) {
            if (count == index) {
                return element;
            }
            count++;
        }

        return null;
    }

}

