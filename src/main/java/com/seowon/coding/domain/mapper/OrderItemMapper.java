package com.seowon.coding.domain.mapper;

import com.seowon.coding.domain.model.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper for OrderItem (MyBatis 트랙용).
 *
 * 기본 단건 INSERT / 조회는 OrderItemMapper.xml 에 미리 구현되어 있습니다.
 * 일괄 INSERT 등 추가 메소드는 본 인터페이스 + XML 에 함께 작성하세요.
 */
@Mapper
public interface OrderItemMapper {

    /** id 가 자동 채번되어 인자 객체에 채워집니다 (useGeneratedKeys). */
    int insert(OrderItem item);

    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    // TODO #3: 일괄 INSERT
    //   int insertBatch(@Param("items") List<OrderItem> items);
}
