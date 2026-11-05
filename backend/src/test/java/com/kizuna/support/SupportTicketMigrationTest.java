package com.kizuna.support;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SupportTicketMigrationTest {
    @Test void refusesOrphanAndUnverifiableEscalation()throws Exception{
        for(String invalid:java.util.List.of("INSERT INTO ticket_messages(message_id,ticket_id,sender_id,message_text) VALUES(gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),'Orphan')","INSERT INTO support_tickets(ticket_id,user_id,subject,is_escalated,status) VALUES(gen_random_uuid(),gen_random_uuid(),'Legacy',true,'ESCALATED')")){
            try(var c=SupportTicketTestEnvironment.dataSource().getConnection()){
                c.setAutoCommit(false);String schema="invalid_"+UUID.randomUUID().toString().replace("-","");
                try{c.createStatement().execute("CREATE SCHEMA "+schema+";SET LOCAL search_path TO "+schema+",public;CREATE TABLE users(user_id uuid PRIMARY KEY);CREATE TABLE support_tickets(ticket_id uuid PRIMARY KEY,user_id uuid,assigned_to uuid,escalated_to uuid,is_escalated boolean DEFAULT false,status text DEFAULT 'OPEN',subject text);CREATE TABLE ticket_messages(message_id uuid,ticket_id uuid,sender_id uuid,message_text text,attachment_url text,sent_at timestamptz);");c.createStatement().execute(invalid);
                    String sql=Files.readString(Path.of("../database/seeds/fe10_support_ticket.sql")).replace("BEGIN;","").replace("COMMIT;","");assertThrows(SQLException.class,()->c.createStatement().execute(sql));
                }finally{c.rollback();}
            }
        }
    }
    @Test void preservesLegacyMessagesAndIsIdempotent() throws Exception {
        try(var c=SupportTicketTestEnvironment.dataSource().getConnection()) {
            c.setAutoCommit(false);
            String schema="migration_"+UUID.randomUUID().toString().replace("-","");
            try {
                c.createStatement().execute("CREATE SCHEMA "+schema+"; SET LOCAL search_path TO "+schema+",public; CREATE TABLE users(user_id uuid PRIMARY KEY); CREATE TABLE support_tickets(ticket_id uuid PRIMARY KEY,user_id uuid,assigned_to uuid,escalated_to uuid,is_escalated boolean DEFAULT false,escalation_notes text,status text DEFAULT 'OPEN',subject varchar(200),created_at timestamptz DEFAULT now(),updated_at timestamptz DEFAULT now(),category text,priority text); CREATE TABLE ticket_messages(message_id uuid PRIMARY KEY,ticket_id uuid,sender_id uuid,message_text text,attachment_url text,sent_at timestamptz DEFAULT now());");
                UUID user=UUID.randomUUID(),ticket=UUID.randomUUID();
                c.createStatement().execute("INSERT INTO users VALUES ('"+user+"'); INSERT INTO support_tickets(ticket_id,user_id,subject) VALUES ('"+ticket+"','"+user+"','Legacy'); INSERT INTO ticket_messages SELECT gen_random_uuid(),'"+ticket+"','"+user+"','Message',null,now() FROM generate_series(1,3)");
                String sql=Files.readString(Path.of("../database/seeds/fe10_support_ticket.sql")).replace("BEGIN;","").replace("COMMIT;","");
                c.createStatement().execute(sql); c.createStatement().execute(sql);
                var rs=c.createStatement().executeQuery("SELECT count(*),count(DISTINCT sequence_no),min(sequence_no),max(sequence_no) FROM ticket_messages"); rs.next();
                assertEquals(3,rs.getInt(1));assertEquals(3,rs.getInt(2));assertEquals(1,rs.getLong(3));assertEquals(3,rs.getLong(4));
                c.createStatement().execute("UPDATE ticket_messages SET attachment_url='https://legacy.invalid/file'");
                assertThrows(SQLException.class,()->c.createStatement().execute(sql));
            } finally { c.rollback(); }
        }
    }
}
