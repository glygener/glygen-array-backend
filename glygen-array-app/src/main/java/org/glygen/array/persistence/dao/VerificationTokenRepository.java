package org.glygen.array.persistence.dao;

import java.util.Date;
import java.util.stream.Stream;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
	VerificationToken findByToken(String token);

    VerificationToken findByUser(UserEntity user);

    Stream<VerificationToken> findAllByExpiryDateLessThan(Date now);

    void deleteByExpiryDateLessThan(Date now);

    @Modifying
    @Query("delete from verification_token t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(Date now);
	

}
