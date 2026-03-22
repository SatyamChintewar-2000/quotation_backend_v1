package com.satyam.quotation.service;

import com.satyam.quotation.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User createUser(User user);

    Optional<User> getUserById(Long id);

    Optional<User> getUserByEmail(String email);

    List<User> getUsersByCompany(Long companyId);

    List<User> getStaffByCompany(Long companyId);
}
