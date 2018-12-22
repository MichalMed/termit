package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.event.LoginAttemptsThresholdExceeded;
import cz.cvut.kbss.termit.exception.AuthorizationException;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.rest.dto.UserUpdateDto;
import cz.cvut.kbss.termit.service.repository.UserRepositoryService;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * User account-related business logic.
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepositoryService repositoryService;

    private final SecurityUtils securityUtils;

    @Autowired
    public UserService(UserRepositoryService repositoryService, SecurityUtils securityUtils) {
        this.repositoryService = repositoryService;
        this.securityUtils = securityUtils;
    }

    /**
     * Gets accounts of all users in the system.
     *
     * @return List of user accounts
     */
    public List<UserAccount> findAll() {
        return repositoryService.findAll();
    }

    /**
     * Finds a user with the specified id.
     *
     * @param id User identifier
     * @return Matching user wrapped in an {@code Optional}
     */
    public Optional<UserAccount> find(URI id) {
        return repositoryService.find(id);
    }

    /**
     * Persists the specified user account.
     *
     * @param account Account to save
     */
    @Transactional
    public void persist(UserAccount account) {
        LOG.trace("Persisting user account {}.", account);
        repositoryService.persist(account);
    }

    /**
     * Updates current user's account with the specified update data.
     * <p>
     * If the update contains also password update, the original password specified in the update object has to match
     * current user's password.
     *
     * @param update Account update data
     * @throws AuthorizationException If the update data concern other than the current user
     */
    @Transactional
    public void updateCurrent(UserUpdateDto update) {
        LOG.trace("Updating current user account.");
        Objects.requireNonNull(update);
        if (!securityUtils.getCurrentUser().getUri().equals(update.getUri())) {
            throw new AuthorizationException(
                    "User " + securityUtils.getCurrentUser() + " attempted to update a different user's account.");
        }
        if (update.getPassword() != null) {
            securityUtils.verifyCurrentUserPassword(update.getOriginalPassword());
        }
        repositoryService.update(update.asUserAccount());
    }

    /**
     * Unlocks the specified user account.
     * <p>
     * The specified password is set as the new password of the user account.
     *
     * @param account     Account to unlock
     * @param newPassword New password for the unlocked account
     */
    @Transactional
    public void unlock(UserAccount account, String newPassword) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(newPassword);
        LOG.trace("Unlocking user account {}.", account);
        account.unlock();
        account.setPassword(newPassword);
        repositoryService.update(account);
    }

    /**
     * Disables the specified user account.
     *
     * @param account Account to disable
     */
    @Transactional
    public void disable(UserAccount account) {
        Objects.requireNonNull(account);
        LOG.trace("Disabling user account {}.", account);
        account.disable();
        repositoryService.update(account);
    }

    /**
     * Enables the specified user account.
     *
     * @param account Account to enable
     */
    @Transactional
    public void enable(UserAccount account) {
        Objects.requireNonNull(account);
        LOG.trace("Enabling user account {}.", account);
        account.enable();
        repositoryService.update(account);
    }

    /**
     * Locks user account when unsuccessful login attempts limit is exceeded.
     * <p>
     * This is an application event listener and should not be called directly.
     *
     * @param event The event emitted when login attempts limit is exceeded
     */
    @Transactional
    @EventListener
    public void onLoginAttemptsThresholdExceeded(LoginAttemptsThresholdExceeded event) {
        Objects.requireNonNull(event);
        final UserAccount account = event.getUser();
        LOG.trace("Locking user account {} due to exceeding unsuccessful login attempts limit.", account);
        account.lock();
        repositoryService.update(account);
    }

    /**
     * Checks whether a user account with the specified username exists in the repository.
     *
     * @param username Username to check
     * @return Whether username already exists
     */
    public boolean exists(String username) {
        return repositoryService.exists(username);
    }
}