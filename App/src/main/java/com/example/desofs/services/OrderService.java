package com.example.desofs.services;

import com.example.desofs.dtos.CreateOrderRequest;
import com.example.desofs.dtos.OrderDTO;
import com.example.desofs.dtos.OrderItemDTO;
import com.example.desofs.domain.Movie;
import com.example.desofs.domain.Order;
import com.example.desofs.domain.OrderItem;
import com.example.desofs.domain.User;
import com.example.desofs.repositories.MovieRepository;
import com.example.desofs.repositories.OrderRepository;
import com.example.desofs.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, MovieRepository movieRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.getUserId()).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found: " + request.getUserId());
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Movie movie = movieRepository.findById(itemReq.getMovieId()).orElse(null);
            if (movie == null) {
                throw new RuntimeException("Movie not found: " + itemReq.getMovieId());
            }

            OrderItem item = new OrderItem();
            item.setMovie(movie);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(movie.getPrice());
            items.add(item);

            BigDecimal itemSubtotal = movie.getPrice().multiply(new BigDecimal(itemReq.getQuantity()));
            total = total.add(itemSubtotal);
        }

        Order order = new Order();
        order.setUser(user);
        order.setItems(items);
        order.setTotal(total);

        Order saved = orderRepository.save(order);
        return toOrderDTO(saved);
    }

    public OrderDTO getOrder(Long id) {
        Optional<Order> opt = orderRepository.findById(id);
        return opt.map(this::toOrderDTO).orElse(null);
    }

    public List<OrderDTO> listAll() {
        return orderRepository.findAll().stream().map(this::toOrderDTO).toList();
    }

    private OrderDTO toOrderDTO(Order order) {
        List<OrderItemDTO> itemDTOs = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            OrderItemDTO dto = new OrderItemDTO(
                item.getId(),
                item.getMovie().getId(),
                item.getMovie().getTitle(),
                item.getQuantity(),
                item.getUnitPrice()
            );
            itemDTOs.add(dto);
        }

        return new OrderDTO(
            order.getId(),
            order.getUser().getId(),
            order.getUser().getEmail(),
            itemDTOs,
            order.getTotal()
        );
    }
}
