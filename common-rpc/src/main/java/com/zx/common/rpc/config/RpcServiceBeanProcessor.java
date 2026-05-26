package com.zx.common.rpc.config;

import com.zx.common.base.utils.ReflectUtils;
import com.zx.common.rpc.annotation.RequestClient;
import com.zx.common.rpc.proxy.RequestClientFactoryBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author ZhaoXu
 * @date 2023/11/2 15:04
 */
@Configuration
@Slf4j
public class RpcServiceBeanProcessor implements BeanPostProcessor {
    @Autowired
    @Qualifier("environment")
    private Environment environment;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RequestClientFactoryBean) {
            RequestClientFactoryBean factoryBean = (RequestClientFactoryBean) bean;
            Class<Object> objectType = factoryBean.getObjectType();
            if (ObjectUtils.isNotEmpty(objectType)) {
                RequestClient requestClient = objectType.getAnnotation(RequestClient.class);
                if (ObjectUtils.isNotEmpty(requestClient)) {
                    String value = requestClient.springDomain();
                    if (ObjectUtils.isNotEmpty(value)) {
                        log.info("获取 requestClient 配置地址，beanName：{}，url：{}", beanName, value);
                        String property = environment.getProperty(value);
                        if (ObjectUtils.isNotEmpty(property)) {
                            ReflectUtils.setAnnotationFieldValue(requestClient, "domains", property.split(","));
                        }
                    }
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}

