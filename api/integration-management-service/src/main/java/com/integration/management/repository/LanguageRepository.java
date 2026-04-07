package com.integration.management.repository;

import com.integration.management.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Language master data.
 */
@Repository
public interface LanguageRepository extends JpaRepository<Language, String> {

    List<Language> findAllByIsEnabledTrueOrderBySortOrderAsc();
}
