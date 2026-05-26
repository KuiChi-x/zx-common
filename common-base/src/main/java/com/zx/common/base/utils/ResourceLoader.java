package com.zx.common.base.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author liukun
 * @description 加载资源
 * @date 2025-08-04
 */
public class ResourceLoader {
    public static List<InputStream> loadResource(String name) {
        if (StringUtils.isBlank(name)) {
            throw new RuntimeException("config name is empty");
        }

        List<InputStream> list = new ArrayList<>();
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(name);
            if (!resources.hasMoreElements()) {
                resources = ResourceLoader.class.getClassLoader().getResources(name);
            }

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                URLConnection urlConnection = url.openConnection();
                try {
                    list.add(urlConnection.getInputStream());
                } catch (IOException ex) {
                    // Close the HTTP connection (if applicable).
                    if (urlConnection instanceof HttpURLConnection) {
                        ((HttpURLConnection) urlConnection).disconnect();
                    }
                    throw ex;
                }
            }
        } catch (Exception e) {
            // do nothing
        }

        return list;
    }
}
