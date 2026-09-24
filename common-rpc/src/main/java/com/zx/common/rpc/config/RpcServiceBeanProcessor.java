package com.zx.common.rpc.config;

import com.zx.common.base.utils.ReflectUtils;
import com.zx.common.rpc.annotation.RequestClient;
import com.zx.common.rpc.proxy.RequestClientFactoryBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * @author ZhaoXu
 * @date 2023/11/2 15:04
 */
@Configuration
@Slf4j
public class RpcServiceBeanProcessor implements BeanPostProcessor {
    @org.springframework.beans.factory.annotation.Autowired
    private Environment environment;

    @Override
    public Object postProcessBeforeInitialization(@Nullable Object bean, @Nullable String beanName) throws BeansException {
        if (bean instanceof RequestClientFactoryBean) {
            RequestClientFactoryBean factoryBean = (RequestClientFactoryBean) bean;
            Class<Object> objectType = factoryBean.getObjectType();
            if (ObjectUtils.isNotEmpty(objectType)) {
                RequestClient requestClient = objectType.getAnnotation(RequestClient.class);
                if (ObjectUtils.isNotEmpty(requestClient)) {
                    String value = requestClient.domainEnvironment();
                    log.info("获取 requestClient 配置地址，beanName：{}，url：{}", beanName, value);
                    String property = environment.getProperty(value);
                    if (ObjectUtils.isNotEmpty(property)) {
                        ReflectUtils.setAnnotationFieldValue(requestClient, "domains", property.split(","));
                    }
                }
            }
        }
        return bean;
    }
}
