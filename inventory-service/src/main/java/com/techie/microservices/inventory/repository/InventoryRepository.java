package com.techie.microservices.inventory.repository;

import com.techie.microservices.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsBySkuCodeAndQuantityIsGreaterThanEqual(
            String skuCode,
            Integer quantity
    );

    Optional<Inventory> findBySkuCode(String skuCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Inventory i
            SET i.quantity = i.quantity + :quantityToAdd
            WHERE i.skuCode = :skuCode
            """)
    int increaseInventoryQuantity(
            @Param("skuCode") String skuCode,
            @Param("quantityToAdd") Integer quantityToAdd
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Inventory i
            SET i.quantity = i.quantity - :quantityToDecrease
            WHERE i.skuCode = :skuCode
            AND i.quantity >= :quantityToDecrease
            """)
    int decreaseInventoryQuantity(
            @Param("skuCode") String skuCode,
            @Param("quantityToDecrease") Integer quantityToDecrease
    );
}