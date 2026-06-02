package io.github.teggr.conventionalapi.repository;

import io.github.teggr.conventionalapi.domain.Order;
import io.github.teggr.conventionalapi.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(OrderStatus status);
}
