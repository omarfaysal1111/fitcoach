package com.fitcoach.repository;

import com.fitcoach.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipient_IdAndReadIsFalse(Long recipientId);

    Optional<Notification> findByIdAndRecipient_Id(Long id, Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true "
            + "WHERE n.recipient.id = :recipientId AND n.read = false")
    int markAllReadForRecipient(@Param("recipientId") Long recipientId);
}
