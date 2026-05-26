//package com.zx.common.base.utils;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.jar.JarInputStream;
//import java.util.jar.JarOutputStream;
//import java.util.jar.Pack200;
//import java.util.zip.GZIPInputStream;
//import java.util.zip.GZIPOutputStream;
//
///**
// * @author ZhaoXu
// * @date 2024/2/27 16:38
// */
//public class Pack200Utils {
//    /**
//     * 压缩
//     * @param inputBytes
//     * @return
//     */
//    public static byte[] compress(byte[] inputBytes) {
//        // 创建Pack200对象
//        Pack200.Packer packer = Pack200.newPacker();
//
//        // 使用默认压缩选项
//        packer.properties().put(Pack200.Packer.EFFORT, "5");
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputBytes);
//        ByteArrayOutputStream compressedByteArrayOutputStream = new ByteArrayOutputStream();
//
//        // 创建一个GZIPOutputStream以将数据保存为.gz文件
//        try {
//            try (OutputStream gzipOutputStream = new GZIPOutputStream(compressedByteArrayOutputStream)) {
//                // 将Pack200数据写入GZIPOutputStream
//                packer.pack(new JarInputStream(byteArrayInputStream), gzipOutputStream);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        return compressedByteArrayOutputStream.toByteArray();
//    }
//
//    /**
//     * 解压pack200，使用流优化占用内存
//     * @param inputStream
//     * @param outputFile
//     */
//    public static void unpackFile(InputStream inputStream, String outputFile) {
//        try (InputStream uncompressedInputStream = new GZIPInputStream(inputStream);
//             JarOutputStream jos = new JarOutputStream(Files.newOutputStream(Paths.get(outputFile)))) {
//            Pack200.Unpacker unpacker = Pack200.newUnpacker();
//            unpacker.unpack(uncompressedInputStream, jos);
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
