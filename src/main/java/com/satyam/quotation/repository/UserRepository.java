package com.satyam.quotation.repository;

import com.satyam.quotation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByResetToken(String resetToken);

    List<User> findByCompanyId(Long companyId);

    List<User> findByCreatedBy(Long createdBy);

    List<User> findByCompanyIdAndRole_RoleName(Long companyId, String roleName);
}
