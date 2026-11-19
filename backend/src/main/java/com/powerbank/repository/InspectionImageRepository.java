package com.powerbank.repository;

import com.powerbank.entity.ImageOwnerType;
import com.powerbank.entity.InspectionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionImageRepository extends JpaRepository<InspectionImage, Long> {
    List<InspectionImage> findByOwnerTypeAndOwnerIdOrderByIdAsc(ImageOwnerType type, Long ownerId);
    long countByOwnerTypeAndOwnerId(ImageOwnerType type, Long ownerId);
}
