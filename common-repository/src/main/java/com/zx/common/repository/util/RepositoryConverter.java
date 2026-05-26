package com.zx.common.repository.util;

import com.zx.common.base.model.PageVO;
import com.zx.common.base.utils.BaseConverter;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZhaoXu
 * @date 2023/11/5 11:47
 */
public class RepositoryConverter {
    public static <S, D> PageVO<D> convertMultiObjectToPage(Page<S> srcPages, Class<D> destClass) {
        PageVO<D> pageResponse = new PageVO<>();
        List<D> destList = new ArrayList<>();
        if (srcPages != null && srcPages.getContent() != null) {
            for (S srcPage : srcPages) {
                destList.add(BaseConverter.convert(srcPage, destClass));
            }
        }
        pageResponse.setTotal(srcPages.getTotalElements());
        pageResponse.setList(destList);
        return pageResponse;
    }
}
