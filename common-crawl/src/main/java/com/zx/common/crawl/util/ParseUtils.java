package com.zx.common.crawl.util;

import com.zx.common.crawl.annotation.FieldSelect;
import com.zx.common.crawl.annotation.RootSelect;
import com.zx.common.crawl.enums.SelectTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author ZhaoXu
 * @date 2024/9/25 20:38
 */
@Slf4j
public class ParseUtils {
    /**
     * 解析document并映射到实体对象当中
     *
     * @param document
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T parseAndMapping(Document document, Class<T> clazz) {
        RootSelect rootSelect = clazz.getAnnotation(RootSelect.class);
        String rootCssQuery = (rootSelect == null || ObjectUtils.isEmpty(rootSelect.cssQuery())) ? "body" : rootSelect.cssQuery();
        Elements rootElements = document.select(rootCssQuery);
        T result = null;
        try {
            result = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        for (Element rootElement : rootElements) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                FieldSelect fieldSelect = declaredField.getAnnotation(FieldSelect.class);
                if (Objects.isNull(fieldSelect)) {
                    continue;
                }
                String fieldCssQuery = fieldSelect.cssQuery();
                if (ObjectUtils.isEmpty(fieldCssQuery)) {
                    continue;
                }
                Elements fieldElements = rootElement.select(fieldCssQuery);
                SelectTypeEnum selectTypeEnum = fieldSelect.selectType();
                String selectAttr = fieldSelect.selectAttr();
                Type genericType = declaredField.getGenericType();
                Object fieldValue = null;
                if (genericType instanceof ParameterizedType) {
                    // list类型属性
                    if (List.class.equals(((ParameterizedType) genericType).getRawType())) {
                        fieldValue = fieldElements.stream()
                                .map(fieldElement -> {
                                    String fieldString = getFieldString(fieldElement, selectTypeEnum, selectAttr);
                                    return convertFieldType(declaredField, fieldString);
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                    }
                } else {
                    if (!fieldElements.isEmpty()) {
                        Element fieldElement = fieldElements.get(0);
                        String fieldString = getFieldString(fieldElement, selectTypeEnum, selectAttr);
                        fieldValue = convertFieldType(declaredField, fieldString);
                    }
                }
                declaredField.setAccessible(Boolean.TRUE);
                try {
                    declaredField.set(result, fieldValue);
                } catch (IllegalAccessException e) {
                    log.warn("内容映射警告，class:{}, fieldName:{}, value:{}", clazz, declaredField.getName(), fieldValue);
                }
            }
        }
        return result;
    }

    /**
     * 获取页面element属性值，返回字符串
     *
     * @param fieldElement
     * @param selectType
     * @param selectAttr
     * @return
     */
    public static String getFieldString(Element fieldElement, SelectTypeEnum selectType, String selectAttr) {
        String fieldElementOrigin = null;
        if (Objects.equals(SelectTypeEnum.VAL, selectType)) {
            fieldElementOrigin = fieldElement.val();
        } else if (Objects.equals(SelectTypeEnum.TEXT, selectType)) {
            fieldElementOrigin = fieldElement.text();
        } else if (Objects.equals(SelectTypeEnum.ATTR, selectType)) {
            fieldElementOrigin = fieldElement.attr(selectAttr);
        } else {
            fieldElementOrigin = fieldElement.toString();
        }
        return fieldElementOrigin;
    }

    /**
     * 参数转换 （支持：Byte、Boolean、String、Short、Integer、Long、Float、Double、Date）
     *
     * @param field
     * @param value
     * @return Object
     */
    public static Object convertFieldType(Field field, String value) {
        Class<?> fieldType = field.getType();

        if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType fieldGenericType = (ParameterizedType) field.getGenericType();
            if (fieldGenericType.getRawType().equals(List.class)) {
                Type type = fieldGenericType.getActualTypeArguments()[0];
                fieldType = (Class<?>) type;
            }
        }
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        value = value.trim();
        if (Byte.class.equals(fieldType) || Byte.TYPE.equals(fieldType)) {
            return Byte.valueOf(value);
        } else if (Boolean.class.equals(fieldType) || Boolean.TYPE.equals(fieldType)) {
            return Boolean.valueOf(value);
        } else if (String.class.equals(fieldType)) {
            return value;
        } else if (Short.class.equals(fieldType) || Short.TYPE.equals(fieldType)) {
            return Short.valueOf(value);
        } else if (Integer.class.equals(fieldType) || Integer.TYPE.equals(fieldType)) {
            return Integer.valueOf(value);
        } else if (Long.class.equals(fieldType) || Long.TYPE.equals(fieldType)) {
            return Long.valueOf(value);
        } else if (Float.class.equals(fieldType) || Float.TYPE.equals(fieldType)) {
            return Float.valueOf(value);
        } else if (Double.class.equals(fieldType) || Double.TYPE.equals(fieldType)) {
            return Double.valueOf(value);
        } else {
            throw new RuntimeException("类型解析错误, type=" + fieldType);
        }
    }
}
