package com.zx.common.base.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author liukun
 * @description
 * @date 2025-08-04
 */
public class ResourceConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(ResourceConfig.class);

    public static Map<String, String> loadConfig(String name) {
        Properties properties = loadConfigToProperties(name);
        final Map<String, String> map = new HashMap<>();
        for (final String key : properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }
        return map;
    }

    public static Properties loadConfigToProperties(String name) {
        List<InputStream> inputStreams = ResourceLoader.loadResource(name);
        if (inputStreams.size() == 0) {
            throw new RuntimeException("can not find configuration file: " + name);
        }

        Properties properties = new Properties();
        for (InputStream inputStream : inputStreams) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                LOGGER.warn("can not read this config file, file:{}", name);
            }
        }
        return properties;
    }
}
