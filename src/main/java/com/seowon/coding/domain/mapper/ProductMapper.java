package com.seowon.coding.domain.mapper;

import com.seowon.coding.domain.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * MyBatis Mapper for Product (MyBatis 트랙용).
 *
 * 기본 CRUD 는 ProductMapper.xml 에 미리 구현되어 있습니다.
 * 추가 메소드가 필요한 경우 본 인터페이스에 메소드를 선언하고
 * ProductMapper.xml 에 동일 id 의 SQL 을 작성하세요.
 */
@Mapper
public interface ProductMapper {

    Optional<Product> findById(@Param("id") Long id);

    List<Product> findAll();

    /** id 가 자동 채번되어 인자 객체에 채워집니다 (useGeneratedKeys). */
    int insert(Product product);

    int update(Product product);

    int deleteById(@Param("id") Long id);

    boolean existsById(@Param("id") Long id);

    // TODO #1: 카테고리로 제품 조회
    //   List<Product> findByCategory(@Param("category") String category);
    //
    // TODO #6: 다건 조회 / 일괄 가격 업데이트
    //   List<Product> findByIdIn(@Param("ids") List<Long> ids);
    //   int bulkUpdatePrice(...);
}
