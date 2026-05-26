package com.zx.common.base.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author ZhaoXu
 * @date 2024/4/30 14:24
 */
public class ZipUtils {
    /**
     * 压缩多个数据条目
     * @param dataMap 包含文件名与对应数据的映射
     * @return 压缩后的数据
     * @throws IOException 如果压缩过程出错
     */
    public static byte[] zipMultipleFiles(Map<String, byte[]> dataMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> entry : dataMap.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    /**
     * 解压数据，假设ZIP文件中可能包含多个文件
     * @param zipData 待解压的ZIP数据
     * @return 包含文件名与文件数据的映射
     * @throws IOException 如果解压过程出错
     */
    public static Map<byte[], byte[]> unzipMultipleFiles(byte[] zipData) throws IOException {
        Map<byte[], byte[]> files = new HashMap<>(8);
        ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
        try (ZipInputStream zis = new ZipInputStream(bais)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                files.put(fileName.getBytes(StandardCharsets.UTF_8), baos.toByteArray());
                zis.closeEntry();
            }
        }
        return files;
    }
}
