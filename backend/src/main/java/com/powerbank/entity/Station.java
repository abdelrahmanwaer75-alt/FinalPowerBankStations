package com.powerbank.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stations")
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "total_capacity", nullable = false)
    private int totalCapacity;

    @Column(name = "opening_hours", length = 120)
    private String openingHours;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StationStatus status = StationStatus.ACTIVE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected Station() {}

    public Station(String name, String address, String city, BigDecimal latitude, BigDecimal longitude,
                   int totalCapacity, String openingHours, String contactPhone, String description) {
        apply(name, address, city, latitude, longitude, totalCapacity, openingHours, contactPhone, description);
    }

    public void apply(String name, String address, String city, BigDecimal latitude, BigDecimal longitude,
                      int totalCapacity, String openingHours, String contactPhone, String description) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalCapacity = totalCapacity;
        this.openingHours = openingHours;
        this.contactPhone = contactPhone;
        this.description = description;
    }

    public void setStatus(StationStatus status) { this.status = status; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public int getTotalCapacity() { return totalCapacity; }
    public String getOpeningHours() { return openingHours; }
    public String getContactPhone() { return contactPhone; }
    public String getDescription() { return description; }
    public String getImagePath() { return imagePath; }
    public StationStatus getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }
}
