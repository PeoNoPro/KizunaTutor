package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.dto.StaffRoleChangeRequests.*;
import com.kizuna.features.admin.repository.*;
import com.kizuna.features.admin.repository.StaffAccountRepository.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Manager đổi vai trò giữa hai nhóm nhân viên và đề xuất thăng chức; chỉ Admin duyệt thăng chức. */
@Service @RequiredArgsConstructor
public class StaffRoleChangeService {
    private static final Map<String,String> MOVES=Map.of("ROLE_ACADEMIC_OFFICER","ROLE_CS_STAFF","ROLE_CS_STAFF","ROLE_ACADEMIC_OFFICER");
    private static final Map<String,String> STATES=Map.of("PENDING","Chờ duyệt","APPROVED","Đã duyệt","REJECTED","Đã từ chối","CANCELLED","Đã hủy");
    private final StaffRoleChangeRepository requests;
    private final StaffAccountRepository staff;
    private final StaffAccountService accounts;
    private final OperationsActorResolver actors;
    private final OperationsCommandExecutor locks;

    private UUID beginManager() {
        staff.serializeWrites();var actor=actors.requireManager();locks.lockManager(actor.userId());return actor.userId();
    }
    private void movable(Account a,UUID actor) {
        if(actor.equals(a.id()))throw new BusinessException(400,"Không thay đổi vai trò của chính mình.");
        if(!MOVES.containsKey(a.role())||!a.roleActive()||!a.status().equals("ACTIVE"))throw new BusinessException(400,"Chỉ áp dụng cho Cán bộ học vụ hoặc Chăm sóc khách hàng đang hoạt động.");
    }
    private void noPending(UUID target) {
        if(requests.hasPending(target))throw new BusinessException(409,"Nhân viên này đang có đề xuất thăng chức chờ duyệt.");
    }
    private StaffRoleChangeRepository.Request pending(UUID id) {
        var request=requests.lock(id);
        if(!request.status().equals("PENDING"))throw new BusinessException(409,"Đề xuất này đã được xử lý. Vui lòng tải lại danh sách.");
        return request;
    }
    private static String change(String from,String to) { return StaffAccountService.ROLES.get(from)+" → "+StaffAccountService.ROLES.get(to); }

    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> eligible(String q,int page,int size) {
        actors.requireManager();int offset=OperationsValidation.page(page,size);
        if(q==null)q="";if(q.length()>200)throw BusinessException.invalid();
        var rows=staff.listMovable(q.trim(),offset,size);
        var pending=requests.pendingTargets(rows.stream().map(Account::id).toList());
        var items=rows.stream().map(a->{
            var item=new LinkedHashMap<String,Object>();
            item.put("userId",a.id());item.put("fullName",a.name());item.put("email",a.email());item.put("roleLabel",StaffAccountService.ROLES.get(a.role()));
            item.put("moveToLabel",StaffAccountService.ROLES.get(MOVES.get(a.role())));item.put("pendingPromotion",pending.contains(a.id()));item.put("updatedAt",a.updatedAt().toString());return item;
        }).toList();
        return Map.of("items",items,"page",page,"size",size,"totalElements",staff.countMovable(q.trim()));
    }

    @Transactional(timeout=5)
    public Map<String,Object> move(UUID id,Move r) {
        UUID actor=beginManager();var a=accounts.checked(id,r.expectedUpdatedAt());movable(a,actor);noPending(id);
        String to=MOVES.get(a.role());int roleId=staff.activeRoleId(to);var time=accounts.nextTime(a);
        staff.changeRole(id,roleId,time);staff.audit(actor,id,"ROLE",change(a.role(),to)+". "+r.reason().trim(),time);
        return Map.of("userId",id,"roleLabel",StaffAccountService.ROLES.get(to));
    }

    @Transactional(timeout=5)
    public Map<String,Object> propose(Propose r) {
        UUID actor=beginManager();var a=accounts.checked(r.targetUserId(),r.expectedUpdatedAt());movable(a,actor);noPending(a.id());
        UUID id=UUID.randomUUID();
        requests.create(id,a.id(),actor,a.roleId(),staff.activeRoleId("ROLE_MANAGER"),r.reason().trim(),accounts.now());
        return Map.of("requestId",id,"statusLabel",STATES.get("PENDING"));
    }

    @Transactional(timeout=5)
    public Map<String,Object> cancel(UUID id) {
        UUID actor=beginManager();var request=pending(id);
        if(!actor.equals(request.requester()))throw new BusinessException(403,"Chỉ người tạo đề xuất được hủy đề xuất này.");
        requests.decide(id,"CANCELLED",actor,null,accounts.now());
        return Map.of("requestId",id,"statusLabel",STATES.get("CANCELLED"));
    }

    @Transactional(timeout=5)
    public Map<String,Object> approve(UUID id,Decide r) {
        UUID actor=accounts.beginWrite();var request=pending(id);var a=staff.get(request.target(),true);
        if(!a.status().equals("ACTIVE")||a.roleId()!=request.fromRole())throw new BusinessException(409,"Nhân viên đã bị khóa hoặc đổi vai trò sau khi đề xuất. Vui lòng từ chối đề xuất này.");
        String to=staff.activeRole(request.toRole());var time=accounts.nextTime(a);String note=r.note()==null?"":r.note().trim();
        staff.changeRole(a.id(),request.toRole(),time);requests.decide(id,"APPROVED",actor,note.isEmpty()?null:note,time);
        staff.audit(actor,a.id(),"ROLE",change(a.role(),to)+". Duyệt đề xuất của "+actors.displayName(request.requester())+": "+request.reason()+(note.isEmpty()?"":" Ghi chú: "+note),time);
        return Map.of("requestId",id,"statusLabel",STATES.get("APPROVED"));
    }

    @Transactional(timeout=5)
    public Map<String,Object> reject(UUID id,Decide r) {
        if(r.note()==null||r.note().isBlank())throw new BusinessException(400,"Vui lòng nhập lý do từ chối.");
        UUID actor=accounts.beginWrite();pending(id);requests.decide(id,"REJECTED",actor,r.note().trim(),accounts.now());
        return Map.of("requestId",id,"statusLabel",STATES.get("REJECTED"));
    }

    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> list(String status,int page,int size) {
        var actor=actors.requireReporter();int offset=OperationsValidation.page(page,size);
        if(status!=null)OperationsValidation.member(status,STATES.keySet());
        // Manager chỉ xem đề xuất của mình; Admin là người duyệt nên xem toàn bộ.
        UUID requester=actor.role().equals("ROLE_ADMIN")?null:actor.userId();
        var items=requests.list(status,requester,offset,size).stream().map(r->{
            var item=new LinkedHashMap<String,Object>();
            item.put("requestId",r[0]);item.put("targetName",r[1]);item.put("targetEmail",r[2]);item.put("requesterName",r[3]);
            item.put("fromRoleLabel",StaffAccountService.ROLES.get((String)r[4]));item.put("toRoleLabel",StaffAccountService.ROLES.get((String)r[5]));item.put("reason",r[6]);
            item.put("status",r[7]);item.put("statusLabel",STATES.get((String)r[7]));item.put("decidedByName",r[8]);item.put("decisionNote",r[9]);item.put("createdAt",r[10]);item.put("decidedAt",r[11]);
            return item;
        }).toList();
        return Map.of("items",items,"page",page,"size",size,"totalElements",requests.count(status,requester));
    }
}
