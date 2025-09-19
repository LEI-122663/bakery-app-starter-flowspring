package com.vaadin.starter.bakery.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.DashboardData;
import com.vaadin.starter.bakery.backend.data.DeliveryStats;
import com.vaadin.starter.bakery.backend.data.OrderState;
import com.vaadin.starter.bakery.backend.data.entity.Order;
import com.vaadin.starter.bakery.backend.data.entity.OrderSummary;
import com.vaadin.starter.bakery.backend.data.entity.Product;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.OrderRepository;

/**
 * Serviço responsável por operações relacionadas com encomendas.
 * Fornece métodos para criar, salvar, consultar e atualizar encomendas,
 * além de gerar estatísticas e dados do dashboard.
 *
 * Implementa {@link CrudService} para operações CRUD genéricas.
 */
@Service
public class OrderService implements CrudService<Order> {

	private final OrderRepository orderRepository;

	/**
	 * Construtor do serviço de encomendas.
	 *
	 * @param orderRepository Repositório JPA para acesso aos dados das encomendas.
	 */
	@Autowired
	public OrderService(OrderRepository orderRepository) {
		super();
		this.orderRepository = orderRepository;
	}

	/**
	 * Conjunto de estados de encomenda que não estão disponíveis para algumas operações.
	 */
	private static final Set<OrderState> notAvailableStates = Collections.unmodifiableSet(
			EnumSet.complementOf(EnumSet.of(OrderState.DELIVERED, OrderState.READY, OrderState.CANCELLED)));

	/**
	 * Salva uma encomenda existente ou cria uma nova, preenchida pelo {@link BiConsumer} fornecido.
	 *
	 * @param currentUser O utilizador que está a realizar a operação.
	 * @param id ID da encomenda a atualizar; se for {@code null}, será criada uma nova encomenda.
	 * @param orderFiller Função que preenche os dados da encomenda.
	 * @return A encomenda salva no repositório.
	 */
	@Transactional(rollbackOn = Exception.class)
	public Order saveOrder(User currentUser, Long id, BiConsumer<User, Order> orderFiller) {
		Order order;
		if (id == null) {
			order = new Order(currentUser);
		} else {
			order = load(id);
		}
		orderFiller.accept(currentUser, order);
		return orderRepository.save(order);
	}

	/**
	 * Salva diretamente uma encomenda existente no repositório.
	 *
	 * @param order A encomenda a salvar.
	 * @return A encomenda salva.
	 */
	@Transactional(rollbackOn = Exception.class)
	public Order saveOrder(Order order) {
		return orderRepository.save(order);
	}

	/**
	 * Adiciona um comentário à encomenda, registando a ação no histórico.
	 *
	 * @param currentUser O utilizador que adiciona o comentário.
	 * @param order A encomenda à qual adicionar o comentário.
	 * @param comment Texto do comentário.
	 * @return A encomenda atualizada com o comentário.
	 */
	@Transactional(rollbackOn = Exception.class)
	public Order addComment(User currentUser, Order order, String comment) {
		order.addHistoryItem(currentUser, comment);
		return orderRepository.save(order);
	}

	/**
	 * Pesquisa encomendas após a data de vencimento opcional, filtrando por texto parcial se fornecido.
	 *
	 * @param optionalFilter Filtro de texto parcial no nome do cliente.
	 * @param optionalFilterDate Filtro de data mínima de vencimento.
	 * @param pageable Objeto de paginação para limitar resultados.
	 * @return Página de encomendas que correspondem aos critérios.
	 */
	public Page<Order> findAnyMatchingAfterDueDate(Optional<String> optionalFilter,
												   Optional<LocalDate> optionalFilterDate,
												   Pageable pageable) {
		if (optionalFilter.isPresent() && !optionalFilter.get().isEmpty()) {
			if (optionalFilterDate.isPresent()) {
				return orderRepository.findByCustomerFullNameContainingIgnoreCaseAndDueDateAfter(
						optionalFilter.get(), optionalFilterDate.get(), pageable);
			} else {
				return orderRepository.findByCustomerFullNameContainingIgnoreCase(optionalFilter.get(), pageable);
			}
		} else {
			if (optionalFilterDate.isPresent()) {
				return orderRepository.findByDueDateAfter(optionalFilterDate.get(), pageable);
			} else {
				return orderRepository.findAll(pageable);
			}
		}
	}

	/**
	 * Retorna todas as encomendas que começam hoje ou posteriormente, como sumário.
	 *
	 * @return Lista de {@link OrderSummary} a partir de hoje.
	 */
	@Transactional
	public List<OrderSummary> findAnyMatchingStartingToday() {
		return orderRepository.findByDueDateGreaterThanEqual(LocalDate.now());
	}

	/**
	 * Conta o número de encomendas após a data de vencimento opcional, com filtro de nome do cliente opcional.
	 *
	 * @param optionalFilter Filtro de texto parcial no nome do cliente.
	 * @param optionalFilterDate Filtro de data mínima de vencimento.
	 * @return Número total de encomendas que correspondem aos critérios.
	 */
	public long countAnyMatchingAfterDueDate(Optional<String> optionalFilter, Optional<LocalDate> optionalFilterDate) {
		if (optionalFilter.isPresent() && optionalFilterDate.isPresent()) {
			return orderRepository.countByCustomerFullNameContainingIgnoreCaseAndDueDateAfter(optionalFilter.get(),
					optionalFilterDate.get());
		} else if (optionalFilter.isPresent()) {
			return orderRepository.countByCustomerFullNameContainingIgnoreCase(optionalFilter.get());
		} else if (optionalFilterDate.isPresent()) {
			return orderRepository.countByDueDateAfter(optionalFilterDate.get());
		} else {
			return orderRepository.count();
		}
	}

