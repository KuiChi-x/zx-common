package com.zx.common.rpc.parameter;

import com.zx.common.rpc.dto.RequestClientDTO;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ZhaoXu
 * @date 2023/11/8 13:22
 */
public class ParameterParser {

    @SuppressWarnings("unchecked")
    public static void setParameters(Method method, Object[] args, RequestClientDTO requestClientDTO) {
        String path = requestClientDTO.getPath();
        StringBuilder pathBuilder = new StringBuilder((path.endsWith("?") ? path : (path + "?")));
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                if (Map.class.isAssignableFrom(parameter.getType())) {
                    ((Map<String, Object>) args[i]).forEach((k, v) -> {
                        pathBuilder.append(k).append("=").append(urlEncode(v)).append("&");
                    });
                } else if (List.class.isAssignableFrom(parameter.getType())) {
                    if (ObjectUtils.isNotEmpty(args[i])) {
                        List<String> listParam = ((List<?>) args[i]).stream().map(String::valueOf).collect(Collectors.toList());
                        String collect = String.join(",", listParam);
                        pathBuilder.append(requestParam.value()).append("=").append(urlEncode(collect)).append("&");
                    }
                } else {
                    pathBuilder.append(requestParam.value()).append("=").append(urlEncode(args[i])).append("&");
                }
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                String paramName = ObjectUtils.isNotEmpty(pathVariable.value()) ? pathVariable.value() : parameter.getName();
                String replaceStr = "{" + paramName + "}";
                int replaceStart = pathBuilder.indexOf(replaceStr);
                int replaceEnd = replaceStart + replaceStr.length();
                pathBuilder.replace(replaceStart, replaceEnd, urlEncode(args[i]));
            } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                requestClientDTO.setRequestBody(args[i]);
            } else if (parameter.getType().isAssignableFrom(MultipartFile.class)) {
                requestClientDTO.setRequestMethod(RequestMethod.POST);
            }
        }
        requestClientDTO.setPath(pathBuilder.toString());
    }

    private static String urlEncode(Object text) {
        try {
            text = URLEncoder.encode(Optional.ofNullable(text).map(String::valueOf).orElse(""), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            text = "";
        }
        return (String) text;
    }
}
