package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.PackagePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PackagePayments extends JpaRepository<PackagePayment, Long> {
    Optional<PackagePayment> findByProviderAndProviderSessionId(String provider, String providerSessionId);
}
