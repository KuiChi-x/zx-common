package com.zx.common.base.utils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author ZhaoXu
 * @date 2023/10/16 14:49
 */
public class AopHelper {
    private static final Logger log = LoggerFactory.getLogger(AopHelper.class);
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    public static String parseAnnotationText(JoinPoint joinPoint, String text) throws NoSuchMethodException {
        Assert.hasLength(text, "text cannot be empty");
        if (SpelUtils.isRawExpression(text)) {
            return text;
        } else {
            Method targetMethod = getTargetMethod(joinPoint);
            EvaluationContext context = new MethodBasedEvaluationContext(joinPoint.getTarget(), targetMethod, joinPoint.getArgs(), NAME_DISCOVERER);
            return String.valueOf(SpelUtils.parseExpression(text, context));
        }
    }

    public static <T extends Annotation> T getClassAnnotation(JoinPoint joinPoint, Class<T> annotation) {
        try {
            Class<?> targetClass = getTargetClass(joinPoint.getTarget());
            return targetClass.getAnnotation(annotation);
        } catch (Exception e) {
            log.error("Fail to find annotation={}, joinPoint={}, message={}", annotation.getSimpleName(), joinPoint, e.getMessage());
            return null;
        }
    }

    public static <T extends Annotation> T getMethodAnnotation(JoinPoint joinPoint, Class<T> annotation) {
        try {
            Method method = getTargetMethod(joinPoint);
            return method.getAnnotation(annotation);
        } catch (Exception e) {
            log.error("Fail to find annotation={}, joinPoint={}, message={}", annotation.getSimpleName(), joinPoint, e.getMessage());
            return null;
        }
    }

    public static Method getTargetMethod(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }

    public static Class<?> getTargetClass(Object candidate) {
        return AopUtils.getTargetClass(candidate);
    }

    public static String simpleMethodSignature(MethodSignature methodSignature) {
        return methodSignature.getDeclaringType().getSimpleName() + "." + methodSignature.getName();
    }
}
