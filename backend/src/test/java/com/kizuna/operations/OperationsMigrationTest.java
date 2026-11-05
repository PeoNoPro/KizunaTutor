package com.kizuna.operations;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.Connection;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class OperationsMigrationTest {
    @Test
    void migrationPreservesLegacyAndIsRepeatable() throws Exception {
        var ds = OperationsTestEnvironment.dataSource();
        String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = ds.getConnection()) {
            c.createStatement().execute("CREATE SCHEMA " + schema);
            c.createStatement().execute("SET search_path TO " + schema);
            c.createStatement().execute("CREATE TABLE users(user_id uuid PRIMARY KEY)");
            c.createStatement().execute("CREATE TABLE trial_bookings(booking_id uuid PRIMARY KEY)");
            c.createStatement().execute("INSERT INTO trial_bookings VALUES ('00000000-0000-0000-0000-000000000010')");
            c.createStatement().execute("CREATE TABLE system_configurations(config_key varchar(100) PRIMARY KEY, config_value text NOT NULL, data_type varchar(20) NOT NULL, description varchar(255), updated_at timestamptz NOT NULL DEFAULT now())");
            c.createStatement().execute("INSERT INTO system_configurations(config_key,config_value,data_type) VALUES ('TRIAL_DEFAULT_DURATION_MINUTES','50','NUMBER'),('BOOKING_MIN_HOURS_IN_ADVANCE','24','NUMBER')");
            String sql = OperationsTestEnvironment.fe11Schema();
            c.createStatement().execute(sql);
            c.createStatement().execute(sql);
            var rs = c.createStatement().executeQuery("SELECT duration_minutes,min_advance_hours,changed_by FROM booking_policy_revisions");
            assertTrue(rs.next()); assertEquals(50, rs.getInt(1)); assertEquals(24, rs.getInt(2));
            assertNull(rs.getObject(3)); assertFalse(rs.next());
            var bookings=c.createStatement().executeQuery("SELECT policy_revision_id,policy_duration_minutes,policy_min_advance_hours FROM trial_bookings");
            assertTrue(bookings.next()); assertNull(bookings.getObject(1));assertNull(bookings.getObject(2));assertNull(bookings.getObject(3));assertFalse(bookings.next());
            c.createStatement().execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }
    @Test void invalidConfigurationStopsWithoutChangingLegacyData() throws Exception {
        var ds=OperationsTestEnvironment.dataSource();
        String schema="invalid_"+UUID.randomUUID().toString().replace("-","");
        try(var c=ds.getConnection()) {
            c.createStatement().execute("CREATE SCHEMA "+schema); c.createStatement().execute("SET search_path TO "+schema);
            c.createStatement().execute("CREATE TABLE users(user_id uuid PRIMARY KEY);CREATE TABLE trial_bookings(booking_id uuid PRIMARY KEY);CREATE TABLE system_configurations(config_key varchar(100) PRIMARY KEY,config_value text,data_type varchar(20))");
            c.createStatement().execute("INSERT INTO system_configurations VALUES ('TRIAL_DEFAULT_DURATION_MINUTES','invalid','NUMBER')");
            String sql=OperationsTestEnvironment.fe11Schema();
            assertThrows(java.sql.SQLException.class,()->c.createStatement().execute(sql));
            c.createStatement().execute("ROLLBACK");
            var result=c.createStatement().executeQuery("SELECT config_value FROM system_configurations");assertTrue(result.next());assertEquals("invalid",result.getString(1));
            c.createStatement().execute("DROP SCHEMA "+schema+" CASCADE");
        }
    }
}
