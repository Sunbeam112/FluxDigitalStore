package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.Order.DeliveryAddressDTO;
import com.artemhontar.fluxdigitalstore.api.model.User.UserOrderRequest;
import com.artemhontar.fluxdigitalstore.model.DeliveryAddress;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.repo.DeliveryAddressRepo;
import com.artemhontar.fluxdigitalstore.service.User.DeliveryAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryAddressServiceTest {

    @Mock
    private DeliveryAddressRepo deliveryAddressRepo;

    @InjectMocks
    private DeliveryAddressService deliveryAddressService;

    // --- Mock Data ---
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_ADDRESS_ID_1 = 101L;
    private final Long TEST_ADDRESS_ID_2 = 102L;
    private final Long OTHER_USER_ID = 2L;

    private LocalUser mockUser;
    private DeliveryAddress address1;
    private DeliveryAddress address2;
    private UserOrderRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Setup User
        mockUser = new LocalUser();
        mockUser.setId(TEST_USER_ID);

        // Setup Addresses
        address1 = new DeliveryAddress();
        address1.setId(TEST_ADDRESS_ID_1);
        address1.setLocalUser(mockUser);

        address2 = new DeliveryAddress();
        address2.setId(TEST_ADDRESS_ID_2);
        address2.setLocalUser(mockUser);

        // Setup Request with null address initially
        mockRequest = new UserOrderRequest();
        mockRequest.setOrderItems(Collections.emptyList());
    }

    // ===================================
    // TEST: getAddressByIdAndUser (Authorization by Ownership)
    // ===================================

    @Test
    void getAddressByIdAndUser_AddressFoundAndOwned_ReturnsOptionalAddress() {
        // Arrange
        when(deliveryAddressRepo.findByIdAndLocalUser_Id(TEST_ADDRESS_ID_1, TEST_USER_ID))
                .thenReturn(Optional.of(address1));

        // Act
        Optional<DeliveryAddress> result = deliveryAddressService.getAddressByIdAndUser(TEST_ADDRESS_ID_1, TEST_USER_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_ADDRESS_ID_1, result.get().getId());
        verify(deliveryAddressRepo, times(1)).findByIdAndLocalUser_Id(TEST_ADDRESS_ID_1, TEST_USER_ID);
    }

    @Test
    void getAddressByIdAndUser_AddressFoundButNotOwned_ReturnsEmptyOptional() {
        // Arrange
        when(deliveryAddressRepo.findByIdAndLocalUser_Id(TEST_ADDRESS_ID_1, TEST_USER_ID))
                .thenReturn(Optional.empty()); // Repo returns empty because the user ID doesn't match

        // Act
        Optional<DeliveryAddress> result = deliveryAddressService.getAddressByIdAndUser(TEST_ADDRESS_ID_1, TEST_USER_ID);

        // Assert
        assertTrue(result.isEmpty());
        verify(deliveryAddressRepo, times(1)).findByIdAndLocalUser_Id(TEST_ADDRESS_ID_1, TEST_USER_ID);
    }

    // ===================================
    // TEST: getDeliveryAddress (Fallback Logic)
    // ===================================

    @Test
    void getDeliveryAddress_RequestedAddressFoundAndOwned_ReturnsRequestedAddress() {
        // Arrange
        DeliveryAddressDTO requestedDto = DeliveryAddressDTO.builder().id(TEST_ADDRESS_ID_2).build();
        mockRequest.setDeliveryAddress(requestedDto);

        // Mock Step 1: getAddressByIdAndUser succeeds
        when(deliveryAddressRepo.findByIdAndLocalUser_Id(TEST_ADDRESS_ID_2, TEST_USER_ID))
                .thenReturn(Optional.of(address2));

        // Act
        Optional<DeliveryAddress> result = deliveryAddressService.getDeliveryAddress(mockRequest, mockUser);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_ADDRESS_ID_2, result.get().getId());
        verify(deliveryAddressRepo, times(1)).findByIdAndLocalUser_Id(TEST_ADDRESS_ID_2, TEST_USER_ID);
        // Verify no fallback attempt (user.getAddresses() is not called implicitly)
    }

    @Test
    void getDeliveryAddress_RequestedAddressNotOwned_FallsBackToFirstSavedAddress() {
        // Arrange
        DeliveryAddressDTO requestedDto = DeliveryAddressDTO.builder().id(TEST_ADDRESS_ID_2).build();
        mockRequest.setDeliveryAddress(requestedDto);

        // Setup User's saved addresses for fallback
        mockUser.setAddresses(List.of(address1, address2)); // Address 1 (ID 101) is the fallback

        // Mock Step 1: getAddressByIdAndUser fails (returns empty, simulated by not being owned)
        when(deliveryAddressRepo.findByIdAndLocalUser_Id(TEST_ADDRESS_ID_2, TEST_USER_ID))
                .thenReturn(Optional.empty());

        // Act
        Optional<DeliveryAddress> result = deliveryAddressService.getDeliveryAddress(mockRequest, mockUser);

        // Assert
        assertTrue(result.isPresent());
        // Should return address1 (ID 101) as the fallback
        assertEquals(TEST_ADDRESS_ID_1, result.get().getId());
        verify(deliveryAddressRepo, times(1)).findByIdAndLocalUser_Id(TEST_ADDRESS_ID_2, TEST_USER_ID);
    }

    @Test
    void getDeliveryAddress_NoIdInRequest_FallsBackToFirstSavedAddress() {
        // Arrange
        DeliveryAddressDTO requestedDto = DeliveryAddressDTO.builder().id(null).build();
        mockRequest.setDeliveryAddress(requestedDto);

        // Setup User's saved addresses for fallback
        mockUser.setAddresses(List.of(address2, address1)); // Address 2 (ID 102) is the fallback

        // Act
        Optional<DeliveryAddress> result = deliveryAddressService.getDeliveryAddress(mockRequest, mockUser);

        // Assert
        assertTrue(result.isPresent());
        // Should return address2 (ID 102) as the fallback
        assertEquals(TEST_ADDRESS_ID_2, result.get().getId());
        // Verify that the repository lookup was skipped because ID was null
        verify(deliveryAddressRepo, never()).findByIdAndLocalUser_Id(anyLong(), anyLong());
    }

    @Test
    void getDeliveryAddress_NoAddressFound_ReturnsEmptyOptional() {
        // Arrange
        // 1. Request has no ID (or ID is invalid/not owned)
        DeliveryAddressDTO requestedDto = DeliveryAddressDTO.builder().id(null).build();
        mockRequest.setDeliveryAddress(requestedDto);

        // 2. User has no saved addresses (null or empty list)
        mockUser.setAddresses(Collections.emptyList());

        // Act
        Optional<DeliveryAddress> result = deliveryAddressService.getDeliveryAddress(mockRequest, mockUser);

        // Assert
        assertTrue(result.isEmpty());
        verify(deliveryAddressRepo, never()).findByIdAndLocalUser_Id(anyLong(), anyLong());
    }

    @Test
    void getDeliveryAddress_NullRequestAddress_FallsBackToFirstSavedAddress() {
        // Arrange
        mockRequest.setDeliveryAddress(null); // No address DTO in the request at all

        // Setup User's saved addresses for fallback
        mockUser.setAddresses(List.of(address1));

        // Act
        Optional<DeliveryAddress> result = deliveryAddressService.getDeliveryAddress(mockRequest, mockUser);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_ADDRESS_ID_1, result.get().getId());
        verify(deliveryAddressRepo, never()).findByIdAndLocalUser_Id(anyLong(), anyLong());
    }
}