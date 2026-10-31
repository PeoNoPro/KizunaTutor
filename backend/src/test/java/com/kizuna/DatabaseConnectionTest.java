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

    @Test
    public void testSelectAllTutors() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("========== ALL TUTORS IN DATABASE ==========");
            String sql = "SELECT t.tutor_id, u.full_name, u.email, t.verification_status, " +
                         "t.highest_jlpt_level, t.hourly_rate, t.avg_rating, t.total_reviews, " +
                         "t.headline, t.is_featured, u.account_status " +
                         "FROM tutors t LEFT JOIN users u ON t.user_id = u.user_id";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.println("--- GIA SU #" + count + " ---");
                    System.out.println("ID: " + rs.getString("tutor_id"));
                    System.out.println("Ho va Ten: " + rs.getString("full_name"));
                    System.out.println("Email: " + rs.getString("email"));
                    System.out.println("Trang thai xet duyet: " + rs.getString("verification_status"));
                    System.out.println("Trang thai tai khoan: " + rs.getString("account_status"));
                    System.out.println("Cap do JLPT: " + rs.getString("highest_jlpt_level"));
                    System.out.println("Hoc phi / gio: " + rs.getBigDecimal("hourly_rate") + " VND");
                    System.out.println("Diem danh gia: " + rs.getBigDecimal("avg_rating") + " sao (" + rs.getInt("total_reviews") + " danh gia)");
                    System.out.println("Tieu de: " + rs.getString("headline"));
                    System.out.println("Noi bat (is_featured): " + rs.getBoolean("is_featured"));
                }
                System.out.println("==========================================");
                System.out.println("TONG SO GIA SU TRONG DATABASE: " + count);
            }
        }
    }
}

