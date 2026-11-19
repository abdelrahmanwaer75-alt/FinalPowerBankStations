package com.powerbank.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "inspection_images")
public class InspectionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 30)
    private ImageOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "file_path", nullable = false, unique = true, length = 255)
    private String filePath;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private int fileSize;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private Instant uploadedAt;

    protected InspectionImage() {}

    public InspectionImage(ImageOwnerType ownerType, Long ownerId, String filePath, String originalName,
                           String mimeType, int fileSize, Long uploadedBy) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.filePath = filePath;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getFilePath() { return filePath; }
    public String getOriginalName() { return originalName; }
    public String getMimeType() { return mimeType; }
    public int getFileSize() { return fileSize; }
}
