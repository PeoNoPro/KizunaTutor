package com.kizuna.features.payment.service;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.TutorState;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import com.kizuna.features.payment.repository.MembershipTutorRepository;
import com.kizuna.features.payment.repository.TutorMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Cập nhật hết hạn khi đọc (FR-013) và theo lịch của {@link MembershipReminderScheduler} (mỗi giờ).
 * Gói ACTIVE có end_date trước hôm nay → EXPIRED. Hết mọi gói còn hiệu lực → tutor_state ACTIVE_PAID trở về
 * APPROVED_UNPAID, membership_status = EXPIRED. Gia sư chưa từng có bản ghi hội viên không bị đụng tới.
 */
@Service
public class MembershipExpiryService {

    private final TutorMembershipRepository membershipRepository;
    private final MembershipTutorRepository tutorRepository;

    /** Thông báo tự động (FE-12). Không bắt buộc, nên test dựng service bằng tay vẫn chạy. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.kizuna.features.notification.service.NotificationEvents notificationEvents;

    public MembershipExpiryService(TutorMembershipRepository membershipRepository,
                                   MembershipTutorRepository tutorRepository) {
        this.membershipRepository = membershipRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional
    public void refresh(UUID tutorId, LocalDate today) {
        if (!membershipRepository.existsByTutorTutorId(tutorId)) {
            return;
        }
        long ended = membershipRepository.countEndedBefore(tutorId, TutorMembershipState.ACTIVE, today);
        long valid = membershipRepository.countValidOn(tutorId, TutorMembershipState.ACTIVE, today);
        Tutor current = tutorRepository.findWithUserByTutorId(tutorId).orElse(null);
        if (current == null) {
            return;
        }
        boolean stateStale = valid == 0 && (current.getTutorState() == TutorState.ACTIVE_PAID
                || current.getMembershipStatus() != TutorMembershipStatus.EXPIRED);
        if (ended == 0 && !stateStale) {
            return;
        }

        Tutor tutor = tutorRepository.findLockedByTutorId(tutorId).orElse(current);
        LocalDate lastEnd = null;
        for (TutorMembership membership : membershipRepository.findEndedBefore(tutorId, TutorMembershipState.ACTIVE, today)) {
            membership.setStatus(TutorMembershipState.EXPIRED);
            if (lastEnd == null || membership.getEndDate().isAfter(lastEnd)) lastEnd = membership.getEndDate();
        }
        if (membershipRepository.countValidOn(tutorId, TutorMembershipState.ACTIVE, today) == 0) {
            if (tutor.getTutorState() == TutorState.ACTIVE_PAID) {
                tutor.setTutorState(TutorState.APPROVED_UNPAID);
            }
            tutor.setMembershipStatus(TutorMembershipStatus.EXPIRED);
            notifyExpired(tutor, lastEnd);
        }
    }

    /**
     * FE-12: báo gia sư khi gói hội viên cuối cùng vừa hết hạn (chỉ khi lần này có gói thật sự chuyển sang hết hạn).
     * Mã sự kiện gắn với ngày hết hạn của gói nên chạy lại (đọc nhiều lần, job nền) không báo trùng.
     */
    private void notifyExpired(Tutor tutor, LocalDate lastEnd) {
        if (notificationEvents == null || lastEnd == null || tutor.getUser() == null) return;
        notificationEvents.publish("membership:" + tutor.getTutorId() + ":expired:" + lastEnd, "PAYMENT", "Gói hội viên đã hết hạn",
                "Gói hội viên của bạn đã hết hạn ngày " + lastEnd.format(MembershipLabels.DATE_VN)
                        + ". Các lớp học hiện chỉ xem được và bạn chưa thể mở lớp mới. Hãy gia hạn để tiếp tục.",
                "/tutor/membership", java.util.List.of(tutor.getUser().getUserId()));
    }
}
