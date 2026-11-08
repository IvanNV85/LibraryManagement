package fu.libmanagement.setting.entity;

import fu.libmanagement.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "settings")
@Getter @Setter
public class Setting {
    @Id @Column(name = "setting_key", length = 50)
    private String key;
    @Column(name = "setting_value", nullable = false, length = 100)
    private String value;
    @Column(length = 255)
    private String description;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
    private User updatedBy;
}
