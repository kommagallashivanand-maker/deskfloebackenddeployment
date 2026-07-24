package com.p99soft.deskflow.repository;

import com.p99soft.deskflow.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
}
