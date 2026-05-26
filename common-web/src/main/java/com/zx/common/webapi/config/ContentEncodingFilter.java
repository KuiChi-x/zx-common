package com.zx.common.webapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * @author ZhaoXu
 * @date 2023/12/25 14:22
 */
@Component
public class ContentEncodingFilter extends OncePerRequestFilter {
    private static final String GZIP_COMPRESS_REQUEST_STR = "gzip";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        // 自动解析content-encoding
        String contentEncoding = request.getHeader(HttpHeaders.CONTENT_ENCODING);
        if (contentEncoding != null && contentEncoding.toLowerCase().contains(GZIP_COMPRESS_REQUEST_STR)) {
            HttpServletRequest delegatingRequest = new HttpServletRequestWrapper(request) {
                @Override
                public ServletInputStream getInputStream() throws IOException {
                    return new DelegatingServletInputStream(new GZIPInputStream(request.getInputStream(), 1024 * 8));
                }
            };

            chain.doFilter(delegatingRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    static class DelegatingServletInputStream extends ServletInputStream {
        private final InputStream sourceStream;
        private boolean finished = false;

        public DelegatingServletInputStream(InputStream sourceStream) {
            Assert.notNull(sourceStream, "Source InputStream must not be null");
            this.sourceStream = sourceStream;
        }

        @Override
        public int read() throws IOException {
            int data = this.sourceStream.read();
            if (data == -1) {
                this.finished = true;
            }

            return data;
        }

        @Override
        public int available() throws IOException {
            return this.sourceStream.available();
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.sourceStream.close();
        }

        @Override
        public boolean isFinished() {
            return this.finished;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }
    }
}