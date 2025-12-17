package com.seowon.coding.domain.mapper;

import com.seowon.coding.domain.model.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * MyBatis Mapper for Order (MyBatis 트랙용).
 *
 * 기본 CRUD 는 OrderMapper.xml 에 미리 구현되어 있습니다.
 * (주의) 본 mapper 는 Order 의 스칼라 필드만 다룹니다.
 *        items 컬렉션은 OrderItemMapper 로 별도 처리하세요.
 */
@Mapper
public interface OrderMapper {

    Optional<Order> findById(@Param("id") Long id);

    List<Order> findAll();

    /** id 가 자동 채번되어 인자 객체에 채워집니다 (useGeneratedKeys). */
    int insert(Order order);

    int update(Order order);

    int deleteById(@Param("id") Long id);

    boolean existsById(@Param("id") Long id);

    // TODO #3 등에서 필요 시:
    //   List<Order> findByCustomerEmail(@Param("email") String email);
    //   List<Order> findByStatus(@Param("status") Order.OrderStatus status);
}
