package com.zorroe.cloud.job.admin.mapper;

import com.zorroe.cloud.job.admin.entity.ExecutorHandlerInfo;
import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExecutorHandlerMapper {

    void deleteByExecutorAddress(@Param("executorAddress") String executorAddress);

    void batchInsert(@Param("items") List<ExecutorHandlerInfo> items);

    List<ExecutorInfo> listMatchedOnlineExecutors(@Param("executorGroup") String executorGroup,
                                                  @Param("executorApp") String executorApp,
                                                  @Param("handlerName") String handlerName);

    List<ExecutorHandlerInfo> listAvailableHandlers(@Param("executorGroup") String executorGroup,
                                                    @Param("executorApp") String executorApp);

    List<ExecutorHandlerInfo> listRegisteredHandlers(@Param("executorGroup") String executorGroup,
                                                     @Param("executorApp") String executorApp,
                                                     @Param("handlerName") String handlerName);
}
