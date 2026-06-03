package io.github.teggr.hypermediaapi.controller;

import io.github.teggr.hypermediaapi.domain.Order;
import io.github.teggr.hypermediaapi.domain.OrderStatus;
import io.github.teggr.hypermediaapi.model.OrderModel;
import io.github.teggr.hypermediaapi.model.OrderModelAssembler;
import io.github.teggr.hypermediaapi.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * Hypermedia-driven REST controller for order management.
 *
 * <p>Every response includes a {@code _links} section that lists <em>only</em>
 * the operations that are valid given the current state of the order. Agents
 * interacting with this API can discover available actions from the response
 * itself rather than relying on a pre-loaded OpenAPI spec or MCP tool list.
 *
 * <p>Produces {@code application/hal+json} by default.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;
    private final OrderModelAssembler assembler;

    public OrderController(OrderService service, OrderModelAssembler assembler) {
        this.service   = service;
        this.assembler = assembler;
    }

    @GetMapping(produces = {MediaTypes.HAL_FORMS_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
    public CollectionModel<OrderModel> listOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) OrderStatus status) {

        List<Order> orders;
        if (customerId != null) {
            orders = service.findByCustomer(customerId);
        } else if (status != null) {
            orders = service.findByStatus(status);
        } else {
            orders = service.findAll();
        }

        CollectionModel<OrderModel> collection = assembler.toCollectionModel(orders);

        Link self = linkTo(methodOn(OrderController.class).listOrders(null, null))
                .withSelfRel()
                .andAffordance(afford(methodOn(OrderController.class).createOrder(null)));

        collection.add(self);
        collection.add(templatedOrderLookupLink());
        return collection;
    }

    @PostMapping(produces = {MediaTypes.HAL_FORMS_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
    public ResponseEntity<OrderModel> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order created = service.create(request.customerId(), request.description());
        OrderModel model = assembler.toModel(created);
        return ResponseEntity
                .created(model.getRequiredLink("self").toUri())
                .body(model);
    }

    @GetMapping(value = "/{id}", produces = {MediaTypes.HAL_FORMS_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
    public OrderModel getOrder(@PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    @PostMapping(value = "/{id}/confirm", produces = {MediaTypes.HAL_FORMS_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
    public OrderModel confirmOrder(@PathVariable Long id) {
        return assembler.toModel(service.transition(id, OrderStatus.CONFIRMED));
    }

    @PostMapping(value = "/{id}/ship", produces = {MediaTypes.HAL_FORMS_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
    public OrderModel shipOrder(@PathVariable Long id) {
        return assembler.toModel(service.transition(id, OrderStatus.SHIPPED));
    }

    @PostMapping(value = "/{id}/deliver", produces = {MediaTypes.HAL_FORMS_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
    public OrderModel deliverOrder(@PathVariable Long id) {
        return assembler.toModel(service.transition(id, OrderStatus.DELIVERED));
    }

    @PostMapping(value = "/{id}/cancel", produces = {MediaTypes.HAL_FORMS_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
    public OrderModel cancelOrder(@PathVariable Long id) {
        return assembler.toModel(service.transition(id, OrderStatus.CANCELLED));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleInvalidTransition() {}

    // ── inner record ─────────────────────────────────────────────────────────

    public record CreateOrderRequest(
            @NotBlank String customerId,
            @NotBlank String description
    ) {}

    private Link templatedOrderLookupLink() {
        String href = linkTo(OrderController.class).toUri().toString() + "/{id}";
        return Link.of(href).withRel("order");
    }
}
