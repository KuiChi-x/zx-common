package com.zx.common.base.aspect;

import com.zx.common.base.annotation.LogAround;
import com.zx.common.base.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ZhaoXu
 * @date 2022/6/4 16:13
 */
@Component
@Aspect
@Slf4j
public class LogMethodAspect {

    @Around("@annotation(logAround)")
    public Object doAround(ProceedingJoinPoint joinPoint, LogAround logAround) throws Throwable {
        Signature signature = joinPoint.getSignature();
        Map<String, Object> paramNameAndValue = getParamNameAndValue(joinPoint);

        // 类名
        String fullClassName = signature.getDeclaringTypeName();

        // 方法名
        String methodName = signature.getName();
        String fullPath = fullClassName + "#" + methodName + " ";

        Object proceed = null;
        long startTime = System.currentTimeMillis();
        try {
            proceed = joinPoint.proceed();
            // com.xxx.TestServiceImpl#testMethod execute success, params:{}, response:{}
            log.info(fullPath + "execute success, params:{}, response:{}", JsonUtils.toJson(paramNameAndValue),
                    JsonUtils.toJson(proceed));
            log.info(fullPath + "execute time:{}", (System.currentTimeMillis() - startTime) + " millisecond");
        } catch (Throwable throwable) {
            // com.xxx.TestServiceImpl#testMethod execute error, params:{}
            log.error(fullPath + "execute error, params:{}", JsonUtils.toJson(paramNameAndValue), throwable);
            throw throwable;
        }
        return proceed;
    }

    /**
     * 获取参数Map集合
     *
     * @param joinPoint
     * @return
     */
    private Map<String, Object> getParamNameAndValue(ProceedingJoinPoint joinPoint) {
        Map<String, Object> param = new HashMap<>(8);
        Object[] paramValues = joinPoint.getArgs();
        String[] paramNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();

        for (int i = 0; i < paramNames.length; i++) {
            param.put(paramNames[i], paramValues[i]);
        }
        return param;
    }

}
