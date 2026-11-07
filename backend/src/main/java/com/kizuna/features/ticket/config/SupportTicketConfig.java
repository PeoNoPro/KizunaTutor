package com.kizuna.features.ticket.config;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
@Configuration
public class SupportTicketConfig {
 @Bean("supportShiftClock") public java.time.Clock supportShiftClock(){return java.time.Clock.system(com.kizuna.features.ticket.service.SupportShiftService.ZONE);}
 @Bean("ticketJdbc") public JdbcTemplate ticketJdbc(DataSource dataSource){var jdbc=new JdbcTemplate(dataSource);jdbc.setQueryTimeout(5);return jdbc;}
}
