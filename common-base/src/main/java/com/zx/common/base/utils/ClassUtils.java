package com.zx.common.base.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author ZhaoXu
 * @date 2023/10/18 17:12
 */
@Slf4j
public class ClassUtils {
    /**
     * 通过方法名、参数名动态执行某个方法
     *
     * @param object
     * @param methodName
     * @param parameters
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     */
    public static Object executeMethod(Object object, String methodName, Object... parameters) throws InvocationTargetException, IllegalAccessException {
        if (!ObjectUtils.allNotNull(object, methodName, parameters)) {
            return null;
        }
        Class<?> clazz = object.getClass();
        Class<?>[] parameterClasses = Arrays.stream(Optional.ofNullable(parameters).orElse(new Object[0])).map(Object::getClass).toArray(Class[]::new);
        Method declaredMethod = getDeclaredMethod(clazz, methodName, parameterClasses);
        if (Objects.nonNull(declaredMethod)) {
            declaredMethod.setAccessible(Boolean.TRUE);
            return declaredMethod.invoke(object, parameters);
        }
        return null;
    }

    /**
     * 获取方法
     *
     * @param methodName
     * @param parameterClasses
     * @param clazz
     */
    public static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterClasses) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterClasses);
        } catch (NoSuchMethodException e) {
            // 找不到尝试遍历方法，能够类型转换就认为能够执行
            Method[] declaredMethods = clazz.getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                String name = declaredMethod.getName();
                if (Objects.equals(name, methodName) && declaredMethod.getParameterCount() == parameterClasses.length) {
                    Class<?>[] methodParameterTypes = declaredMethod.getParameterTypes();
                    boolean allMatch = true;
                    for (int i = 0; i < parameterClasses.length; i++) {
                        if (!methodParameterTypes[i].isAssignableFrom(parameterClasses[i])) {
                            allMatch = false;
                        }
                    }
                    if (allMatch) {
                        return declaredMethod;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取类的所有字段
     *
     * @param clazz
     * @return
     */
    public static List<Field> getClassFields(Class<?> clazz, List<Field> fieldList) {
        if (Objects.isNull(fieldList)) {
            fieldList = new ArrayList<>();
        }
        Field[] declaredFields = clazz.getDeclaredFields();
        if (ObjectUtils.isNotEmpty(declaredFields)) {
            fieldList.addAll(Arrays.asList(declaredFields));
        }
        Class<?> superclass = clazz.getSuperclass();
        if (ObjectUtils.isNotEmpty(superclass)) {
            getClassFields(superclass, fieldList);
        }
        return fieldList;
    }

    /**
     * 把所有String字段转为小写
     * @param object
     */
    public static void convertStringFieldToLower(Object object) {
        if (Objects.isNull(object)) {
            return;
        }
        Class<?> aClass = object.getClass();
        Field[] declaredFields = aClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            Class<?> type = declaredField.getType();
            if (type.isAssignableFrom(String.class)) {
                try {
                    declaredField.setAccessible(Boolean.TRUE);
                    Object value = declaredField.get(object);
                    if (Objects.nonNull(value)) {
                        declaredField.set(object, value.toString().toLowerCase());
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * 加载类
     * @param path
     */
    public static void loadClasses(String path) {
        // 获取目录下的所有JAR文件
        File directory = new File(path);
        File[] jars = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (ObjectUtils.isNotEmpty(jars)) {
            for (File jar : jars) {
                try {
                    // 创建JAR文件对象
                    JarFile jarFile = new JarFile(jar.getPath());

                    // 获取JAR文件中的所有条目（即类文件）
                    jarFile.stream().forEach(entry -> {
                        try {
                            // 获取条目名称
                            String entryName = entry.getName();

                            if (entryName.endsWith(".class")) {
                                // 转换类文件路径为类名，并替换 '/' 为 '.'
                                String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');

                                // 创建ClassLoader
                                URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, Thread.currentThread().getContextClassLoader());
                                classLoader.loadClass(className);

                                // 关闭ClassLoader，释放资源
                                classLoader.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                    // 关闭JAR文件，释放资源
                    jarFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 根据包路径扫描类
     *
     * @param packageName
     * @return
     */
    public static Set<Class<?>> scanClass(String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        packageName = packageName.trim();
        if (ObjectUtils.isEmpty(packageName)) {
            return classes;
        }
        String packagePath = packageName.replace(".", "/");
        Enumeration<URL> resources = null;
        try {
            resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());) {
                String protocol = url.getProtocol();
                if ("jar".equals(protocol)) {
                    JarURLConnection jarUrlConnection = (JarURLConnection) url.openConnection();
                    JarFile jarFile = jarUrlConnection.getJarFile();
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntry = entries.nextElement();
                        String name = jarEntry.getName();
                        int index = name.indexOf(packagePath);
                        if (index != -1 && name.endsWith(".class")) {
                            String replace = name.substring(index, name.length() - 6).replace("/", ".");
                            try {
                                Class<?> clazz = urlClassLoader.loadClass(replace);
                                classes.add(clazz);
                            } catch (Throwable e) {
                            }
                        }
                    }
                } else if ("file".endsWith(protocol)) {
                    String path = url.getPath();
                    addClasses(path, classes, packageName);
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        log.info("扫描完毕，包：" + packageName + "下一共有:" + classes.size() + "个类");
        return classes;
    }

    private static void addClasses(String path, Set<Class<?>> classes, String packageName) throws ClassNotFoundException {
        File[] files = new File(path).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (file.isFile() && file.getName().endsWith(".class")) || file.isDirectory();
            }
        });

        if (ObjectUtils.isNotEmpty(files)) {
            for (File file : files) {
                String fileName = file.getName();
                if (file.isFile()) {
                    String className = fileName.substring(0, fileName.lastIndexOf("."));
                    String fullClassName = packageName + "." + className;
                    try {
                        Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(fullClassName);
                        classes.add(clazz);
                    } catch (Throwable e) {
                    }
                } else {
                    String subPackagePath = path + "/" + fileName;
                    String subPackageName = packageName + "." + fileName;
                    addClasses(subPackagePath, classes, subPackageName);
                }
            }
        }
    }
}
