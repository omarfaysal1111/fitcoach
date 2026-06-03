package com.fitcoach.repository;

import com.fitcoach.domain.entity.ProgressPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, Long> {

    @Query("SELECT p FROM ProgressPhoto p JOIN FETCH p.trainee WHERE p.trainee.id = :traineeId ORDER BY p.photoDate DESC")
    List<ProgressPhoto> findByTraineeIdOrderByPhotoDateDesc(@Param("traineeId") Long traineeId);
}
