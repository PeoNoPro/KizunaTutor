package com.kizuna.features.ticket.dto;
import java.util.UUID;
public record SupportTicketActor(UUID userId,String role,String displayName) {
 public boolean requester(){return role.equals("ROLE_LEARNER") || role.equals("ROLE_TUTOR");}
 public boolean cs(){return role.equals("ROLE_CS_STAFF");}
 public boolean manager(){return role.equals("ROLE_MANAGER");}
 public String viewRole(){return cs()?"cs":manager()?"manager":role.equals("ROLE_TUTOR")?"tutor":"learner";}
}
