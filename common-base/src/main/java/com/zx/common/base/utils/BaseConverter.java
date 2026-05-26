package com.zx.common.base.utils;

import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ZhaoXu
 * @date 2022/6/15 15:30
 */
public class BaseConverter {
    public static <S, T> T convert(S source, Class<T> targetClass) {
        T t = null;
        try {
            t = targetClass.newInstance();
            if (Objects.nonNull(source)) {
                BeanUtils.copyProperties(source, t);
            }
            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <S, T> List<T> convertList(List<S> source, Class<T> targetClass) {
        return Optional.ofNullable(source).orElse(Collections.emptyList())
                .stream()
                .map(item -> convert(item, targetClass))
                .collect(Collectors.toList());
    }
}
