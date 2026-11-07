package fu.libmanagement.borrowing.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


@Component
public class LoanSettings {

    @PersistenceContext
    private EntityManager em;

    public int maxBooksPerReader() {
        return getInt("MAX_BOOKS_PER_READER", 5);
    }

    public int maxRenewals() {
        return getInt("MAX_RENEWALS", 2);
    }

    public int renewDays() {
        return getInt("RENEW_DAYS", 7);
    }


    public BigDecimal finePerDay() {
        return BigDecimal.valueOf(getInt("FINE_PER_DAY", 2000));
    }

    public int dueReminderDays() {
        return getInt("DUE_REMINDER_DAYS", 2);
    }

    private int getInt(String key, int defaultValue) {
        List<?> rows = em.createNativeQuery("select setting_value from settings where setting_key = :key")
                .setParameter("key", key)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rows.get(0).toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}