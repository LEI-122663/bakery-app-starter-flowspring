package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.entity.Product;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.ProductRepository;

/**
 * Service class for managing {@link Product} entities.
 * <p>
 * Provides CRUD operations and filtering capabilities for Product objects.
 * Implements {@link FilterableCrudService} to provide generic
 * data access methods, and interacts with the {@link ProductRepository}
 * for persistence logic. Handles user-friendly data exception
 * when a product with a duplicate name is saved.
 * </p>
 */
@Service
public class ProductService implements FilterableCrudService<Product> {

    /** Repository for accessing {@link Product} data. */
    private final ProductRepository productRepository;

    /**
     * Constructs a new {@code ProductService} with the given repository.
     *
     * @param productRepository the repository for products
     */
    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Finds a page of {@link Product} entities matching the given filter.
     * If the filter is present, products with names containing the filter string
     * (case-insensitive) are returned. Otherwise, all products are returned.
     *
     * @param filter   an optional string to filter product names
     * @param pageable pagination information
     * @return a page of matching products
     */
    @Override
    public Page<Product> findAnyMatching(Optional<String> filter, Pageable pageable) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return productRepository.findByNameLikeIgnoreCase(repositoryFilter, pageable);
        } else {
            return find(pageable);
        }
    }

    /**
     * Counts the number of {@link Product} entities matching the given filter.
     * If the filter is present, counts products with names containing the filter string
     * (case-insensitive). Otherwise, counts all products.
     *
     * @param filter an optional string to filter product names
     * @return the count of matching products
     */
    @Override
    public long countAnyMatching(Optional<String> filter) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return productRepository.countByNameLikeIgnoreCase(repositoryFilter);
        } else {
            return count();
        }
    }

    /**
     * Finds a page of all {@link Product} entities.
     *
     * @param pageable pagination information
     * @return a page of products
     */
    public Page<Product> find(Pageable pageable) {
        return productRepository.findBy(pageable);
    }

    /**
     * Returns the repository used for CRUD operations on {@link Product} entities.
     *
     * @return the product repository
     */
    @Override
    public JpaRepository<Product, Long> getRepository() {
        return productRepository;
    }

    /**
     * Creates a new {@link Product} entity.
     *
     * @param currentUser the current user (not used in this implementation)
     * @return a new product instance
     */
    @Override
    public Product createNew(User currentUser) {
        return new Product();
    }

    /**
     * Saves the given {@link Product} entity.
     * <p>
     * If a product with the same name already exists, throws a
     * {@link UserFriendlyDataException} with an appropriate message.
     *
     * @param currentUser the current user (not used in this implementation)
     * @param entity      the product entity to save
     * @return the saved product
     * @throws UserFriendlyDataException if a duplicate product name exists
     */
    @Override
    public Product save(User currentUser, Product entity) {
        try {
            return FilterableCrudService.super.save(currentUser, entity);
        } catch (DataIntegrityViolationException e) {
            throw new UserFriendlyDataException(
                    "There is already a product with that name. Please select a unique name for the product.");
        }

    }

}
