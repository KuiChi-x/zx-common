package com.zx.common.repository.baseController;

import com.zx.common.base.model.PageVO;
import com.zx.common.base.utils.BaseConverter;
import com.zx.common.base.utils.SpringManager;
import com.zx.common.repository.annotation.ModelMapping;
import com.zx.common.repository.baseRepository.BaseRepository;
import com.zx.common.repository.constant.RepositoryConstants;
import com.zx.common.repository.util.ReflectUtil;
import com.zx.common.repository.util.RepositoryConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
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
public class BaseController<S, E> implements ApplicationRunner {
    public BaseRepository<E, Long> baseRepository;

    private Type[] actualTypeArguments;

    @RequestMapping(path = "", method = RequestMethod.POST)
    @ModelMapping
    public void add(@RequestBody S entityVO) {
        E entity = BaseConverter.convert(entityVO, (Class<E>) actualTypeArguments[1]);
        try {
            ReflectUtil.setValue(entity, "valid", 1);
        } catch (Exception ignored) {
        }
        baseRepository.saveIgnoreNull(entity);
    }

    @RequestMapping(path = "", method = RequestMethod.PUT)
    @ModelMapping
    public void update(@RequestBody S entityVO) {
        E entity = BaseConverter.convert(entityVO, (Class<E>) actualTypeArguments[1]);
        try {
            ReflectUtil.setValue(entity, "valid", 1);
        } catch (Exception ignored) {
        }
        baseRepository.saveIgnoreNull(entity);
    }

    @RequestMapping(path = "/{ids}", method = RequestMethod.DELETE)
    @ModelMapping
    public void delete(@PathVariable String ids) {
        baseRepository.delete(ids);
    }

    @RequestMapping(path = "/deleteValid/{ids}", method = RequestMethod.DELETE)
    @ModelMapping
    public void deleteValid(@PathVariable String ids) {
        baseRepository.deleteValid(ids);
    }

    @RequestMapping(path = "/{attr}/{condition}", method = RequestMethod.GET)
    @ModelMapping
    public S findOneByAttr(@PathVariable String attr, @PathVariable String condition) {
        return BaseConverter.convert(baseRepository.findOneByAttr(attr, condition), (Class<S>) actualTypeArguments[0]);
    }

    @RequestMapping(path = "/list/{attr}/{condition}", method = RequestMethod.GET)
    @ModelMapping
    public List<S> findByAttrs(@PathVariable String attr,
                               @PathVariable String condition) {
        return BaseConverter.convertList((List<?>) baseRepository.findByAttr(attr, condition), (Class<S>) actualTypeArguments[0]);
    }

    @RequestMapping(path = "/findAll", method = RequestMethod.GET)
    @ModelMapping
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
