package com.powerbank.service;

import com.powerbank.dto.ImageResponse;
import com.powerbank.dto.ReturnResponse;
import com.powerbank.entity.*;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.powerbank.service.ReservationService.conflict;
import static com.powerbank.service.ReservationService.ref;

@Service
@Transactional
public class ReturnService {

    /** Devices physically docked at a station. These count against its capacity. */
    static final Set<PowerBankStatus> DOCKED = EnumSet.of(
            PowerBankStatus.AVAILABLE, PowerBankStatus.RESERVED, PowerBankStatus.RETURNED_PENDING_REVIEW);

    private final RentalRepository rentals;
    private final StationRepository stations;
    private final PowerBankRepository powerBanks;
    private final RentalPricingSnapshotRepository snapshots;
    private final RentalReturnRepository returns;
    private final InspectionImageRepository images;

    public ReturnService(RentalRepository rentals, StationRepository stations, PowerBankRepository powerBanks,
                         RentalPricingSnapshotRepository snapshots, RentalReturnRepository returns,
                         InspectionImageRepository images) {
        this.rentals = rentals;
        this.stations = stations;
        this.powerBanks = powerBanks;
        this.snapshots = snapshots;
        this.returns = returns;
        this.images = images;
    }

    public ReturnResponse create(long userId, long rentalId, long returnStationId) {
        Rental rental = rentals.findByIdAndUserId(rentalId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RENTAL_NOT_FOUND", "Rental not found"));
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw conflict("RENTAL_NOT_ACTIVE", "This rental is already closed");
        }

        Station station = stations.findById(returnStationId)
                .filter(s -> s.getStatus() == StationStatus.ACTIVE)
                .orElseThrow(() -> conflict("STATION_UNAVAILABLE", "This station does not accept returns right now"));

        long docked = powerBanks.countDockedAt(station.getId(), DOCKED);
        if (docked >= station.getTotalCapacity()) {
            throw conflict("STATION_FULL", "This station is full. Choose another station.");
        }

        RentalPricingSnapshot snap = snapshots.findByRentalId(rental.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SNAPSHOT_MISSING",
                        "Pricing record is missing for this rental"));

        Instant now = Instant.now();
        long minutes = RentalService.minutesBetween(rental.getStartTime(), now);
        BigDecimal cost = PricingCalculator.cost(snap.getBaseFee(), snap.getPricePerMinute(),
                snap.getDailyMaxFee(), snap.getFreeMinutes(), minutes);

        rental.complete(now, (int) minutes, cost);

        // The device waits for inspection before it becomes AVAILABLE again.
        PowerBank bank = rental.getPowerBank();
        bank.setStation(station);
        bank.setStatus(PowerBankStatus.RETURNED_PENDING_REVIEW);

        RentalReturn ret = new RentalReturn(ref("RET"), rental, station, now, (int) minutes, cost);
        returns.save(ret);
        return toResponse(ret, List.of());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ReturnResponse get(long userId, long id) {
        RentalReturn ret = owned(userId, id);
        return toResponse(ret, imagesOf(id));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String status(long userId, long id) {
        return owned(userId, id).getStatus().name();
    }

    RentalReturn owned(long userId, long id) {
        return returns.findByIdAndRentalUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RETURN_NOT_FOUND", "Return not found"));
    }

    List<ImageResponse> imagesOf(long returnId) {
        return images.findByOwnerTypeAndOwnerIdOrderByIdAsc(ImageOwnerType.RETURN, returnId).stream()
                .map(i -> new ImageResponse(i.getId(), i.getOriginalName(), i.getMimeType(), i.getFileSize()))
                .toList();
    }

    ReturnResponse toResponse(RentalReturn r, List<ImageResponse> imgs) {
        return new ReturnResponse(
                r.getId(), r.getReference(), r.getRental().getId(), r.getStatus().name(),
                r.getReturnStation().getName(), r.getReturnedAt(),
                r.getFinalDurationMinutes(), r.getFinalCost(), imgs);
    }
}
