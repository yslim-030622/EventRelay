package com.eventrelay.repository;

import com.eventrelay.model.EventDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventDeliveryRepository extends JpaRepository<EventDelivery, Long> {
}
