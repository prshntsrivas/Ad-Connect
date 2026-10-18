package com.adconnect.auth.repository;

import com.adconnect.auth.entity.User;
import com.adconnect.auth.entity.UserStatus;
import com.adconnect.common.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
              and (:q is null or lower(u.email) like lower(concat('%', :q, '%'))
                   or lower(u.fullName) like lower(concat('%', :q, '%')))
            """)
    Page<User> search(@Param("role") Role role, @Param("status") UserStatus status, @Param("q") String q,
                      Pageable pageable);

    @Query("select u.role as role, u.status as status, count(u) as total from User u group by u.role, u.status")
    List<RoleStatusCount> countByRoleAndStatus();

    interface RoleStatusCount {
        Role getRole();

        UserStatus getStatus();

        long getTotal();
    }
}
