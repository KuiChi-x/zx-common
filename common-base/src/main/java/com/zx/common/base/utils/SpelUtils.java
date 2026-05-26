package com.zx.common.base.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author ZhaoXu
 * @date 2022/6/13 14:53
 */
public class SpelUtils {
    private static final Logger log = LoggerFactory.getLogger(SpelUtils.class);
    public static final String EXPRESSION_PREFIX = "#{";
    public static final String EXPRESSION_SUFFIX = "}";
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final Map<String, Expression> EXPRESSION_MAP = new ConcurrentReferenceHashMap<>();
    private static final Pattern RAW_PATTERN = Pattern.compile("[A-Za-z0-9_:.]*");

    public static boolean isRawExpression(String expressionString) {
        return RAW_PATTERN.matcher(expressionString).matches();
    }

    public static Object parseExpression(String expressionString, EvaluationContext context) {
        if (StringUtils.startsWith(expressionString, EXPRESSION_PREFIX) && StringUtils.endsWith(expressionString, EXPRESSION_SUFFIX)) {
            expressionString = expressionString.substring(EXPRESSION_PREFIX.length(), expressionString.length() - EXPRESSION_SUFFIX.length());
        } else if (StringUtils.startsWith(expressionString, EXPRESSION_PREFIX) || StringUtils.endsWith(expressionString, EXPRESSION_SUFFIX)) {
            log.warn("{} might be illegal SPEL expression", expressionString);
        }

        Expression expression = EXPRESSION_MAP.computeIfAbsent(expressionString, PARSER::parseExpression);
        return expression.getValue(context);
    }
}
