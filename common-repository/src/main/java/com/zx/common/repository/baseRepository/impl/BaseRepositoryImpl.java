package com.zx.common.repository.baseRepository.impl;

import com.zx.common.base.utils.JsonUtils;
import com.zx.common.base.utils.ListUtils;
import com.zx.common.base.utils.SpringManager;
import com.zx.common.repository.baseRepository.BaseRepository;
import com.zx.common.repository.constant.RepositoryConstants;
import com.zx.common.repository.util.ReflectUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JPA通用功能扩展
 *
 * @author : zhaoxu
 */
@SuppressWarnings("unchecked")
public class BaseRepositoryImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID> implements BaseRepository<T, ID> {
    private EntityManager entityManager;

    private JpaEntityInformation<T, ?> jpaEntityInformation;

    private final Class<T> clazz;

    private ThreadPoolTaskExecutor threadPoolExecutor;

    private PlatformTransactionManager platformTransactionManager;

    @Autowired(required = false)
    public BaseRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.clazz = entityInformation.getJavaType();
        this.entityManager = entityManager;
        this.jpaEntityInformation = entityInformation;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public T saveIgnoreNull(T entity) {
        ID id = (ID) jpaEntityInformation.getId(entity);
        if (id != null) {
            Optional<T> op = findById(id);
            if (op.isPresent()) {
                T t = op.get();
                List<String> nullFields = new ArrayList<>();
                try {
                    Map<String, Object> values = ReflectUtil.getValues(entity);
                    values.forEach((k, v) -> {
                        if (v == null) {
                            nullFields.add(k);
                        }
                    });
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                BeanUtils.copyProperties(entity, t, nullFields.toArray(new String[0]));
                entity = t;
                entityManager.merge(t);
            }
        } else {
            entityManager.persist(entity);
        }
        entityManager.flush();
        return entity;
    }

    @Override
    public void batchSave(List<T> entityList, String repositoryName) {
        if (threadPoolExecutor == null || platformTransactionManager == null) {
            this.threadPoolExecutor = (ThreadPoolTaskExecutor) SpringManager.getBean("basicThreadPoolExecutor");
            this.platformTransactionManager = (PlatformTransactionManager) SpringManager.getBean("transactionManager");
        }
        if (ObjectUtils.isNotEmpty(entityList)) {
            AtomicBoolean successFlag = new AtomicBoolean(Boolean.TRUE);
            List<List<T>> partition = ListUtils.partition(entityList, 100);
            CyclicBarrier cyclicBarrier = new CyclicBarrier(partition.size());

            if (partition.size() > threadPoolExecutor.getCorePoolSize()) {
                threadPoolExecutor.setCorePoolSize(partition.size());
                threadPoolExecutor.setMaxPoolSize(partition.size());
            }
            AtomicReference<Exception> throwException = new AtomicReference<>(null);
            CompletableFuture<?>[] completableFutures = partition.stream().map(subList ->
                            CompletableFuture.runAsync(() -> {
                                DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
                                transactionDefinition.setTimeout(15);
                                TransactionStatus transaction = platformTransactionManager.getTransaction(transactionDefinition);
                                try {
                                    for (T t : subList) {
                                        BaseRepository<T, ID> baseRepository = (BaseRepository<T, ID>) SpringManager.getBean(repositoryName);
                                        baseRepository.saveIgnoreNull(t);
                                    }
                                } catch (Exception e) {
                                    throwException.set(e);
                                    successFlag.set(Boolean.FALSE);
                                }
                                try {
                                    cyclicBarrier.await(15, TimeUnit.SECONDS);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                if (successFlag.get()) {
                                    platformTransactionManager.commit(transaction);
                                } else {
                                    platformTransactionManager.rollback(transaction);
                                }
                            }, threadPoolExecutor))
                    .toArray(CompletableFuture[]::new);
            try {
                CompletableFuture.allOf(completableFutures).get();
                if (partition.size() > threadPoolExecutor.getCorePoolSize()) {
                    threadPoolExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
                    threadPoolExecutor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (throwException.get() != null) {
                throw new RuntimeException(throwException.get());
            }
        }
    }

    @Override
    public Page<T> findByPage(Map<String, String> objConditions, Integer current, Integer pageSize, List<String> excludeLikeAttr, String sortAttr) {
        Pageable pageable;
        if (!StringUtils.isEmpty(sortAttr)) {
            pageable = PageRequest.of(current - 1, pageSize, sortAttr(sortAttr, "id"));
        } else {
            pageable = PageRequest.of(current - 1, pageSize);
        }

        Specification<T> specification = ReflectUtil.createSpecification(objConditions, clazz, excludeLikeAttr);
        return this.findAll(specification, pageable);
    }

    /**
     * 省去不必要的关联map参数
     *
     * @param objConditions   查询条件
     * @param excludeLikeAttr 是字符串类型，但是不使用模糊查询的字段，可为空
     * @param sortAttr        排序，可为空
     * @return List
     */
    @Override
    public List<T> findByConditions(Map<String, String> objConditions, List<String> excludeLikeAttr, String sortAttr) {
        Specification<T> specification = ReflectUtil.createSpecification(objConditions, clazz, excludeLikeAttr);

        if (!StringUtils.isEmpty(sortAttr)) {
            return this.findAll(specification, sortAttr(sortAttr, "id"));
        } else {
            return this.findAll(specification);
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void delete(String ids) {
        List<String> strings = Arrays.asList(ids.split(","));
        if (!CollectionUtils.isEmpty(strings)) {
            strings.forEach(id -> {
                this.deleteById((ID) id);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteValid(String ids) {
        List<String> strings = Arrays.asList(ids.split(","));
        if (!CollectionUtils.isEmpty(strings)) {
            //获取主键
            List<Field> idAnnotation = ReflectUtil.getTargetAnnoation(clazz, Id.class);
            if (!CollectionUtils.isEmpty(idAnnotation)) {
                Field field = idAnnotation.get(0);
                strings.forEach(id -> {
                    T object = this.findOneByAttr(field.getName(), id);
                    if (object != null) {
                        try {
                            ReflectUtil.setValue(object, "valid", 0);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        this.save(object);
                    }
                });
            }
        }
    }

    @Override
    public T findOneByAttr(String attr, String condition) {
        Specification<T> specification = ReflectUtil.createOneSpecification(attr, condition);
        Optional<T> result = this.findOne(specification);

        return result.orElse(null);
    }

    @Override
    public List<T> findByAttr(String attr, String condition) {
        Specification<T> specification = ReflectUtil.createOneSpecification(attr, condition);
        return this.findAll(specification);
    }

    /**
     * 表格排序
     *
     * @param sortString tableMap
     * @param defaultSorterBy 默认按此属性排序
     * @return Sort
     */
    public static Sort sortAttr(String sortString, String defaultSorterBy) {
        Sort sort;
        if (sortString != null && !RepositoryConstants.EMPTY_SORTER.equals(sortString)) {
            Map<String, String> map = JsonUtils.fromJson(sortString, Map.class);
            Iterator<String> iterator = map.keySet().iterator();
            String sortAttr = iterator.next();

            if (RepositoryConstants.ASCEND.equals(map.get(sortAttr))) {
                sort = Sort.by(Sort.Direction.ASC, sortAttr);
            } else {
                sort = Sort.by(Sort.Direction.DESC, sortAttr);
            }
        } else {
            sort = Sort.by(Sort.Direction.DESC, defaultSorterBy);
        }
        return sort;
    }
}
