package com.tu.backend.auth.security;

import com.tu.backend.auth.entity.UserEntity;
import com.tu.backend.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {
        String normalizedAccount = account == null ? "" : account.trim().toLowerCase();
        UserEntity user = userRepository.findByUsername(normalizedAccount)
            .or(() -> userRepository.findByEmail(normalizedAccount))
            .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        return new AppUserDetails(user);
    }
}
