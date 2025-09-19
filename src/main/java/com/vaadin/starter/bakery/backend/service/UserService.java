package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.UserRepository;

/**
 * Service class that provides CRUD operations and filtering capabilities for {@link User} entities.
 * It applies validation rules such as preventing modifications of locked users
 * and disallowing deletion of the current logged-in user.
 */
@Service
public class UserService implements FilterableCrudService<User> {

    /** Exception message when a locked user is modified or deleted. */
    public static final String MODIFY_LOCKED_USER_NOT_PERMITTED = "User has been locked and cannot be modified or deleted";

    /** Exception message when a user attempts to delete their own account. */
    private static final String DELETING_SELF_NOT_PERMITTED = "You cannot delete your own account";

    private final UserRepository userRepository;

    /**
     * Constructs a {@link UserService} with the given {@link UserRepository}.
     *
     * @param userRepository repository used to access {@link User} data
     */
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Finds users matching the given filter string across multiple fields
     * (email, first name, last name, role). If no filter is provided, all users are returned.
     *
     * @param filter   optional search filter
     * @param pageable pagination information
     * @return a page of matching users
     */
    public Page<User> findAnyMatching(Optional<String> filter, Pageable pageable) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return getRepository()
                    .findByEmailLikeIgnoreCaseOrFirstNameLikeIgnoreCaseOrLastNameLikeIgnoreCaseOrRoleLikeIgnoreCase(
                            repositoryFilter, repositoryFilter, repositoryFilter, repositoryFilter, pageable);
        } else {
            return find(pageable);
        }
    }

    /**
     * Counts the number of users that match the given filter across multiple fields.
     * If no filter is provided, counts all users.
     *
     * @param filter optional search filter
     * @return number of matching users
     */
    @Override
    public long countAnyMatching(Optional<String> filter) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return userRepository.countByEmailLikeIgnoreCaseOrFirstNameLikeIgnoreCaseOrLastNameLikeIgnoreCaseOrRoleLikeIgnoreCase(
                    repositoryFilter, repositoryFilter, repositoryFilter, repositoryFilter);
        } else {
            return count();
        }
    }

    /**
     * Returns the underlying {@link UserRepository}.
     *
     * @return repository instance
     */
    @Override
    public UserRepository getRepository() {
        return userRepository;
    }

    /**
     * Retrieves all users with pagination.
     *
     * @param pageable pagination information
     * @return a page of users
     */
    public Page<User> find(Pageable pageable) {
        return getRepository().findBy(pageable);
    }

    /**
     * Saves a user entity after validating that it is not locked.
     *
     * @param currentUser the user performing the save operation
     * @param entity      the user entity to save
     * @return the saved user entity
     * @throws UserFriendlyDataException if the user is locked
     */
    @Override
    public User save(User currentUser, User entity) {
        throwIfUserLocked(entity);
        return getRepository().saveAndFlush(entity);
    }

    /**
     * Deletes a user entity, ensuring the current user cannot delete themselves
     * and that locked users cannot be deleted.
     *
     * @param currentUser   the user performing the delete operation
     * @param userToDelete  the user to delete
     * @throws UserFriendlyDataException if the user is locked or if attempting to delete self
     */
    @Override
    @Transactional
    public void delete(User currentUser, User userToDelete) {
        throwIfDeletingSelf(currentUser, userToDelete);
        throwIfUserLocked(userToDelete);
        FilterableCrudService.super.delete(currentUser, userToDelete);
    }

    /**
     * Throws an exception if the current user attempts to delete their own account.
     *
     * @param currentUser the user performing the operation
     * @param user        the user being deleted
     * @throws UserFriendlyDataException if deleting self
     */
    private void throwIfDeletingSelf(User currentUser, User user) {
        if (currentUser.equals(user)) {
            throw new UserFriendlyDataException(DELETING_SELF_NOT_PERMITTED);
        }
    }

    /**
     * Throws an exception if the given user is locked.
     *
     * @param entity the user being modified
     * @throws UserFriendlyDataException if the user is locked
     */
    private void throwIfUserLocked(User entity) {
        if (entity != null && entity.isLocked()) {
            throw new UserFriendlyDataException(MODIFY_LOCKED_USER_NOT_PERMITTED);
        }
    }

    /**
     * Creates a new instance of {@link User}.
     *
     * @param currentUser the user performing the operation
     * @return a new user entity
     */
    @Override
    public User createNew(User currentUser) {
        return new User();
    }

}
