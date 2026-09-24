package com.zx.common.repository;

import com.zx.common.base.utils.JsonUtils;
import com.zx.common.repository.constant.RepositoryConstants;
import com.zx.common.repository.util.ReflectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JPA通用功能扩展
 *
 * @author : zhaoxu
 */
public class BaseRepositoryImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID> implements BaseRepository<T, ID> {

    private EntityManager entityManager;

    private final Class<T> clazz;

    @Autowired(required = false)
    public BaseRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.clazz = entityInformation.getJavaType();
        this.entityManager = entityManager;
    }

    @Override
    public Page<T> findByPage(Map<String, String> objConditions, Integer current, Integer pageSize, List<String> excludeLikeAttr, String sortAttr) {
        Pageable pageable;
        if (StringUtils.hasText(sortAttr)) {
            pageable = PageRequest.of(current - 1, pageSize, sortAttr(objConditions, sortAttr));
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

        if (StringUtils.hasText(sortAttr)) {
            return this.findAll(specification, sortAttr(objConditions, sortAttr));
        } else {
            return this.findAll(specification);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteValid(String ids) {
        List<String> strings = Arrays.asList(ids.split(","));
        if (!CollectionUtils.isEmpty(strings)) {
            //获取主键
            List<Field> idAnnoation = ReflectUtil.getTargetAnnoation(clazz, Id.class);
            if (!CollectionUtils.isEmpty(idAnnoation)) {
                Field field = idAnnoation.get(0);
                strings.forEach(id -> {
                    T object = this.findOneByAttr(field.getName(), id);
                    if (object != null) {
                        try {
                            ReflectUtil.setValue(object, "valid", 0);
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
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
     * @param tableMap tableMap
     * @param sorterBy 默认按此属性排序
     * @return Sort
     */
    public static Sort sortAttr(Map<String, String> tableMap, String sorterBy) {
        Sort sort;
        if (tableMap.get(RepositoryConstants.SORTER) != null && !RepositoryConstants.EMPTY_SORTER.equals(tableMap.get(RepositoryConstants.SORTER))) {
            String sortString = tableMap.get(RepositoryConstants.SORTER);
            Map<String, String> map = JsonUtils.fromJson(sortString, Map.class);
            Iterator<String> iterator = map.keySet().iterator();
            String sortAttr = iterator.next();

            if (RepositoryConstants.ASCEND.equals(map.get(sortAttr))) {
                sort = Sort.by(Sort.Direction.ASC, sortAttr);
            } else {
                sort = Sort.by(Sort.Direction.DESC, sortAttr);
            }
        } else {
            sort = Sort.by(Sort.Direction.DESC, sorterBy);
        }
        return sort;
    }
}
