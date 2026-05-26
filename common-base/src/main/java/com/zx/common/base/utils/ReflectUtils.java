package com.zx.common.base.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * @author ZhaoXu
 * @date 2022/9/22 22:12
 */
@SuppressWarnings("unchecked")
public class ReflectUtils {
    public static <T> T getAnnotationFieldValue(Annotation annotation, String fieldName) {
        Map<String, Object> annotationMemberMap = getAnnotationMemberMap(annotation);
        return (T) annotationMemberMap.get(fieldName);
    }

    public static void setAnnotationFieldValue(Annotation annotation, String fieldName, Object value) {
        Map<String, Object> annotationMemberMap = getAnnotationMemberMap(annotation);
        annotationMemberMap.put(fieldName, value);
    }

    private static Map<String, Object> getAnnotationMemberMap(Annotation annotation) {
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(annotation);
        try {
            Field memberValues = invocationHandler.getClass().getDeclaredField("memberValues");
            memberValues.setAccessible(Boolean.TRUE);
            return (Map<String, Object>) memberValues.get(invocationHandler);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
