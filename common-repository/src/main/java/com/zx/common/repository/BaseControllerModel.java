package com.zx.common.repository;

import com.zx.common.repository.annotation.ModelMapping;
import com.zx.common.base.utils.BaseConverter;
import com.zx.common.base.utils.SpringManager;
import com.zx.common.repository.constant.RepositoryConstants;
import com.zx.common.base.model.PageVO;
import com.zx.common.repository.util.ReflectUtil;
import com.zx.common.repository.util.RepositoryConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author: zhaoxu
 * 公用controller
 */
public class BaseControllerModel<S, E> implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(BaseControllerModel.class);

    public BaseRepository<E, Long> baseRepository;

    private Type[] actualTypeArguments;

    @RequestMapping(path = "", method = RequestMethod.POST)
    @ModelMapping
    @Operation(summary = "新增")
    @Transactional(rollbackFor = Exception.class)
    public void add(@RequestBody S entityVO) {
        E entity = BaseConverter.convert(entityVO, (Class<E>) actualTypeArguments[1]);
        try {
            ReflectUtil.setValue(entity, "valid", 1);
        } catch (Exception e) {
            logger.warn("没有找到属性：valid");
        }

        baseRepository.save(entity);
    }

    @RequestMapping(path = "", method = RequestMethod.PUT)
    @ModelMapping
    @Operation(summary = "更新", description = "必须传数据库id值")
    @Transactional(rollbackFor = Exception.class)
    public void update(@RequestBody S entityVO) {
        E entity = BaseConverter.convert(entityVO, (Class<E>) actualTypeArguments[1]);
        try {
            ReflectUtil.setValue(entity, "valid", 1);
        } catch (Exception e) {
            logger.warn("没有找到属性：valid");
        }
        baseRepository.save(entity);
    }

    @RequestMapping(path = "/{ids}", method = RequestMethod.DELETE)
    @ModelMapping
    @Operation(summary = "信息删除", description = "多个用逗号隔开")
    @Transactional(rollbackFor = Exception.class)
    public void deleteValid(@PathVariable String ids) {
        baseRepository.deleteValid(ids);
    }

    @RequestMapping(path = "/{attr}/{condition}", method = RequestMethod.GET)
    @ModelMapping
    @Operation(summary = "单条件查询", description = "对象的所有属性可作为条件进行查询，返回一个设备对象的实体信息。")
    @Parameters({
            @Parameter(name = "attr", description = "对象属性例如：id", required = true),
            @Parameter(name = "condition", description = "条件例如：1", required = true)
    })
    public S findOneByAttr(@PathVariable String attr, @PathVariable String condition) {
        return BaseConverter.convert(baseRepository.findOneByAttr(attr, condition), (Class<S>) actualTypeArguments[0]);
    }

    @RequestMapping(path = "/list/{attr}/{condition}", method = RequestMethod.GET)
    @ModelMapping
    @Operation(summary = "条件查询，精准查询，可作为条件的为对象的所有属性，返回一个列表")
    @Parameters({
            @Parameter(name = "attr", description = "对象属性例如：id", required = true),
            @Parameter(name = "condition", description = "条件例如：1,2,3", required = true)
    })
    public List<S> findByAttrs(@PathVariable String attr,
                               @PathVariable String condition) {
        return BaseConverter.convertList((List<?>) baseRepository.findByAttr(attr, condition), (Class<S>) actualTypeArguments[0]);
    }

    @RequestMapping(path = "/findAll", method = RequestMethod.GET)
    @ModelMapping
    @Operation(summary = "多条件组合查询所有，字符串默认使用模糊搜索")
    @Parameters({
            @Parameter(name = RepositoryConstants.SORTER, description = "排序条件：sorter={\"id\":\"descend\"}，ascend升序，descend降序"),
            @Parameter(name = "excludeLikeAttr", description = "是字符串类型属性但不使用模糊查询的字段，逗号隔开")
    })
    public List<S> findAllByConditions(@RequestBody(required = false) S reqObj,
                                       @RequestParam(required = false) Map<String, String> reqReplaceMap,
                                       @RequestParam(name = RepositoryConstants.SORTER, required = false) String sorter,
                                       @RequestParam(name = "excludeLikeAttr", defaultValue = "", required = false) String excludeLikeAttr) throws IllegalAccessException {
        List<String> excludeAttrs = Arrays.asList(excludeLikeAttr.split(","));
        List<E> byConditions = baseRepository.findByConditions(reqReplaceMap, excludeAttrs, sorter);
        return BaseConverter.convertList(byConditions, (Class<S>) actualTypeArguments[0]);
    }

    @RequestMapping(path = "/findByPage", method = RequestMethod.GET)
    @ModelMapping
    @Operation(summary = "多条件组合分页查询，字符串默认使用模糊搜索")
    @Parameters({
            @Parameter(name = RepositoryConstants.SORTER, description = "排序条件：sorter={\"id\":\"descend\"}，ascend升序，descend降序"),
            @Parameter(name = "excludeLikeAttr", description = "是字符串类型属性但不使用模糊查询的字段，逗号隔开"),
            @Parameter(name = RepositoryConstants.CURRENT, description = "当前页默认第 1 页"),
            @Parameter(name = RepositoryConstants.PAGE_SIZE, description = "每页数据条数默认 20 条")
    })
    public PageVO<S> findByPage(@RequestBody(required = false) S reqObj,
                                @RequestParam(required = false) Map<String, String> reqReplaceMap,
                                @RequestParam(name = RepositoryConstants.SORTER, required = false) String sorter,
                                @RequestParam(name = "excludeLikeAttr", defaultValue = "", required = false) String excludeLikeAttr,
                                @RequestParam(name = RepositoryConstants.CURRENT, required = false, defaultValue = "1") Integer current,
                                @RequestParam(name = RepositoryConstants.PAGE_SIZE, required = false, defaultValue = "20") Integer pageSize) throws IllegalAccessException {
        List<String> excludeAttrs = Arrays.asList(excludeLikeAttr.split(","));
        Page<E> byPage = baseRepository.findByPage(reqReplaceMap, current, pageSize, excludeAttrs, sorter);
        return (PageVO<S>) RepositoryConverter.convertMultiObjectToPage(byPage, (Class<?>) actualTypeArguments[0]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run(ApplicationArguments args) {
        Class<?> aClass = this.getClass();
        // 获取泛型类型
        Type genericSuperclass = aClass.getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        this.actualTypeArguments = actualTypeArguments;
        // 查找Repository
        List<String> strings = Arrays.asList(aClass.getName().split("\\."));
        String controllerName = strings.get(strings.size() - 1);
        String serviceApi = Character.toLowerCase(controllerName.charAt(0)) + controllerName.split("Controller")[0].substring(1);
        this.baseRepository = (BaseRepository<E, Long>) SpringManager.getBean(serviceApi + "Repository");
    }
}
