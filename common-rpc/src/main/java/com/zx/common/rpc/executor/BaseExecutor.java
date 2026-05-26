package com.zx.common.rpc.executor;


import com.zx.common.rpc.dto.RequestClientDTO;

/**
 * @author ZhaoXu
 * @date 2023/11/8 14:37
 */
public interface BaseExecutor {
    /**
     * 执行请求
     * @param requestClientDTO
     * @return
     */
    public Object execute(RequestClientDTO requestClientDTO);
}
