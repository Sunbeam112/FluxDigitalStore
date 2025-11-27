package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.model.DispatchLog;
import com.artemhontar.fluxdigitalstore.repo.DispatchLogRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class LogDispatchService {


    private final DispatchLogRepository dispatchLogRepository;

    public LogDispatchService(DispatchLogRepository dispatchLogRepository) {
        this.dispatchLogRepository = dispatchLogRepository;
    }

    void logDispatch(Book book, int quantity, Long orderId, Long deliveryAddressId) {

        // 1. Create the Log Entity
        DispatchLog log = new DispatchLog();

        // 2. Set the archived data (using the Book entity and transaction details)
        log.setBookId(book.getId());
        log.setBookIsbn(book.getIsbn()); // Denormalized field
        log.setItemName(book.getTitle()); // Denormalized field
        log.setDispatchedQuantity(quantity);
        log.setOrderId(orderId);

        // 3. Set the linking and temporal data
        log.setDeliveryAddressId(deliveryAddressId);
        log.setDispatchTimestamp(new Timestamp(System.currentTimeMillis()));

        // 4. Save the permanent record
        dispatchLogRepository.save(log);
    }


}
