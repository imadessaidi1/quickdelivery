package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.Document;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Documents extends CrudRepository<Document, Long> {
}
