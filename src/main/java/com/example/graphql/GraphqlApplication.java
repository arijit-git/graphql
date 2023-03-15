package com.example.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.example.graphql.CustomerEventType.CREATED;
import static com.example.graphql.CustomerEventType.UPDATED;

@SpringBootApplication
public class GraphqlApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphqlApplication.class, args);
    }

}

record Order(Integer id, Integer customerId) {}
record Customer(Integer id, String name) {}
record CustomerEvent(Customer customer, CustomerEventType event) {}

enum CustomerEventType {
    CREATED, UPDATED
}

@Component
class CrmClient {

    CrmClient(){

        Flux.fromIterable(List.of("Cornēlia", "Chris", "Arijit", "Soutrik", "Max", "Hanno", "Veronik"));
    }

    private final Map<Customer, Collection<Order>> db = new ConcurrentHashMap<>();
    private final AtomicInteger id = new AtomicInteger();

    Flux<Customer> getCustomers() {
        return Flux.fromIterable(this.db.keySet());
    }

    Mono<Customer> getCustomerById(Integer customerId) {
        return getCustomers().filter(c -> c.id().equals(customerId)).singleOrEmpty();
    }

    Flux<Customer> getCustomersByName(String name) {
        return getCustomers().filter(c -> c.name().equalsIgnoreCase(name));
    }

    Mono<Customer> addCustomer(String name) {
        var key = new Customer(id(), name);
        this.db.put(key, new CopyOnWriteArrayList<>());
        return Mono.just(key);
    }

    Flux<Order> getOrdersFor(Integer customerId){
        return getCustomerById(customerId)
                .map(this.db::get)
                .flatMapMany(Flux::fromIterable);
    }

    Flux<CustomerEvent> getCustomerEvents(Integer customerId) {
        return getCustomerById(customerId)
                .flatMapMany(customer ->
                        Flux.fromStream(Stream.generate(() -> {
                            var event = Math.random() > 0.5 ? CREATED : UPDATED;
                            return new CustomerEvent(customer, event);
                        }))
                                .take(10).delayElements(Duration.ofSeconds(1)));
    }

    private int id() {
        return this.id.incrementAndGet();
    }
}