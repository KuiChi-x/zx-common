package com.zx.common.base.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;

/**
 * @author ZhaoXu
 */
@Slf4j
public class JsonUtils {
    @Getter
    private static ObjectMapper mapper = null;

    static {
        init();
    }

    private static void init() {
        mapper = new ObjectMapper();
        initMapper(mapper);
    }

    @SuppressWarnings("deprecation")
    private static void initMapper(ObjectMapper mapper) {
        // 反序列化的时候如果多了其他属性,不抛出异常
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许使用单引号 ' 包围的字符串作为 JSON 字符串的引号方式
        mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
        // 允许在 JSON 对象或数组中省略值
        mapper.configure(Feature.ALLOW_MISSING_VALUES, true);
        // 允许在 JSON 字符串中包含非转义的控制字符
        mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        // 允许将单个值解析为数组
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        // 允许在 JSON 对象中使用非引号包围的字段名
        mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 许在数字中包含前导零
        mapper.configure(Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        // 允许在 JSON 中包含非数字的数字值
        mapper.configure(Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        // 允许在 JSON 字符串中使用反斜杠 \ 转义任何字符。
        mapper.configure(Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        // 允许在 JSON 中包含注释
        mapper.configure(Feature.ALLOW_COMMENTS, true);
        // 允许空对象
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        //关闭序列化的时候没有为属性找到getter方法,报错
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        //关闭反序列化的时候，没有找到属性的setter报错
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //序列化的时候序列对象的所有属性
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // ignore Timestamp field
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);
        mapper.registerModule(new JavaTimeModule());
    }

    public static String toJson(Object entity) {
        try {
            return mapper.writeValueAsString(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(String text, Class<T> clazz) {
        try {
            return mapper.readValue(text, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(String json, JavaType valueType) {
        try {
            return mapper.readValue(json, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static JsonNode readTree(String content) {
        try {
            return mapper.readTree(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode toJsonNode(Object object) {
        try {
            return mapper.valueToTree(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaType constructType(Type type) {
        return mapper.constructType(type);
    }

    public static JavaType constructSimpleType(Class<?> rawClass, Type type) {
        JavaType javaType = mapper.constructType(type);
        return mapper.getTypeFactory().constructSimpleType(rawClass, new JavaType[]{javaType});
    }

    public static <T> T convertObject(Object source, JavaType javaType) {
        return mapper.convertValue(source, javaType);
    }

    public static <T> T convertObject(Object source, Class<T> tClass) {
        return mapper.convertValue(source, tClass);
    }

    public static <T> T convertObject(Object source, TypeReference<T> typeReference) {
        return mapper.convertValue(source, typeReference);
    }
}
