package com.np.aseado_server.session;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReceivingSessionRepository extends JpaRepository<ReceivingSession, Long> {
    Optional<ReceivingSession> findByAccessKey(String accessKey);
    Optional<ReceivingSession> findFirstByBucketIdAndClosedAtIsNull(Long bucketId);
    List<ReceivingSession> findByBucketId(Long bucketId);
}
