package com.satyam.quotation.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.satyam.quotation.model.User;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public UserServiceImpl(UserRepository userRepository, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Evict only the affected company's user cache entries.
     * Other companies' user lists remain cached and unaffected.
     */
    private void evictUserCache(Long companyId) {
        var cache = cacheManager.getCache("users");
        if (cache != null) {
            if (companyId != null) {
                cache.evict("company:" + companyId);
                cache.evict("staff:"   + companyId);
            }
            cache.evict("all"); // super-admin "all" view must always refresh
        }
    }

    @Override
    public User createUser(User user) {
        User saved = userRepository.save(user);
        Long companyId = saved.getCompany() != null ? saved.getCompany().getId() : null;
        evictUserCache(companyId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'id:' + #id")
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'all'")
    public List<User> getAllUsers() {
        return userRepository.findAllWithDetails();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'company:' + #companyId")
    public List<User> getUsersByCompany(Long companyId) {
        return userRepository.findByCompanyId(companyId);
    }

    @Override
    @Cacheable(value = "users", key = "'staff:' + #companyId")
    public List<User> getStaffByCompany(Long companyId) {
        return userRepository.findByCompanyIdAndRole_RoleName(companyId, "STAFF");
    }
}
