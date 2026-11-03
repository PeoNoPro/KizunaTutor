package com.kizuna.support;
import com.kizuna.features.ticket.service.SupportTicketAccessPolicy;
import com.kizuna.features.ticket.dto.SupportTicketActor;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SupportTicketPolicyTest {
 @Test void ownerAndCurrentRecipientOnly() {
   UUID owner=UUID.randomUUID(),cs=UUID.randomUUID(),manager=UUID.randomUUID();
   var ticket=new HashMap<String,Object>();ticket.put("user_id",owner);ticket.put("assigned_to",cs);ticket.put("escalated_to",manager);ticket.put("status","ESCALATED");
   var policy=new SupportTicketAccessPolicy();
   assertTrue(policy.canRead(new SupportTicketActor(owner,"ROLE_LEARNER","Owner"),ticket));
   assertFalse(policy.canRead(new SupportTicketActor(UUID.randomUUID(),"ROLE_LEARNER","Other"),ticket));
   assertTrue(policy.canRead(new SupportTicketActor(manager,"ROLE_MANAGER","Manager"),ticket));
   assertFalse(policy.canRead(new SupportTicketActor(UUID.randomUUID(),"ROLE_MANAGER","Other"),ticket));
   assertFalse(policy.canRead(new SupportTicketActor(UUID.randomUUID(),"ROLE_ADMIN","Admin"),ticket));
   assertFalse(policy.actions(new SupportTicketActor(manager,"ROLE_MANAGER","Manager"),ticket).contains("message"));
   ticket.put("status","RESOLVED");
   assertTrue(policy.actions(new SupportTicketActor(owner,"ROLE_LEARNER","Owner"),ticket).contains("close"));
   assertFalse(policy.actions(new SupportTicketActor(cs,"ROLE_CS_STAFF","CS"),ticket).contains("close"));
 }
}
