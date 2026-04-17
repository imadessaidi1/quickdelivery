package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.CustomerAddressBookEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerAddressBookEntries extends JpaRepository<CustomerAddressBookEntry, Long> {
    @Query("SELECT entry FROM CustomerAddressBookEntry entry " +
            "WHERE entry.owner.id = :ownerUserId " +
            "AND entry.active = true " +
            "AND (" +
            "LOWER(COALESCE(entry.firstName, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(entry.lastName, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(entry.email, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(entry.line1, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(entry.town, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(entry.zipCode, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(entry.addressAuto, '')) LIKE LOWER(CONCAT('%', :query, '%'))" +
            ") " +
            "ORDER BY entry.lastUsedAt DESC, entry.updatedAt DESC")
    List<CustomerAddressBookEntry> searchActiveEntries(@Param("ownerUserId") Long ownerUserId,
                                                       @Param("query") String query,
                                                       Pageable pageable);

    @Query("SELECT entry FROM CustomerAddressBookEntry entry " +
            "WHERE entry.owner.id = :ownerUserId " +
            "AND entry.active = true " +
            "AND LOWER(entry.email) = LOWER(:email)")
    List<CustomerAddressBookEntry> findActiveByOwnerAndEmail(@Param("ownerUserId") Long ownerUserId,
                                                            @Param("email") String email);
}
