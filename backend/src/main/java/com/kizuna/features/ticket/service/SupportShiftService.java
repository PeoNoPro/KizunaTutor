package com.kizuna.features.ticket.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.ticket.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import java.time.*;
import java.util.*;

/**
 * Phân ca nhân viên hỗ trợ và tự giao yêu cầu theo ca.
 * - Quản lý xếp nhân viên vào 3 ca cố định mỗi ngày.
 * - Hệ thống tự giao yêu cầu mới cũ nhất cho nhân viên đang trong ca và chưa giữ yêu cầu nào đang xử lý,
 *   nên hai người không thể cùng nhận một yêu cầu.
 * - Nhân viên chỉ tự bấm "Nhận xử lý" được khi đang trong ca và đã xong yêu cầu trước.
 * - Ngày Quản lý chưa xếp ca nào thì giữ cách cũ: nhân viên hỗ trợ tự nhận, không giới hạn.
 */
@Slf4j
@Service
public class SupportShiftService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public record Shift(String code, String label, LocalTime start, LocalTime end) { }
    public static final List<Shift> SHIFTS = List.of(
        new Shift("MORNING", "Ca sáng", LocalTime.of(8, 0), LocalTime.of(12, 0)),
        new Shift("AFTERNOON", "Ca chiều", LocalTime.of(13, 0), LocalTime.of(17, 0)),
        new Shift("EVENING", "Ca tối", LocalTime.of(18, 0), LocalTime.of(22, 0)));
    private static final int MAX_ACTIVE = 1;

    private final SupportShiftRepository shifts;
    private final SupportTicketRepository tickets;
    private final TicketAuditEventRepository audit;
    private final SupportTicketActorResolver actors;
    private final SupportTicketIdentityRepository identities;
    private final Clock clock;

    public SupportShiftService(SupportShiftRepository shifts, SupportTicketRepository tickets,
            TicketAuditEventRepository audit, SupportTicketActorResolver actors,
            SupportTicketIdentityRepository identities,
            @org.springframework.beans.factory.annotation.Qualifier("supportShiftClock") Clock clock) {
        this.shifts=shifts;this.tickets=tickets;this.audit=audit;this.actors=actors;this.identities=identities;this.clock=clock;
    }

    private static Optional<Shift> shift(String code) { return SHIFTS.stream().filter(s -> s.code().equals(code)).findFirst(); }
    /** Ca đang diễn ra lúc này (giờ Việt Nam), nếu có. */
    public static Optional<Shift> currentShift(ZonedDateTime now) {
        var time = now.toLocalTime();
        return SHIFTS.stream().filter(s -> !time.isBefore(s.start()) && time.isBefore(s.end())).findFirst();
    }
    private ZonedDateTime now() { return ZonedDateTime.now(clock).withZoneSameInstant(ZONE); }
    private static Map<String, Object> describe(Shift s, LocalDate date) {
        var m = new LinkedHashMap<String, Object>();
        m.put("code", s.code()); m.put("label", s.label()); m.put("date", date.toString());
        m.put("start", s.start().toString()); m.put("end", s.end().toString());
        return m;
    }

    // ---------------- Lịch ca (Quản lý xếp, nhân viên hỗ trợ xem) ----------------

    @Transactional(readOnly = true)
    public Map<String, Object> week(LocalDate weekStart) {
        var actor = actors.current(false);
        if (!actor.manager() && !actor.cs()) throw new BusinessException(403, "Chỉ quản lý và nhân viên hỗ trợ được xem lịch ca.");
        var monday = (weekStart == null ? now().toLocalDate() : weekStart).with(DayOfWeek.MONDAY);
        var rows = shifts.between(monday, monday.plusDays(6));
        var days = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < 7; i++) {
            var date = monday.plusDays(i);
            var cells = new LinkedHashMap<String, Object>();
            for (var s : SHIFTS) cells.put(s.code(), rows.stream()
                .filter(r -> date.equals(((java.sql.Date) r.get("shift_date")).toLocalDate()) && s.code().equals(r.get("shift_code")))
                .map(r -> Map.of("shiftId", r.get("shift_id"), "staffId", r.get("staff_id"), "displayName", r.get("full_name"))).toList());
            days.add(Map.of("date", date.toString(), "shifts", cells));
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("weekStart", monday.toString());
        result.put("today", now().toLocalDate().toString());
        result.put("shiftTypes", SHIFTS.stream().map(s -> describe(s, monday)).toList());
        result.put("days", days);
        result.put("staff", identities.candidates("ROLE_CS_STAFF"));
        result.put("canEdit", actor.manager());
        return result;
    }

    @Transactional
    public Map<String, Object> assign(UUID staffId, LocalDate date, String code) {
        requireManager();
        lockAssignments();
        var actor = actors.current(true);
        if (!actor.manager()) throw new BusinessException(403, "Chỉ quản lý được xếp ca.");
        if (date == null || shift(code).isEmpty()) throw new BusinessException(400, "Vui lòng chọn ngày và ca hợp lệ.");
        if (date.isBefore(now().toLocalDate())) throw new BusinessException(400, "Không xếp ca cho ngày đã qua.");
        actors.recipient(staffId, "ROLE_CS_STAFF");
        if (!shifts.add(UUID.randomUUID(), staffId, date, code, actor.userId())) throw new BusinessException(409, "Nhân viên này đã có trong ca đó.");
        dispatchAfterCommit();
        return week(date);
    }

    @Transactional
    public Map<String, Object> unassign(UUID shiftId, LocalDate weekStart) {
        requireManager();
        lockAssignments();
        var actor = actors.current(true);
        if (!actor.manager()) throw new BusinessException(403, "Chỉ quản lý được xếp ca.");
        var date=shifts.dateOf(shiftId);
        if(date.isEmpty()) throw new BusinessException(404, "Không tìm thấy ca này, có thể đã được gỡ.");
        if(date.get().isBefore(now().toLocalDate())) throw new BusinessException(400,"Không gỡ lịch ca của ngày đã qua.");
        shifts.remove(shiftId, actor.userId());
        return week(weekStart);
    }

    /**
     * Chặn sớm người không phải Quản lý (403) trước khi lấy khóa phân việc, để họ không giữ khóa chung.
     * Sau khi lấy khóa vẫn kiểm tra lại với danh tính đã khóa (giữ thứ tự khóa giống bộ phân việc).
     */
    private void requireManager() {
        if (!actors.current(false).manager()) throw new BusinessException(403, "Chỉ quản lý được xếp ca.");
    }

    // ---------------- Thông tin ca cho nhân viên hỗ trợ ----------------

    /** Ca hiện tại / ca kế tiếp và số yêu cầu đang giữ, hiện ở Tổng quan của nhân viên hỗ trợ. */
    @Transactional(readOnly = true)
    public Map<String, Object> myShift(UUID staff) {
        var now = now();
        var result = new LinkedHashMap<String, Object>();
        var current = currentShift(now).filter(s -> shifts.onShift(staff, now.toLocalDate(), s.code()));
        result.put("current", current.map(s -> describe(s, now.toLocalDate())).orElse(null));
        Map<String, Object> next = null;
        for (var row : shifts.upcoming(staff, now.toLocalDate(), 6)) {
            var date = ((java.sql.Date) row.get("shift_date")).toLocalDate();
            var s = shift(row.get("shift_code").toString()).orElseThrow();
            if (date.isAfter(now.toLocalDate()) || s.start().isAfter(now.toLocalTime())) { next = describe(s, date); break; }
        }
        result.put("next", next);
        result.put("activeTickets", shifts.activeTickets(staff));
        result.put("maxActive", MAX_ACTIVE);
        return result;
    }

    /**
     * Nhân viên chỉ được tự nhận khi đang trong ca và chưa giữ yêu cầu nào đang xử lý.
     * Kết quả nhớ trong một lượt yêu cầu web để danh sách nhiều dòng không truy vấn lặp lại.
     */
    public boolean canClaim(UUID staff) {
        var attributes = RequestContextHolder.getRequestAttributes();
        String key = "supportShift.canClaim." + staff;
        if (attributes != null && attributes.getAttribute(key, RequestAttributes.SCOPE_REQUEST) instanceof Boolean cached) return cached;
        // Ngày chưa xếp ca nào: giữ cách cũ (nhân viên tự nhận), để yêu cầu không bị kẹt khi nhóm chưa dùng phân ca.
        boolean result = canTakeNewTicket(staff);
        if (attributes != null) attributes.setAttribute(key, result, RequestAttributes.SCOPE_REQUEST);
        return result;
    }

    /** Lock order for every writer: allocation lock -> ticket -> actor/recipient. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void lockAssignments() { shifts.lockDispatch(); }

    private boolean canTakeNewTicket(UUID staff) {
        var time=now();
        return !shifts.anyShiftOn(time.toLocalDate()) ||
            (currentShift(time).map(s -> shifts.onShift(staff,time.toLocalDate(),s.code())).orElse(false)
                && shifts.activeTickets(staff)<MAX_ACTIVE);
    }

    /** Never use the UI/request cache when granting work: state may have changed while waiting for the lock. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void requireNewAssignment(UUID staff) {
        if(!canTakeNewTicket(staff)) throw new BusinessException(409,"Nhân viên phải đang trong ca và chưa có yêu cầu đang xử lý để nhận việc mới.");
    }

    // ---------------- Tự giao yêu cầu theo ca ----------------

    /** Chạy sau khi giao dịch hiện tại lưu xong (tạo yêu cầu, giải quyết, chuyển quản lý...). */
    public void dispatchAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { dispatchSafely(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { dispatchSafely(); }
        });
    }

    /** Lưới an toàn: định kỳ giao các yêu cầu còn chờ, kể cả khi vừa sang ca mới. */
    @Scheduled(fixedDelay = 30000, initialDelay = 20000)
    public void dispatchSafely() {
        try { self().dispatch(); }
        catch (RuntimeException exception) { log.warn("[SUPPORT SHIFT] Chưa tự giao được yêu cầu: {}", exception.getClass().getSimpleName()); }
    }

    /** Gọi qua proxy của Spring để @Transactional của dispatch() có hiệu lực. */
    @org.springframework.beans.factory.annotation.Autowired @org.springframework.context.annotation.Lazy
    private SupportShiftService proxy;
    private SupportShiftService self() { return proxy == null ? this : proxy; }

    /** Giao lần lượt: mỗi nhân viên rảnh trong ca nhận một yêu cầu mới cũ nhất. Khóa chung để hai lượt không chạy chồng. */
    @Transactional(propagation=Propagation.REQUIRES_NEW, timeout = 15)
    public int dispatch() {
        var now = now();
        var current = currentShift(now);
        if (current.isEmpty()) return 0;
        shifts.lockDispatch();
        int assigned = 0;
        for (var staff : shifts.freeStaff(now.toLocalDate(), current.get().code())) {
            // Lock current identity against concurrent deactivation/role changes.
            try { if(!identities.find(null,staff,true).cs()) continue; }
            catch(BusinessException exception) { if(exception.status()==403)continue;throw exception; }
            if(!canTakeNewTicket(staff)) continue;
            // An inactive assignee must not leave a ticket stranded. End-of-shift alone is not inactivity.
            var handover=shifts.oldestUnavailableTicket();
            var ticketId = handover.isPresent()?handover:shifts.oldestOpenTicket();
            if (ticketId.isEmpty()) break;
            var t = tickets.find(ticketId.get(), true);
            if(t==null)continue;
            boolean replacing=handover.isPresent();
            if(!replacing&&(!"OPEN".equals(String.valueOf(t.get("status")))||t.get("assigned_to")!=null))continue;
            var before = new LinkedHashMap<String, Object>();
            before.put("status", t.get("status")); before.put("assigned_to", t.get("assigned_to")); before.put("version", t.get("version"));
            t.put("assigned_to", staff); if(!replacing)t.put("status", "IN_PROGRESS");
            tickets.save(t);
            long next = ((Number) t.get("version")).longValue() + 1;
            var after = new LinkedHashMap<String, Object>();
            after.put("status", t.get("status")); after.put("assigned_to", staff); after.put("version", next);
            audit.append(ticketId.get(), staff, replacing?"REASSIGN_CS":"AUTO_ASSIGN", next, before, after,
                replacing?"Hệ thống bàn giao vì nhân viên cũ không còn hoạt động; "+current.get().label():current.get().label());
            assigned++;
        }
        if (assigned > 0) log.info("[SUPPORT SHIFT] Đã tự giao {} yêu cầu trong {}", assigned, current.get().label());
        return assigned;
    }
}
