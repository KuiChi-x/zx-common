package com.zx.common.base.utils;

import com.zx.common.base.exception.RequestException;
import com.zx.common.base.model.BaseResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author ZhaoXu
 * @date 2023/10/30 16:28
 */
@Slf4j
public class BaseResponseUtils {
    /**
     * 解析 code、message、data结构中的data数据并进行映射
     * @param content
     * @param typeReference
     * @return
     * @param <T>
     */
    public static <T> T parseData(String content, TypeReference<T> typeReference) {
        if (Objects.isNull(content)) {
            return null;
        }
        JavaType responseType = JsonUtils.constructSimpleType(BaseResponse.class, typeReference.getType());
        BaseResponse<T> response = JsonUtils.fromJson(content, responseType);
        if (response.isSuccess()) {
            return response.getData();
        } else {
            throw new RequestException(response.getMessage());
        }
    }
}
