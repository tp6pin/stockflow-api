package com.tp6pin.stockflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.UserRole;
import com.tp6pin.stockflow.entity.UserRoleId;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findAllByUser_Id(Long userId);

    boolean existsByUser_IdAndRole_Id(Long userId, Long roleId);

    void deleteByUser_IdAndRole_Id(Long userId, Long roleId);
}