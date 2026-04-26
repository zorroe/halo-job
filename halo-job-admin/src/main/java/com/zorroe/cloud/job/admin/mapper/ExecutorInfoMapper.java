package com.zorroe.cloud.job.admin.mapper;

import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExecutorInfoMapper {

    void insertOrUpdate(ExecutorInfo info);

    List<ExecutorInfo> listExecutors(@Param("executorGroup") String executorGroup,
                                     @Param("executorApp") String executorApp,
                                     @Param("status") Integer status);

    void updateOffline(Long timeout);
}
