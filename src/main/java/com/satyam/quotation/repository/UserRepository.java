package com.satyam.quotation.repository;

import com.satyam.quotation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByResetToken(String resetToken);

    // JOIN FETCH role + company to avoid N+1 on user list queries
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.company WHERE u.company.id = :companyId")
    List<User> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.company WHERE u.createdBy = :createdBy")
    List<User> findByCreatedBy(@Param("createdBy") Long createdBy);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.company WHERE u.company.id = :companyId AND u.role.roleName = :roleName")
    List<User> findByCompanyIdAndRole_RoleName(@Param("companyId") Long companyId, @Param("roleName") String roleName);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.company")
    List<User> findAllWithDetails();

    long countByCompanyIdAndActiveTrue(Long companyId);
}
