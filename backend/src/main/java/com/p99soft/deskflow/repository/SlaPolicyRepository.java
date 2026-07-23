package com.p99soft.deskflow.repository;

import com.p99soft.deskflow.entity.SlaPolicy;
import com.p99soft.deskflow.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, UUID> {
    Optional<SlaPolicy> findByPriority(Priority priority);
}
