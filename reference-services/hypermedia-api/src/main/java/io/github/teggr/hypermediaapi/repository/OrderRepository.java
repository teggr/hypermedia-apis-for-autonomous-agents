package io.github.teggr.hypermediaapi.repository;

import io.github.teggr.hypermediaapi.domain.Order;
import io.github.teggr.hypermediaapi.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(OrderStatus status);
}
