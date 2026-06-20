package com.ventureverse.ventureverse_api.services.impl;

import com.ventureverse.ventureverse_api.dto.request.StartupCreateRequest;
import com.ventureverse.ventureverse_api.dto.response.StartupResponse;
import com.ventureverse.ventureverse_api.entities.Startup;
import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.repositories.StartupRepository;
import com.ventureverse.ventureverse_api.repositories.UserRepository;
import com.ventureverse.ventureverse_api.security.SecurityUtils;
import com.ventureverse.ventureverse_api.services.StartupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StartupServiceImpl implements StartupService {

        private final StartupRepository startupRepository;
        private final UserRepository userRepository;

        @Override
        public StartupResponse createStartup(
                        StartupCreateRequest request) {

                String email = SecurityUtils.getCurrentUserEmail();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Startup startup = Startup.builder()
                                .startupName(request.getStartupName())
                                .ideaDescription(request.getIdeaDescription())
                                .industry(request.getIndustry())
                                .targetMarket(request.getTargetMarket())
                                .owner(user)
                                .createdAt(LocalDateTime.now())
                                .build();

                Startup saved = startupRepository.save(startup);

                return map(saved);
        }

        @Override
        public List<StartupResponse> getMyStartups() {

                String email = SecurityUtils.getCurrentUserEmail();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return startupRepository
                                .findByOwnerId(user.getId())
                                .stream()
                                .map(this::map)
                                .toList();
        }

        @Override
        public StartupResponse getStartupById(Long id) {

                String email = SecurityUtils.getCurrentUserEmail();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Startup startup = startupRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Startup not found"));

                if (!startup.getOwner()
                                .getId()
                                .equals(user.getId())) {

                        throw new RuntimeException(
                                        "Access denied");
                }

                return map(startup);
        }

        private StartupResponse map(
                        Startup startup) {

                return StartupResponse.builder()
                                .id(startup.getId())
                                .startupName(startup.getStartupName())
                                .ideaDescription(startup.getIdeaDescription())
                                .industry(startup.getIndustry())
                                .targetMarket(startup.getTargetMarket())
                                .build();
        }
}