package com.vaadin.starter.bakery.backend.repositories;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.vaadin.starter.bakery.backend.data.OrderState;
import com.vaadin.starter.bakery.backend.data.entity.Order;
import com.vaadin.starter.bakery.backend.data.entity.OrderSummary;

/**
 * Repository interface for managing {@link Order} entities.
 * Provides methods for querying and counting orders, as well as retrieving summaries and statistics.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds orders with due dates after the specified date.
     *
     * @param filterDate the date after which orders are due
     * @param pageable the paging information
     * @return a page of orders
     */
    @EntityGraph(value = Order.ENTITY_GRAPTH_BRIEF, type = EntityGraphType.LOAD)
    Page<Order> findByDueDateAfter(LocalDate filterDate, Pageable pageable);

    /**
     * Finds orders where the customer's full name contains the given search query (case-insensitive).
     *
     * @param searchQuery the search string for the customer's full name
     * @param pageable the paging information
     * @return a page of orders
     */
    @EntityGraph(value = Order.ENTITY_GRAPTH_BRIEF, type = EntityGraphType.LOAD)
    Page<Order> findByCustomerFullNameContainingIgnoreCase(String searchQuery, Pageable pageable);

    /**
     * Finds orders where the customer's full name contains the given search query (case-insensitive)
     * and the due date is after the specified date.
     *
     * @param searchQuery the search string for the customer's full name
     * @param dueDate the date after which orders are due
     * @param pageable the paging information
     * @return a page of orders
     */
    @EntityGraph(value = Order.ENTITY_GRAPTH_BRIEF, type = EntityGraphType.LOAD)
    Page<Order> findByCustomerFullNameContainingIgnoreCaseAndDueDateAfter(String searchQuery, LocalDate dueDate, Pageable pageable);

    /**
     * Finds all orders.
     *
     * @return a list of all orders
     */
    @Override
    @EntityGraph(value = Order.ENTITY_GRAPTH_BRIEF, type = EntityGraphType.LOAD)
    List<Order> findAll();

    /**
     * Finds all orders in a paginated format.
     *
     * @param pageable the paging information
     * @return a page of orders
     */
    @Override
    @EntityGraph(value = Order.ENTITY_GRAPTH_BRIEF, type = EntityGraphType.LOAD)
    Page<Order> findAll(Pageable pageable);

    /**
     * Finds order summaries with due dates greater than or equal to the specified date.
     *
     * @param dueDate the minimum due date
     * @return a list of order summaries
     */
    @EntityGraph(value = Order.ENTITY_GRAPTH_BRIEF, type = EntityGraphType.LOAD)
    List<OrderSummary> findByDueDateGreaterThanEqual(LocalDate dueDate);

    /**
     * Finds an order by its ID with a full entity graph.
     *
     * @param id the order ID
     * @return an optional containing the order, if found
     */
    @Override
    @EntityGraph(value = Order.ENTITY_GRAPTH_FULL, type = EntityGraphType.LOAD)
    Optional<Order> findById(Long id);

    /**
     * Counts orders with due dates after the specified date.
     *
     * @param dueDate the date after which orders are due
     * @return the count of orders
     */
    long countByDueDateAfter(LocalDate dueDate);

    /**
     * Counts orders where the customer's full name contains the given search query (case-insensitive).
     *
     * @param searchQuery the search string for the customer's full name
     * @return the count of matching orders
     */
    long countByCustomerFullNameContainingIgnoreCase(String searchQuery);

    /**
     * Counts orders where the customer's full name contains the given search query (case-insensitive)
     * and the due date is after the specified date.
     *
     * @param searchQuery the search string for the customer's full name
     * @param dueDate the date after which orders are due
     * @return the count of matching orders
     */
    long countByCustomerFullNameContainingIgnoreCaseAndDueDateAfter(String searchQuery, LocalDate dueDate);

    /**
     * Counts orders with the specified due date.
     *
     * @param dueDate the due date
     * @return the count of orders
     */
    long countByDueDate(LocalDate dueDate);

    /**
     * Counts orders with the specified due date and in the given collection of states.
     *
     * @param dueDate the due date
     * @param state the collection of order states
     * @return the count of orders
     */
    long countByDueDateAndStateIn(LocalDate dueDate, Collection<OrderState> state);

    /**
     * Counts orders by their state.
     *
     * @param state the order state
     * @return the count of orders
     */
    long countByState(OrderState state);

    /**
     * Counts the number of orders per month for a given state and year.
     *
     * @param orderState the state of the orders
     * @param year the year to filter by
     * @return a list of objects with the month and the count of deliveries
     */
    @Query("SELECT month(dueDate) as month, count(*) as deliveries FROM OrderInfo o where o.state=?1 and year(dueDate)=?2 group by month(dueDate)")
    List<Object[]> countPerMonth(OrderState orderState, int year);

    /**
     * Sums the order values per month for the last three years for a given state and year.
     *
     * @param orderState the state of the orders
     * @param year the maximum year (inclusive)
     * @return a list of objects with the year, month, and sum of deliveries
     */
    @Query("SELECT year(o.dueDate) as y, month(o.dueDate) as m, sum(oi.quantity*p.price) as deliveries FROM OrderInfo o JOIN o.items oi JOIN oi.product p where o.state=?1 and year(o.dueDate)<=?2 AND year(o.dueDate)>?2-3 group by y, m")
    List<Object[]> sumPerMonthLastThreeYears(OrderState orderState, int year);

    /**
     * Counts the number of orders per day for a given state, year, and month.
     *
     * @param orderState the state of the orders
     * @param year the year to filter by
     * @param month the month to filter by
     * @return a list of objects with the day and the count of deliveries
     */
    @Query("SELECT day(dueDate) as day, count(*) as deliveries FROM OrderInfo o where o.state=?1 and year(dueDate)=?2 and month(dueDate)=?3 group by day(dueDate)")
    List<Object[]> countPerDay(OrderState orderState, int year, int month);

    /**
     * Counts the total quantity of each product ordered in a given state, year, and month.
     *
     * @param orderState the state of the orders
     * @param year the year to filter by
     * @param month the month to filter by
     * @return a list of objects with the sum of quantities and the product
     */
    @Query("SELECT sum(oi.quantity), p FROM OrderInfo o JOIN o.items oi JOIN oi.product p WHERE o.state=?1 AND year(o.dueDate)=?2 AND month(o.dueDate)=?3 GROUP BY p.id ORDER BY p.id")
    List<Object[]> countPerProduct(OrderState orderState, int year, int month);

}
