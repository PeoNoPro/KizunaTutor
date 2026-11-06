package com.kizuna.support;
import org.springframework.context.annotation.*;
/** Test classpath only: a ticket-test profile must never fall back to an application DB. */
@Configuration @Profile("ticket-test")
public class TicketTestProfileGuard {
 @Bean public static org.springframework.beans.factory.config.BeanFactoryPostProcessor ticketTestDatabaseGuard(){
   return factory->SupportTicketTestEnvironment.dataSource();
 }
}
