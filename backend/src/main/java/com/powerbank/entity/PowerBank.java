package com.powerbank.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "powerbanks")
public class PowerBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serial_number", nullable = false, unique = true, length = 60)
    private String serialNumber;

    @Column(length = 100)
    private String model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PowerBankStatus status = PowerBankStatus.AVAILABLE;

    protected PowerBank() {}

    public PowerBank(String serialNumber, String model) {
        this.serialNumber = serialNumber;
        this.model = model;
    }

    public Long getId() { return id; }
    public String getSerialNumber() { return serialNumber; }
    public String getModel() { return model; }
    public Station getStation() { return station; }
    public void setStation(Station station) { this.station = station; }
    public PowerBankStatus getStatus() { return status; }
    public void setStatus(PowerBankStatus status) { this.status = status; }
}
