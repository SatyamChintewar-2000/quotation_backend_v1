package com.satyam.quotation.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.satyam.quotation.model.User;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.service.UserService;


@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
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
    public List<User> getUsersByCompany(Long companyId) {
        return userRepository.findByCompanyId(companyId);
    }

    @Override
    public List<User> getStaffByCompany(Long companyId) {
        return userRepository.findByCompanyIdAndRole_RoleName(companyId, "STAFF");
    }
}
