package com.fitcoach.repository;

import com.fitcoach.domain.entity.ProgressPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, Long> {
    List<ProgressPhoto> findByTraineeIdOrderByPhotoDateDesc(Long traineeId);
}
