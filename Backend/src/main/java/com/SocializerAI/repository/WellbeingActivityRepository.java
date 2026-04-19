package com.SocializerAI.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.SocializerAI.model.WellbeingActivity;
import java.util.List;
import java.util.UUID;

public interface WellbeingActivityRepository extends JpaRepository<WellbeingActivity, UUID> {
    List<WellbeingActivity> findByRecommendedForContaining(String emotion);
}
