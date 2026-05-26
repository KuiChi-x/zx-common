package com.zx.common.repository.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author : zhaoxu
 * 重写requestMapping
 */
@Configuration
@EnableJpaRepositories(basePackages = {"com.zx"}, repositoryFactoryBeanClass = BaseJpaRepositoryFactoryBean.class)
@EnableTransactionManagement
@EntityScan(basePackages = {"com.zx"})
public class BaseJpaRepositoryConfig {

}
