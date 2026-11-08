package fu.libmanagement.user.service;

import fu.libmanagement.user.dto.ReaderResponse;
import org.springframework.data.domain.Page;

//UC6: Quản lý đôc giả
public interface ReaderManagementService {

    Page<ReaderResponse> search(String keyword, String status, int page);

    void approve(Long cardId);

    void lock(Long cardId);

    void unlock(Long cardId);

    void extend(Long cardId);
}