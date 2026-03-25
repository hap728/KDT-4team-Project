package com.project.mvcgithublogin.profile;

import com.project.mvcgithublogin.dto.ProfileUserProfileRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class ProfileUserService {
    private final ProfileUserRepository profileUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ProfileUserService(ProfileUserRepository profileUserRepository) {
        this.profileUserRepository = profileUserRepository;
    }

    @Transactional
    public ProfileUser updateProfile(ProfileUserProfileRequest request) {
        ProfileUser user = profileUserRepository.findByLoginId(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        String nickname = request.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = user.getNickname();
        }
        String intro = request.getIntro();
        if (intro == null || intro.isBlank()) {
            intro = user.getIntro();
        }
        String stackName = joinStackNames(request.getStackNames());
        if (stackName == null || stackName.isBlank()) {
            stackName = user.getStackName();
        }
        user.updateProfile(nickname, intro, stackName);

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return user;
    }

    @Transactional(readOnly = true)
    public List<String> getUserStackNames(String loginId) {
        ProfileUser user = profileUserRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (user.getStackName() == null || user.getStackName().isBlank()) {
            return Collections.emptyList();
        }

        return List.of(user.getStackName().split("\\s*,\\s*"));
    }

    @Transactional(readOnly = true)
    public ProfileUser getByLoginId(String loginId) {
        return profileUserRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    private String joinStackNames(List<String> stackNames) {
        if (stackNames == null || stackNames.isEmpty()) {
            return null;
        }

        return String.join(", ", stackNames);
    }
}
