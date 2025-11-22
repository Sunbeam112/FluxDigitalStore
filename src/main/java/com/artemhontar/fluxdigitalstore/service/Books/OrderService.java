package com.artemhontar.fluxdigitalstore.service.Books;

import com.artemhontar.fluxdigitalstore.api.model.User.UserOrderRequest;
import com.artemhontar.fluxdigitalstore.exception.FinancialOperationException;
import com.artemhontar.fluxdigitalstore.exception.PaymentFailedException;
import com.artemhontar.fluxdigitalstore.exception.UserNotExistsException;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.UserOrder;
import com.artemhontar.fluxdigitalstore.repo.OrderRepo;
import com.artemhontar.fluxdigitalstore.service.FinanceService;
import com.artemhontar.fluxdigitalstore.service.User.AuthenticationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;

@Service
public class OrderService {


    private final FinanceService financeService;
    private final AuthenticationService authenticationService;
    private final OrderRepo orderRepo;

    public OrderService(FinanceService financeService, AuthenticationService authenticationService, OrderRepo orderRepo) {
        this.financeService = financeService;
        this.authenticationService = authenticationService;
        this.orderRepo = orderRepo;
    }

    public UserOrder createOrder(UserOrderRequest orderRequest) {
        try {
            String paymentID = financeService.checkout(orderRequest);
            return generateOrder(orderRequest, paymentID);
        } catch (FinancialOperationException e) {
            throw new PaymentFailedException("Payment failed!");
        } catch (IllegalStateException e) {
            throw new UserNotExistsException("No user is found with this credentials");
        }
    }

    private UserOrder generateOrder(UserOrderRequest orderRequest, String paymentID) {

        Optional<LocalUser> opUser = authenticationService.tryGetCurrentUser();
        if (opUser.isPresent()) {
            UserOrder order = new UserOrder();
            order.setOrderItems(orderRequest.getOrderItems());
            order.setDate(new Timestamp(System.currentTimeMillis()));
            order.setPaymentId(paymentID);
            order.setLocalUser(opUser.get());
            return orderRepo.save(order);
        }
        throw new IllegalStateException("User is not logged in!");
    }
}
