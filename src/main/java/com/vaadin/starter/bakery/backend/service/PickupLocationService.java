package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.entity.PickupLocation;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.PickupLocationRepository;

/**
 * Service class that provides CRUD operations and filtering capabilities
 * for {@link PickupLocation} entities. It allows searching by name, counting
 * matches, retrieving the default pickup location, and creating new instances.
 */
@Service
public class PickupLocationService implements FilterableCrudService<PickupLocation> {

    private final PickupLocationRepository pickupLocationRepository;

    /**
     * Constructs a {@link PickupLocationService} with the given repository.
     *
     * @param pickupLocationRepository repository used to access {@link PickupLocation} data
     */
    @Autowired
    public PickupLocationService(PickupLocationRepository pickupLocationRepository) {
        this.pickupLocationRepository = pickupLocationRepository;
    }

    /**
     * Finds pickup locations matching the given filter string by name.
     * If no filter is provided, all pickup locations are returned.
     *
     * @param filter   optional search filter for the location name
     * @param pageable pagination information
     * @return a page of matching pickup locations
     */
    public Page<PickupLocation> findAnyMatching(Optional<String> filter, Pageable pageable) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return pickupLocationRepository.findByNameLikeIgnoreCase(repositoryFilter, pageable);
        } else {
            return pickupLocationRepository.findAll(pageable);
        }
    }

    /**
     * Counts the number of pickup locations that match the given filter by name.
     * If no filter is provided, counts all pickup locations.
     *
     * @param filter optional search filter for the location name
     * @return number of matching pickup locations
     */
    public long countAnyMatching(Optional<String> filter) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return pickupLocationRepository.countByNameLikeIgnoreCase(repositoryFilter);
        } else {
            return pickupLocationRepository.count();
        }
    }

    /**
     * Retrieves the default pickup location, which is the first available
     * when no filter is applied.
     *
     * @return the default {@link PickupLocation}
     */
    public PickupLocation getDefault() {
        return findAnyMatching(Optional.empty(), PageRequest.of(0, 1)).iterator().next();
    }

    /**
     * Returns the underlying {@link PickupLocationRepository}.
     *
     * @return repository instance
     */
    @Override
    public JpaRepository<PickupLocation, Long> getRepository() {
        return pickupLocationRepository;
    }

    /**
     * Creates a new instance of {@link PickupLocation}.
     *
     * @param currentUser the user performing the operation
     * @return a new pickup location entity
     */
    @Override
    public PickupLocation createNew(User currentUser) {
        return new PickupLocation();
    }
}
