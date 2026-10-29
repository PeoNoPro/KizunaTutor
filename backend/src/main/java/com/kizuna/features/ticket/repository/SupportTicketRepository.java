package com.kizuna.features.ticket.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.ticket.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
}
