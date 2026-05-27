package com.zx.common.repository.util;

import com.zx.common.base.utils.ClassUtils;
import com.zx.common.base.utils.JsonUtils;
import com.zx.common.repository.constant.RepositoryConstants;
import com.zx.common.repository.exception.CommonRepositoryException;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 反射封装工具类
 *
 * @author : zhaoxu
 */
@Slf4j
public class ReflectUtil {
    /**
     * 生成全属性条件查询通用Specification
     *
     * @param objConditions   属性参数
     * @param clazz           要查询的实体类或vo类
     * @param excludeLikeAttr 不使用模糊搜索的字符串属性
     * @param <E>             泛型
     * @return Specification
     */
    @SuppressWarnings("unchecked")
    public static <E> Specification<E> createSpecification(Map<String, String> objConditions, Class<E> clazz, List<String> excludeLikeAttr) {
        Map<String, String> conditions = objConditions == null ? new HashMap<>(8) : objConditions;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            //未删除的数据
            if (getField(clazz, RepositoryConstants.VALID) != null) {
                if (!StringUtils.isEmpty(conditions.get(RepositoryConstants.VALID))) {
                    predicates.add(cb.equal(root.get(RepositoryConstants.VALID), Integer.valueOf(conditions.get(RepositoryConstants.VALID))));
                } else {
                    predicates.add(cb.equal(root.get(RepositoryConstants.VALID), 1));
                }
            }

            List<Field> declaredFields = ClassUtils.getClassFields(clazz, null);

            for (Field field : declaredFields) {
                String fieldName = field.getName();
                String condition = conditions.get(fieldName);
                if (!StringUtils.isEmpty(condition)) {
                    String typeName = field.getType().getName();
                    Class aClass;
                    try {
                        aClass = Class.forName(typeName);
                    } catch (ClassNotFoundException e) {
                        throw new CommonRepositoryException("未找到class!");
                    }
                    //属性不包含特定的属性并且是字符串采用模糊搜索
                    boolean isLike = aClass == String.class && (CollectionUtils.isEmpty(excludeLikeAttr) || !excludeLikeAttr.contains(fieldName));
                    if (isLike) {
                        // 转义下划线和百分号，防止被数据库当成通配符
                        String queryFieldName = "%" + condition
                                .replace("/", "\\/")
                                .replace("_", "\\_")
                                .replace("%", "\\%")
                                + "%";
                        // Hibernate 6 新增的 like 方法，第三个参数是 escape 字符
                        predicates.add(cb.like(root.get(fieldName), queryFieldName, '\\'));
                    } else {
                        Object[] array = Arrays.stream(condition.split(","))
                                .map(item -> JsonUtils.convertObject(item, aClass))
                                .toArray();
                        predicates.add(cb.and(root.get(fieldName).in(array)));
                    }
                }
            }

            //外键关联查询，新，可以省去map参数
            if (!CollectionUtils.isEmpty(conditions)) {
                for (Map.Entry<String, String> entry : conditions.entrySet()) {
                    if (entry.getKey().contains(".")) {
                        //递归解析真实的path
                        List<String> conditionList = Arrays.asList(entry.getValue().split(","));
                        predicates.add(cb.and(getRootPath(root, null, entry.getKey()).in(conditionList)));
                    }
                }
            }
            Predicate[] pre = new Predicate[predicates.size()];
            Predicate preAnd = cb.and(predicates.toArray(pre));
            return query.where(preAnd).getRestriction();
        };
    }

    /**
     * 指定条件查询
     *
     * @param attr      查询的字段
     * @param condition 条件
     * @param <E>       泛型
     * @return Specification
     */
    public static <E> Specification<E> createOneSpecification(String attr, String condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            //未删除的数据
            try {
                if (RepositoryConstants.VALID.equals(attr)) {
                    predicates.add(cb.equal(root.get(RepositoryConstants.VALID), condition));
                } else {
                    predicates.add(cb.equal(root.get(RepositoryConstants.VALID), 1));
                }
            } catch (Exception ignored) {
            }

            //外键关联查询
            if (attr.contains(RepositoryConstants.POINT)) {
                //递归解析真实的path
                predicates.add(cb.equal(getRootPath(root, null, attr), condition));
            } else {
                List<String> conditionList = Arrays.asList(condition.split(","));
                predicates.add(cb.and(root.get(attr).in(conditionList)));
            }

            Predicate[] pre = new Predicate[predicates.size()];
            Predicate preAnd = cb.and(predicates.toArray(pre));
            return query.where(preAnd).getRestriction();
        };
    }

    /**
     * 获取关联查询真实path
     *
     * @param root    root
     * @param path    path
     * @param allPath allPath
     * @param <E>     E
     * @return Path
     */
    public static <E> Path<E> getRootPath(Root<E> root, Path<E> path, String allPath) {
        List<String> pathList = Arrays.asList(allPath.split("\\."));
        //下一个解析的path
        StringBuilder restPath = new StringBuilder();
        Path<E> nowPath = null;
        if (!CollectionUtils.isEmpty(pathList)) {
            if (root != null) {
                nowPath = root.get(pathList.get(0));
                //拥有下一个解析点
                if (pathList.size() > 1) {
                    for (int i = 1; i < pathList.size(); i++) {
                        restPath.append(pathList.get(i));
                        if (i + 1 < pathList.size()) {
                            restPath.append(".");
                        }
                    }
                    //递归
                    nowPath = getRootPath(null, nowPath, restPath.toString());
                }
            } else {
                nowPath = path.get(pathList.get(0));
                //拥有下一个解析点
                if (pathList.size() > 1) {
                    for (int i = 1; i < pathList.size(); i++) {
                        restPath.append(pathList.get(i));
                        if (i + 1 < pathList.size()) {
                            restPath.append(".");
                        }
                    }
                    //递归
                    nowPath = getRootPath(root, nowPath, restPath.toString());
                }
            }
        }
        return nowPath;
    }

    /**
     * 通过方法名动态执行某个方法
     *
     * @param object     object
     * @param methodName 方法名
     * @param parameters 参数
     * @return Object
     * @throws InvocationTargetException InvocationTargetException
     * @throws IllegalAccessException    IllegalAccessException
     * @throws NoSuchMethodException     NoSuchMethodException
     */
    public static Object executeMethod(Object object, String methodName, Object... parameters) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Class<?> clazz = object.getClass();
        ArrayList<Class<?>> paramTypeList = new ArrayList<>();
        for (Object paramType : parameters) {
            paramTypeList.add(paramType.getClass());
        }
        Class<?>[] classArray = new Class[paramTypeList.size()];
        Method method = clazz.getMethod(methodName, paramTypeList.toArray(classArray));
        Object invoke = method.invoke(object, parameters);
        return invoke;
    }

    /**
     * 设置属性值
     *
     * @param property 设置的字段
     * @param value    值
     * @param object   object
     * @return Boolean
     */
    public static Boolean setValue(Object object, String property, Object value) throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        Field declaredField = getField(clazz, property);
        if (declaredField == null) {
            throw new NoSuchFieldException(property);
        }
        declaredField.setAccessible(true);
        declaredField.set(object, value);
        return true;
    }

    /**
     * 获取对象所有属性及对应的类别
     *
     * @param object object
     * @return Map
     * @throws IllegalAccessException IllegalAccessException
     */
    public static Map<String, Class<?>> getFields(Object object) throws IllegalAccessException {
        Class<?> clazz = object.getClass();
        Map<String, Class<?>> attrMap = new HashMap<>(16);
        if (clazz != null) {
            Iterator<String> iterator = getValues(object).keySet().iterator();

            while (iterator.hasNext()) {
                attrMap.put(iterator.next(), Object.class);
            }
        }
        return attrMap;
    }

    /**
     * 获取所有属性值
     *
     * @param object object
     * @return Map
     * @throws IllegalAccessException IllegalAccessException
     */
    public static Map<String, Object> getValues(Object object) throws IllegalAccessException {
        Map<String, Object> fieldValuesMap = new HashMap<>(16);
        Class<?> clazz = object.getClass();
        List<Field> fields = ClassUtils.getClassFields(clazz, null);
        for (Field field : fields) {
            field.setAccessible(true);
            Object fieldValue = field.get(object);
            fieldValuesMap.put(field.getName(), fieldValue);
        }
        return fieldValuesMap;
    }

    /**
     * 获取所有属性值，自动转为字符串
     *
     * @param object object
     * @return Map
     * @throws IllegalAccessException IllegalAccessException
     */
    public static Map<String, String> getStringValues(Object object) throws IllegalAccessException {
        if (object == null) {
            return new HashMap<>(8);
        }
        Map<String, String> fieldValuesMap = new HashMap(8);
        Class<?> clazz = object.getClass();
        if (clazz != null) {
            List<Field> fields = ClassUtils.getClassFields(clazz, null);
            for (Field field : fields) {
                field.setAccessible(true);
                Object fieldValue = field.get(object);
                fieldValuesMap.put(field.getName(), fieldValue == null ? null : fieldValue.toString());
            }
            return fieldValuesMap;
        }
        return fieldValuesMap;
    }

    /**
     * 获取拥有指定注解的字段
     *
     * @param objectClass 对象
     * @param annoClass   查询的注解
     * @return List
     */
    public static List<Field> getTargetAnnoation(Class<?> objectClass, Class<? extends Annotation> annoClass) {
        List<Field> fields = new ArrayList<>();
        List<Field> declaredFields = ClassUtils.getClassFields(objectClass, null);

        for (Field field : declaredFields) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(annoClass)) {
                continue;
            } else {
                fields.add(field);
            }
        }

        if (!CollectionUtils.isEmpty(fields)) {
            return fields;
        } else {
            return null;
        }
    }

    private static Field getField(Class<?> clazz, String fieldName) {
        return ClassUtils.getClassFields(clazz, null).stream()
                .filter(field -> field.getName().equals(fieldName))
                .findFirst()
                .orElse(null);
    }
}
