package com.seowon.coding.service;

import com.seowon.coding.domain.model.Product;
import com.seowon.coding.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
    
    public Product updateProduct(Long id, Product product) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        product.setId(id);
        return productRepository.save(product);
    }
    
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    /**
     * TODO #1 [JPA]: Repository를 사용하여 category 로 찾을 제품목록 제공
     *
     * TODO #1 [MyBatis]: ProductMapper.selectByCategory(category) 형태로 mapper 메소드를 정의하고
     *                     XML 또는 @Select 어노테이션으로 구현
     */
    @Transactional(readOnly = true)
    public List<Product> findProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    /**
     * TODO #6 [JPA] (리팩토링): 가격 변경 로직을 도메인 객체 안으로 리팩토링하세요.
     * 다음 관점에서 개선하세요:
     * - BigDecimal 정확성 (scale, RoundingMode)
     * - 가격 계산 책임 위치 (도메인 vs 서비스)
     * - 세율 / 반올림 정책의 외부화
     * - DB I/O 최소화 (영속성 컨텍스트 활용)
     * Repository 의존은 도메인 객체 밖에 유지
     *
     * TODO #6 [MyBatis] (리팩토링): 동일 목표 + SQL 설계 관점:
     * - 다건 SELECT (foreach IN 절) 1회로 대상 Product 조회
     * - 도메인 메소드로 가격 계산 (JPA 트랙과 공통)
     * - 단일 bulk UPDATE 쿼리로 일괄 반영 (CASE WHEN 또는 foreach UPDATE 등)
     * - SQL Injection 안전성, NULL/스케일 처리, 부분 실패 시 롤백 정책
     * Mapper 의존은 도메인 객체 밖에 유지
     */
    public void applyBulkPriceChange(List<Long> productIds, double percentage, boolean includeTax) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("empty productIds");
        }
        for (Long id : productIds) {
            Product p = productRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

            double base = p.getPrice() == null ? 0.0 : p.getPrice().doubleValue();
            double changed = base + (base * (percentage / 100.0));
            if (includeTax) {
                changed = changed * 1.1;
            }
            BigDecimal newPrice = BigDecimal.valueOf(changed).setScale(2, RoundingMode.HALF_UP);
            p.setPrice(newPrice);
            productRepository.save(p);
        }
    }
}
