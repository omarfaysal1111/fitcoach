package com.fitcoach.repository;

import com.fitcoach.domain.entity.ProgressPicture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressPictureRepository extends JpaRepository<ProgressPicture, Long> {

    List<ProgressPicture> findByTraineeIdOrderByDateDesc(Long traineeId);
}
