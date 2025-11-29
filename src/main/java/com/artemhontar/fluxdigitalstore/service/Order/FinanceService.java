package com.artemhontar.fluxdigitalstore.service.Order;

import com.artemhontar.fluxdigitalstore.api.model.User.UserOrderRequest;
import com.artemhontar.fluxdigitalstore.exception.FinancialOperationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FinanceService {


    public String checkout(UserOrderRequest orderRequest) throws FinancialOperationException {
        //TODO: add user wallet and money check
        return UUID.randomUUID().toString();
    }
}
