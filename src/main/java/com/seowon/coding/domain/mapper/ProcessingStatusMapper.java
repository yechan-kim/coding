package com.seowon.coding.domain.mapper;

import com.seowon.coding.domain.model.ProcessingStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * TODO #5 코드 리뷰 시 참고용.
 */
@Mapper
public interface ProcessingStatusMapper {

    Optional<ProcessingStatus> findByJobId(@Param("jobId") String jobId);

    /** id 가 자동 채번되어 인자 객체에 채워집니다 (useGeneratedKeys). */
    int insert(ProcessingStatus status);

    int update(ProcessingStatus status);
}
