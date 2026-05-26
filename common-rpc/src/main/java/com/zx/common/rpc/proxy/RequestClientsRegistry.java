package com.zx.common.rpc.proxy;

import com.zx.common.rpc.annotation.EnableHttpRequest;
import com.zx.common.rpc.annotation.RequestClient;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ZhaoXu
 */
public class RequestClientsRegistry extends ClassPathScanningCandidateComponentProvider implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 获取MapperScan注解的属性属性
        AnnotationAttributes mapperScanAttrs = AnnotationAttributes
                .fromMap(importingClassMetadata.getAnnotationAttributes(EnableHttpRequest.class.getName()));
        if (mapperScanAttrs != null) {
            String packageName = ClassUtils.getPackageName(importingClassMetadata.getClassName());
            Set<BeanDefinition> classesWithMethodAnnotation = findClassesWithMethodAnnotation(RequestMapping.class, packageName);
            BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();
            for (BeanDefinition beanDefinition : classesWithMethodAnnotation) {
                GenericBeanDefinition genericBeanDefinition = (GenericBeanDefinition) beanDefinition;
                String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
                this.registerBeanDefinition(definitionHolder, registry);
                String beanClassName = genericBeanDefinition.getBeanClassName();
                if (Objects.nonNull(beanClassName)) {
                    genericBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
                }
                genericBeanDefinition.setBeanClass(RequestClientFactoryBean.class);
                genericBeanDefinition.setAutowireMode(2);
                genericBeanDefinition.setLazyInit(true);
            }
        }
    }

    public Set<BeanDefinition> findClassesWithMethodAnnotation(Class<? extends Annotation> annotation, String basePackage) {
        this.addIncludeFilter((metadataReader, metadataReaderFactory) -> metadataReader.getAnnotationMetadata().getAnnotationTypes().contains(RequestClient.class.getName()));
        Set<BeanDefinition> beanDefinitions = findCandidateComponents(basePackage);
        return beanDefinitions.stream().filter(item -> {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(item.getBeanClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            for (Method method : clazz.getDeclaredMethods()) {
                Annotation[] methodAnnotations = method.getAnnotations();
                for (Annotation methodAnnotation : methodAnnotations) {
                    if(methodAnnotation.annotationType().isAnnotationPresent(annotation)) {
                        return true;
                    }
                }
            }
            return false;
        }).collect(Collectors.toSet());
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return true;
    }

    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
        BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
    }
}
