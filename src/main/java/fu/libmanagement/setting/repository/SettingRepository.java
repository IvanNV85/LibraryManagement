package fu.libmanagement.setting.repository;

import fu.libmanagement.setting.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, String> { }
