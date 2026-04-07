package com.integration.management.repository;

import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.management.entity.CredentialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CredentialTypeRepository extends JpaRepository<CredentialType, UUID> {
    Optional<CredentialType> findByCredentialAuthType(CredentialAuthType credentialAuthType);

    List<CredentialType> findByIsEnabledTrue();
}
