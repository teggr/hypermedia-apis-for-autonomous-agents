package io.github.teggr.hypermediaapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.hateoas.config.EnableHypermediaSupport;

@SpringBootApplication
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL_FORMS)
public class HypermediaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HypermediaApiApplication.class, args);
    }
}
