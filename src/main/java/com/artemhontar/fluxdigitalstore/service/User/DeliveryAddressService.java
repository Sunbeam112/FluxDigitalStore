package com.artemhontar.fluxdigitalstore.service.User;

import com.artemhontar.fluxdigitalstore.api.model.User.UserOrderRequest;
import com.artemhontar.fluxdigitalstore.model.DeliveryAddress;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.repo.DeliveryAddressRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing {@link DeliveryAddress} entities, primarily handling
 * retrieval and validation based on ownership for security purposes.
 */
@Service
@Slf4j
public class DeliveryAddressService {


    private final DeliveryAddressRepo deliveryAddressRepo;

    /**
     * Constructs a new DeliveryAddressService with the necessary repository dependency.
     *
     * @param deliveryAddressRepo The repository for accessing delivery address data.
     */
    public DeliveryAddressService(DeliveryAddressRepo deliveryAddressRepo) {
        this.deliveryAddressRepo = deliveryAddressRepo;
    }

    /**
     * Safely retrieves a specific delivery address by its ID, ensuring it belongs to the specified user ID.
     * This method implements the **Authorization by Ownership** pattern.
     *
     * @param addressId The primary key ID of the delivery address to find.
     * @param userId    The ID of the {@link LocalUser} who must own the address.
     * @return An {@link Optional} containing the {@link DeliveryAddress} if found and owned by the user,
     * otherwise {@code Optional.empty()}.
     */
    public Optional<DeliveryAddress> getAddressByIdAndUser(Long addressId, Long userId) {

        return deliveryAddressRepo.findByIdAndLocalUser_Id(addressId, userId);
    }

    /**
     * Determines the appropriate {@link DeliveryAddress} for a new order based on fallback logic.
     * <p>
     * The priority sequence is:
     * 1. **Requested Address:** Use the ID provided in {@code UserOrderRequest} if it exists and belongs to the {@code user}.
     * 2. **Fallback Address:** If no valid ID is provided, use the first address found in the {@code user}'s saved address list.
     * </p>
     *
     * @param orderRequest The request containing potential delivery address information (as {@code DeliveryAddressDTO}).
     * @param user         The authenticated {@link LocalUser} placing the order.
     * @return An {@link Optional} containing the determined {@link DeliveryAddress}. Returns {@code Optional.empty()}
     * if no address is provided in the request and the user has no saved addresses.
     */
    public Optional<DeliveryAddress> getDeliveryAddress(UserOrderRequest orderRequest, LocalUser user) {

        Long requestedAddressId = null;
        Long currentUserId = user.getId();

        if (orderRequest.getDeliveryAddress() != null) {
            requestedAddressId = orderRequest.getDeliveryAddress().getId();
        }

        if (requestedAddressId != null) {
            log.debug("Order request specified Delivery Address ID: {}. Attempting to fetch.", requestedAddressId);

            Optional<DeliveryAddress> opAddress = getAddressByIdAndUser(requestedAddressId, currentUserId);

            if (opAddress.isPresent()) {
                log.debug("Successfully fetched requested address ID: {}.", requestedAddressId);
                return opAddress;
            } else {
                log.warn("Requested address ID {} not found or does not belong to user {}. Falling back to first saved address.", requestedAddressId, currentUserId);
            }
        }

        log.debug("Attempting to get the user's first saved delivery address as fallback.");
        List<DeliveryAddress> addresses = user.getAddresses();

        if (addresses != null && !addresses.isEmpty()) {
            DeliveryAddress address = addresses.get(0);
            log.debug("Assigned user's first address ID: {} as fallback.", address.getId());
            return Optional.of(address);
        }

        log.error("Failed to assign delivery address: No address provided in request and user has no saved addresses.");
        return Optional.empty();
    }
}