package com.satyam.quotation.repository;

import com.satyam.quotation.model.Quotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    // ── Non-paginated (used by service layer for status/expiry/reports) ───────

    @Query("SELECT DISTINCT q FROM Quotation q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.company " +
           "WHERE q.company.id = :companyId AND q.active = true " +
           "ORDER BY q.createdAt DESC")
    List<Quotation> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT DISTINCT q FROM Quotation q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.company " +
           "WHERE q.createdBy = :userId AND q.active = true " +
           "ORDER BY q.createdAt DESC")
    List<Quotation> findByCreatedBy(@Param("userId") Long userId);

    @Query("SELECT DISTINCT q FROM Quotation q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.company " +
           "WHERE q.active = true " +
           "ORDER BY q.createdAt DESC")
    List<Quotation> findAllActive();

    // ── Paginated (used by the history list endpoint) ─────────────────────────
    // Note: JOIN FETCH cannot be combined with pagination at the DB level.
    // Spring will load the page of quotation IDs first, then batch-fetch associations.
    // The N+1 for customer/company is handled by hibernate.default_batch_fetch_size=50.

    @Query(value  = "SELECT q FROM Quotation q WHERE q.company.id = :companyId AND q.active = true",
           countQuery = "SELECT COUNT(q) FROM Quotation q WHERE q.company.id = :companyId AND q.active = true")
    Page<Quotation> findPageByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query(value  = "SELECT q FROM Quotation q WHERE q.createdBy = :userId AND q.active = true",
           countQuery = "SELECT COUNT(q) FROM Quotation q WHERE q.createdBy = :userId AND q.active = true")
    Page<Quotation> findPageByCreatedBy(@Param("userId") Long userId, Pageable pageable);

    @Query(value  = "SELECT q FROM Quotation q WHERE q.active = true",
           countQuery = "SELECT COUNT(q) FROM Quotation q WHERE q.active = true")
    Page<Quotation> findAllActivePage(Pageable pageable);

    // ── Detail query (single record with all collections) ────────────────────
    // Note: Cannot JOIN FETCH both items and services simultaneously (MultipleBagFetchException).
    // We fetch items + product in the main query; services are loaded by Hibernate batch fetch.

    @Query("SELECT DISTINCT q FROM Quotation q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.company " +
           "LEFT JOIN FETCH q.items i " +
           "LEFT JOIN FETCH i.product " +
           "WHERE q.id = :id AND q.active = true")
    Optional<Quotation> findByIdWithDetails(@Param("id") Long id);

    // ── Aggregate queries ─────────────────────────────────────────────────────

    @Query("SELECT SUM(q.totalAmount) FROM Quotation q WHERE q.company.id = :companyId")
    Double getTotalRevenueByCompany(@Param("companyId") Long companyId);

    @Query("SELECT SUM(q.totalAmount) FROM Quotation q WHERE q.createdBy = :userId")
    Double getTotalRevenueByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(q) FROM Quotation q WHERE q.createdBy = :userId AND q.active = true")
    long countByCreatedByAndActive(@Param("userId") Long userId);

    @Query("SELECT DISTINCT q FROM Quotation q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.company " +
           "WHERE q.status = :status AND q.active = true " +
           "AND (:companyId IS NULL OR q.company.id = :companyId)")
    List<Quotation> findByStatusAndCompany(@Param("status") String status, @Param("companyId") Long companyId);

    @Query("SELECT DISTINCT q FROM Quotation q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.company " +
           "WHERE q.active = true AND q.expiryDate IS NOT NULL AND q.expiryDate < CURRENT_DATE " +
           "AND (:companyId IS NULL OR q.company.id = :companyId)")
    List<Quotation> findExpiredQuotations(@Param("companyId") Long companyId);
}
