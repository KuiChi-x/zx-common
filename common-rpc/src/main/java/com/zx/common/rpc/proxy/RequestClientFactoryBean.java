package com.zx.common.rpc.proxy;

import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

/**
 * @author ZhaoXu
 */
public class RequestClientFactoryBean implements FactoryBean<Object> {
    private final Class<Object> mapperInterface;

    public RequestClientFactoryBean(Class<Object> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @Override
    public Object getObject() {
        RequestClientHandler<Object> requestClientHandler = new RequestClientHandler<>(mapperInterface);
        return Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[]{mapperInterface}, requestClientHandler);
    }

    @Override
    public Class<Object> getObjectType() {
        return mapperInterface;
    }
}
