package com.powerbank.admin;

import com.powerbank.dto.AdminReturnRow;
import com.powerbank.entity.*;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.*;
import com.powerbank.service.WaitingListService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Staff decisions on returns. Only an approved return makes its PowerBank available again. */
@Service
@Transactional
public class ReturnReviewService {

    static final Set<ReturnStatus> REVIEWABLE = EnumSet.of(
            ReturnStatus.PENDING_REVIEW, ReturnStatus.REQUIRES_MANUAL_REVIEW);

    private final RentalReturnRepository returns;
    private final ManualReviewRepository reviews;
    private final InspectionImageRepository images;
    private final WaitingListService waitingList;

    public ReturnReviewService(RentalReturnRepository returns, ManualReviewRepository reviews,
                               InspectionImageRepository images, WaitingListService waitingList) {
        this.returns = returns;
        this.reviews = reviews;
        this.images = images;
        this.waitingList = waitingList;
    }

    @Transactional(readOnly = true)
    public List<AdminReturnRow> queue(ReturnStatus status) {
        return returns.findByStatusOrderByReturnedAtAsc(status).stream().map(this::row).toList();
    }

    public void approve(long reviewerId, long returnId, String notes) {
        RentalReturn ret = load(returnId);
        requireReviewable(ret);
        ret.setStatus(ReturnStatus.APPROVED);
        PowerBank bank = ret.getRental().getPowerBank();
        bank.setStatus(PowerBankStatus.AVAILABLE);
        if (bank.getStation() != null) waitingList.promoteForAvailableStock(bank.getStation().getId(), 1);
        reviews.save(new ManualReview(ret, reviewerId, ReviewDecision.APPROVED, notes));
    }

    /** The device is taken out of service until it is repaired. */
    public void reject(long reviewerId, long returnId, String reason) {
        RentalReturn ret = load(returnId);
        requireReviewable(ret);
        ret.setStatus(ReturnStatus.REJECTED);
        ret.getRental().getPowerBank().setStatus(PowerBankStatus.UNAVAILABLE);
        reviews.save(new ManualReview(ret, reviewerId, ReviewDecision.REJECTED, reason));
    }

    /** Keeps the device out of the pool until a person has looked at the photos. */
    public void requestManualReview(long reviewerId, long returnId, String notes) {
        RentalReturn ret = load(returnId);
        requireReviewable(ret);
        ret.setStatus(ReturnStatus.REQUIRES_MANUAL_REVIEW);
        reviews.save(new ManualReview(ret, reviewerId, ReviewDecision.MANUAL_REVIEW, notes));
    }

    private RentalReturn load(long id) {
        return returns.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RETURN_NOT_FOUND", "Return not found"));
    }

    private void requireReviewable(RentalReturn ret) {
        if (!REVIEWABLE.contains(ret.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "RETURN_NOT_REVIEWABLE",
                    "This return has already been decided");
        }
    }

    private AdminReturnRow row(RentalReturn r) {
        return new AdminReturnRow(
                r.getId(), r.getReference(), r.getStatus().name(), r.getRental().getId(),
                r.getRental().getPowerBank().getSerialNumber(), r.getReturnStation().getName(),
                r.getReturnedAt(), r.getFinalDurationMinutes(),
                r.getFinalCost() == null ? null : r.getFinalCost().toPlainString(),
                (int) images.countByOwnerTypeAndOwnerId(ImageOwnerType.RETURN, r.getId()));
    }
}
