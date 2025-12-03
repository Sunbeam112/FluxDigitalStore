package com.artemhontar.fluxdigitalstore.service.Order;

import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.model.DispatchLog;
import com.artemhontar.fluxdigitalstore.model.repo.DispatchLogRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class LogDispatchService {


    private final DispatchLogRepository dispatchLogRepository;

    public LogDispatchService(DispatchLogRepository dispatchLogRepository) {
        this.dispatchLogRepository = dispatchLogRepository;
    }

    public void logDispatch(Book book, int quantity, Long orderId, Long deliveryAddressId) {


        DispatchLog log = new DispatchLog();


        log.setBookId(book.getId());
        log.setBookIsbn(book.getIsbn());
        log.setItemName(book.getTitle());
        log.setDispatchedQuantity(quantity);
        log.setOrderId(orderId);


        log.setDeliveryAddressId(deliveryAddressId);
        log.setDispatchTimestamp(new Timestamp(System.currentTimeMillis()));

        dispatchLogRepository.save(log);
    }


}
