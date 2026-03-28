package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.PackageSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageSettlements extends JpaRepository<PackageSettlement, Long> {
    @Query("SELECT ps FROM PackageSettlement ps JOIN FETCH ps.aPackage p ORDER BY ps.calculatedAt DESC")
    List<PackageSettlement> findAllWithPackageOrderByCalculatedAtDesc();
}
