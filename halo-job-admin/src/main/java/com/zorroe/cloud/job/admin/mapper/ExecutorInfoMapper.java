package com.zorroe.cloud.job.admin.mapper;

import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExecutorInfoMapper {

    void insertOrUpdate(ExecutorInfo info);

    List<ExecutorInfo> listOnline();

    void updateOffline(Long timeout);
}