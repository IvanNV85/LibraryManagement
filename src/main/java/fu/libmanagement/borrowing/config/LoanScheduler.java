package fu.libmanagement.borrowing.config;

import fu.libmanagement.borrowing.service.LoanMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanScheduler {

    private final LoanMaintenanceService maintenanceService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        runSafely();
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void daily() {
        runSafely();
    }

    private void runSafely() {
        try {
            maintenanceService.run();
        } catch (Exception e) {
            log.warn("Không chạy được tác vụ định kỳ mượn trả", e);
        }
    }
}