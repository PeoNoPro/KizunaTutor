package com.kizuna.support;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.ticket.service.SupportTicketValidation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class SupportTicketValidationTest {
 @Test void rejectsTextWithoutVisibleCharacters() {
   for(String blank:List.of(" \t\n"," ","　  ","​‍﻿"))assertThrows(BusinessException.class,()->SupportTicketValidation.text(blank,100));
   assertEquals("Nội dung",SupportTicketValidation.text("  Nội dung ",100));
   assertEquals("😀",SupportTicketValidation.text("😀",1));
   assertThrows(BusinessException.class,()->SupportTicketValidation.text("😀😀",1));
 }
}
