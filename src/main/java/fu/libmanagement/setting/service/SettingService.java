package fu.libmanagement.setting.service;

import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.setting.entity.Setting;
import fu.libmanagement.setting.repository.SettingRepository;
import fu.libmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Đọc quy định từ database; chỉ ba quy định được phép cập nhật qua UC24. */
@Service
@RequiredArgsConstructor
public class SettingService {
    public static final String MAX_BOOKS = "MAX_BOOKS_PER_READER";
    public static final String LOAN_DAYS = "LOAN_DAYS";
    public static final String FINE_PER_DAY = "FINE_PER_DAY";
    private final SettingRepository settings;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public String getValue(String key) {
        return settings.findById(key).orElseThrow(() -> new BusinessException("Thiếu cấu hình: " + key)).getValue();
    }

    @Transactional(readOnly = true)
    public int getInt(String key) { return Integer.parseInt(getValue(key)); }

    @Transactional(readOnly = true)
    public BigDecimal getDecimal(String key) { return new BigDecimal(getValue(key)); }

    @Transactional(readOnly = true)
    public Map<String, String> currentRules() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : new String[]{MAX_BOOKS, LOAN_DAYS, FINE_PER_DAY}) result.put(key, getValue(key));
        return result;
    }

    @Transactional
    public void updateRules(String maxBooks, String loanDays, String fine, Long userId) {
        int max = positiveInteger(maxBooks, "Số sách tối đa");
        int days = positiveInteger(loanDays, "Số ngày mượn");
        BigDecimal amount;
        try {
            amount = new BigDecimal(fine == null ? "" : fine.trim()).stripTrailingZeros();
            if (amount.signum() < 0 || amount.scale() > 2 || amount.compareTo(new BigDecimal("9999999999999999.99")) > 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new BusinessException("Mức phạt phải không âm, tối đa 2 chữ số thập phân");
        }
        var user = users.findById(userId).orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản"));
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> values = Map.of(MAX_BOOKS, Integer.toString(max), LOAN_DAYS, Integer.toString(days), FINE_PER_DAY, amount.toPlainString());
        for (var entry : values.entrySet()) {
            Setting setting = settings.findById(entry.getKey()).orElseThrow(() -> new BusinessException("Thiếu cấu hình: " + entry.getKey()));
            setting.setValue(entry.getValue());
            setting.setUpdatedBy(user);
            setting.setUpdatedAt(now);
        }
    }

    private int positiveInteger(String value, String label) {
        try {
            int n = Integer.parseInt(value == null ? "" : value.trim());
            if (n <= 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) { throw new BusinessException(label + " phải là số nguyên dương"); }
    }
}
