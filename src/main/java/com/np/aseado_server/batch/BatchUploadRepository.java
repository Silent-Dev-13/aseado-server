package com.np.aseado_server.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BatchUploadRepository extends JpaRepository<BatchUpload, Long> {
    List<BatchUpload> findBySessionIdInAndStatus(List<Long> sessionIds, BatchUploadStatus status);
}
