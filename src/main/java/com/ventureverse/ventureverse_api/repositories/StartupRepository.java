package com.ventureverse.ventureverse_api.repositories;

import com.ventureverse.ventureverse_api.entities.Startup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StartupRepository extends JpaRepository<Startup, Long> {

    List<Startup> findByOwnerId(Long ownerId);
}