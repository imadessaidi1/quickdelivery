package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Users extends CrudRepository<User, Long> {
    List<Users> findOneByFirstName(String firstName);
    @Query("SELECT u " +
            "FROM User u WHERE u.emailAddress = :email")
    User finByEmail(@Param("email") String email);
}