	/**
	 * Gera estatísticas de entregas para o dashboard, incluindo encomendas de hoje, amanhã,
	 * entregues e não disponíveis.
	 *
	 * @return Objeto {@link DeliveryStats} com estatísticas atuais.
	 */
	private DeliveryStats getDeliveryStats() {
		DeliveryStats stats = new DeliveryStats();
		LocalDate today = LocalDate.now();
		stats.setDueToday((int) orderRepository.countByDueDate(today));
		stats.setDueTomorrow((int) orderRepository.countByDueDate(today.plusDays(1)));
		stats.setDeliveredToday((int) orderRepository.countByDueDateAndStateIn(today,
				Collections.singleton(OrderState.DELIVERED)));

		stats.setNotAvailableToday((int) orderRepository.countByDueDateAndStateIn(today, notAvailableStates));
		stats.setNewOrders((int) orderRepository.countByState(OrderState.NEW));

		return stats;
	}

	/**
	 * Retorna os dados do dashboard para um determinado mês e ano,
	 * incluindo estatísticas de entregas, vendas por mês e por produto.
	 *
	 * @param month Mês (1-12) para filtrar entregas.
	 * @param year Ano para filtrar entregas.
	 * @return Objeto {@link DashboardData} com todos os dados agregados.
	 */
	public DashboardData getDashboardData(int month, int year) {
		DashboardData data = new DashboardData();
		data.setDeliveryStats(getDeliveryStats());
		data.setDeliveriesThisMonth(getDeliveriesPerDay(month, year));
		data.setDeliveriesThisYear(getDeliveriesPerMonth(year));

		Number[][] salesPerMonth = new Number[3][12];
		data.setSalesPerMonth(salesPerMonth);
		List<Object[]> sales = orderRepository.sumPerMonthLastThreeYears(OrderState.DELIVERED, year);

		for (Object[] salesData : sales) {
			int y = year - (int) salesData[0];
			int m = (int) salesData[1] - 1;
			if (y == 0 && m == month - 1) {
				continue; // skip current month as it contains incomplete data
			}
			long count = (long) salesData[2];
			salesPerMonth[y][m] = count;
		}

		LinkedHashMap<Product, Integer> productDeliveries = new LinkedHashMap<>();
		data.setProductDeliveries(productDeliveries);
		for (Object[] result : orderRepository.countPerProduct(OrderState.DELIVERED, year, month)) {
			int sum = ((Long) result[0]).intValue();
			Product p = (Product) result[1];
			productDeliveries.put(p, sum);
		}

		return data;
	}

	/**
	 * Cria uma lista com contagem de entregas por dia no mês especificado,
	 * preenchendo dias sem entregas com {@code null}.
	 *
	 * @param month Mês (1-12).
	 * @param year Ano.
	 * @return Lista de {@link Number} representando entregas diárias.
	 */
	private List<Number> getDeliveriesPerDay(int month, int year) {
		int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
		return flattenAndReplaceMissingWithNull(daysInMonth,
				orderRepository.countPerDay(OrderState.DELIVERED, year, month));
	}

	/**
	 * Cria uma lista com contagem de entregas por mês no ano especificado,
	 * preenchendo meses sem entregas com {@code null}.
	 *
	 * @param year Ano.
	 * @return Lista de {@link Number} representando entregas mensais.
	 */
	private List<Number> getDeliveriesPerMonth(int year) {
		return flattenAndReplaceMissingWithNull(12, orderRepository.countPerMonth(OrderState.DELIVERED, year));
	}

	/**
	 * Preenche uma lista de tamanho fixo com contagens extraídas de {@code list},
	 * substituindo valores ausentes por {@code null}.
	 *
	 * @param length Tamanho da lista final.
	 * @param list Lista de Object[] contendo índice e valor.
	 * @return Lista de {@link Number} com valores preenchidos.
	 */
	private List<Number> flattenAndReplaceMissingWithNull(int length, List<Object[]> list) {
		List<Number> counts = new ArrayList<>();
		for (int i = 0; i < length; i++) {
			counts.add(null);
		}

		for (Object[] result : list) {
			counts.set((Integer) result[0] - 1, (Number) result[1]);
		}
		return counts;
	}

	/**
	 * Retorna o repositório JPA associado à entidade {@link Order}.
	 *
	 * @return Repositório JPA de encomendas.
	 */
	@Override
	public JpaRepository<Order, Long> getRepository() {
		return orderRepository;
	}

	/**
	 * Cria uma nova encomenda com valores padrão de data e hora de entrega.
	 *
	 * @param currentUser Utilizador que cria a encomenda.
	 * @return Nova instância de {@link Order}.
	 */
	@Override
	@Transactional
	public Order createNew(User currentUser) {
		Order order = new Order(currentUser);
		order.setDueTime(LocalTime.of(16, 0));
		order.setDueDate(LocalDate.now());
		return order;
	}

}