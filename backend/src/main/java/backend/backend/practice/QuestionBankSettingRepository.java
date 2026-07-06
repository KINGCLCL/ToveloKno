package backend.backend.practice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionBankSettingRepository extends JpaRepository<QuestionBankSetting, Long> {
    Optional<QuestionBankSetting> findByUserId(Long userId);
}
