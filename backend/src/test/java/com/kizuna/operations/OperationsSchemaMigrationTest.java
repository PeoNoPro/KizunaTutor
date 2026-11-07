package com.kizuna.operations;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Consolidated SQL works on a fresh feature schema and preserves existing data when reapplied. */
class OperationsSchemaMigrationTest {
 private String sql()throws Exception{return OperationsTestEnvironment.fe11Schema().replace("BEGIN;","").replace("COMMIT;","");}
 private void core(Connection c,String schema)throws Exception{
  c.createStatement().execute("CREATE SCHEMA "+schema+";SET LOCAL search_path TO "+schema+",public;"+
   "CREATE TABLE roles(role_id smallint PRIMARY KEY);"+
   "CREATE TABLE users(user_id uuid PRIMARY KEY);"+
   "CREATE TABLE trial_bookings(booking_id uuid PRIMARY KEY);"+
   "CREATE TABLE system_configurations(config_key varchar(100) PRIMARY KEY,config_value text NOT NULL);"+
   "INSERT INTO system_configurations VALUES ('TRIAL_DEFAULT_DURATION_MINUTES','45'),('BOOKING_MIN_HOURS_IN_ADVANCE','12');");
 }
 @Test void oneFileIncludesStaffPromotionMailAndIncidentUpdatesAndPreservesData()throws Exception{
  try(var c=OperationsTestEnvironment.dataSource().getConnection()){
   c.setAutoCommit(false);
   try{
    core(c,"fe11_merge_"+UUID.randomUUID().toString().replace("-",""));
    c.createStatement().execute(sql());
    UUID actor=UUID.randomUUID(),target=UUID.randomUUID();
    c.createStatement().execute("INSERT INTO users VALUES ('"+actor+"'),('"+target+"');"+
      "INSERT INTO staff_account_audit_events VALUES (gen_random_uuid(),'"+actor+"','"+target+"','CREATE','Giữ lịch sử đã có',now());"+
      "INSERT INTO staff_credential_emails(user_id,status,requested_by,updated_at) VALUES ('"+target+"','PENDING','"+actor+"',now());");
    c.createStatement().execute(sql());
    try(var r=c.createStatement().executeQuery("SELECT (SELECT count(*) FROM staff_account_audit_events),(SELECT count(*) FROM staff_credential_emails),(SELECT count(*) FROM booking_policy_revisions),to_regclass('staff_role_change_requests') IS NOT NULL,to_regclass('system_setting_audit_events') IS NOT NULL")){
     r.next();assertEquals(1,r.getInt(1));assertEquals(1,r.getInt(2));assertEquals(1,r.getInt(3));assertTrue(r.getBoolean(4));assertTrue(r.getBoolean(5));
    }
    try(var r=c.createStatement().executeQuery("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='technical_incident_events'::regclass AND conname='technical_incident_events_action_check'")){
     assertTrue(r.next());assertTrue(r.getString(1).contains("RELEASE"));
    }
   }finally{c.rollback();}
  }
 }
 @Test void invalidExistingConfigurationStopsWholeMigration()throws Exception{
  try(var c=OperationsTestEnvironment.dataSource().getConnection()){
   c.setAutoCommit(false);
   try{
    String schema="fe11_bad_"+UUID.randomUUID().toString().replace("-","");core(c,schema);
    c.createStatement().execute("UPDATE system_configurations SET config_value='not-an-integer' WHERE config_key='TRIAL_DEFAULT_DURATION_MINUTES'");
    var point=c.setSavepoint();assertThrows(SQLException.class,()->c.createStatement().execute(sql()));c.rollback(point);
    try(var r=c.createStatement().executeQuery("SELECT to_regclass('"+schema+".operations_schema_versions') IS NULL")){r.next();assertTrue(r.getBoolean(1));}
   }finally{c.rollback();}
  }
 }
}
