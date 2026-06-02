package io.github.teggr.hypermediaapi.service;

import io.github.teggr.hypermediaapi.domain.Order;
import io.github.teggr.hypermediaapi.domain.OrderStatus;
import io.github.teggr.hypermediaapi.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order create(String customerId, String description) {
        return repository.save(new Order(customerId, description));
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Order> findByCustomer(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus status) {
        return repository.findByStatus(status);
    }

    public Order transition(Long id, OrderStatus newStatus) {
        Order order = findById(id);
        order.transitionTo(newStatus);
        return repository.save(order);
    }
}
