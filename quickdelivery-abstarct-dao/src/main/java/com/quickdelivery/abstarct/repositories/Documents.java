package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Documents extends JpaRepository<Document, Long> {
    @Query("""
            SELECT DISTINCT d
            FROM Document d
            LEFT JOIN FETCH d.user u
            LEFT JOIN FETCH u.vehicles
            LEFT JOIN FETCH u.document
            LEFT JOIN FETCH d.vehicle
            WHERE d.id = :id
            """)
    Optional<Document> findByIdWithMatchingContext(@Param("id") Long id);
}
