package com.eshopping.profile.repository;

import com.eshopping.profile.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGithubId(String githubId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = 'ROLE_MERCHANT' ORDER BY u.createdAt DESC")
    List<User> findAllMerchants();
}

