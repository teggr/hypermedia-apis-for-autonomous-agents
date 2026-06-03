package io.github.teggr.hypermediaapi.controller;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/", produces = {MediaTypes.HAL_FORMS_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
public class RootController {

    @GetMapping
    public RepresentationModel<?> root() {
        Link self = linkTo(methodOn(RootController.class).root())
                .withSelfRel()
                .andAffordance(afford(methodOn(OrderController.class).listOrders(null, null)))
                .andAffordance(afford(methodOn(OrderController.class).createOrder(null)));

        RepresentationModel<?> root = new RepresentationModel<>();
        root.add(self);
        root.add(linkTo(methodOn(OrderController.class).listOrders(null, null)).withRel("orders"));
        root.add(templatedOrderLookupLink());
        return root;
    }

    private Link templatedOrderLookupLink() {
        String href = linkTo(OrderController.class).toUri().toString() + "/{id}";
        return Link.of(href).withRel("order");
    }
}
