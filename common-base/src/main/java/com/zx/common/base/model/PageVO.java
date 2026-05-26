package com.zx.common.base.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ZhaoXu
 * @date 2022/7/24 1:58
 */
@Data
public class PageVO<T> implements Serializable {
    private static final long serialVersionUID = -3355752076145642645L;
    /**
     * 总数
     */
    Long total;

    /**
     * 数据
     */
    List<T> list;
}
