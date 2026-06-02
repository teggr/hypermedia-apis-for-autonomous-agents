package io.github.teggr.hypermediaapi.model;

import io.github.teggr.hypermediaapi.controller.OrderController;
import io.github.teggr.hypermediaapi.domain.Order;
import io.github.teggr.hypermediaapi.domain.OrderStatus;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * Assembles an {@link OrderModel} from an {@link Order} domain object, adding
 * only the hypermedia links that are valid for the order's current state.
 *
 * <p>This is the heart of the hypermedia approach: the server decides which
 * actions are available — not the client (or agent). An agent following HATEOAS
 * principles never needs to know the full set of endpoints; it simply inspects
 * {@code _links} in each response.
 */
@Component
public class OrderModelAssembler extends RepresentationModelAssemblerSupport<Order, OrderModel> {

    public OrderModelAssembler() {
        super(OrderController.class, OrderModel.class);
    }

    @Override
    public OrderModel toModel(Order order) {
        OrderModel model = new OrderModel(order);

        // self link — always present
        model.add(linkTo(methodOn(OrderController.class).getOrder(order.getId())).withSelfRel());

        // collection link — always present
        model.add(linkTo(methodOn(OrderController.class).listOrders(null, null)).withRel("orders"));

        // state-conditional transition links
        OrderStatus status = order.getStatus();

        if (status.canTransitionTo(OrderStatus.CONFIRMED)) {
            model.add(linkTo(methodOn(OrderController.class).confirmOrder(order.getId())).withRel("confirm"));
        }
        if (status.canTransitionTo(OrderStatus.SHIPPED)) {
            model.add(linkTo(methodOn(OrderController.class).shipOrder(order.getId())).withRel("ship"));
        }
        if (status.canTransitionTo(OrderStatus.DELIVERED)) {
            model.add(linkTo(methodOn(OrderController.class).deliverOrder(order.getId())).withRel("deliver"));
        }
        if (status.canTransitionTo(OrderStatus.CANCELLED)) {
            model.add(linkTo(methodOn(OrderController.class).cancelOrder(order.getId())).withRel("cancel"));
        }

        return model;
    }
}
