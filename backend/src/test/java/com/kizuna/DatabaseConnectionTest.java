package com.kizuna;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void testDatabaseConnection() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("========== DATABASE CONNECTION SUCCESSFUL ==========");
            System.out.println("Connected to: " + connection.getMetaData().getURL());
            System.out.println("Database Product Name: " + connection.getMetaData().getDatabaseProductName());
            System.out.println("Database Product Version: " + connection.getMetaData().getDatabaseProductVersion());
            System.out.println("User: " + connection.getMetaData().getUserName());

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT current_database(), current_user, version()")) {
                if (rs.next()) {
                    System.out.println("Current Database: " + rs.getString(1));
                    System.out.println("Current User: " + rs.getString(2));
                    System.out.println("PostgreSQL Version: " + rs.getString(3));
                }
            }
            System.out.println("=====================================================");
            assertTrue(connection.isValid(5));
        }
    }
}

