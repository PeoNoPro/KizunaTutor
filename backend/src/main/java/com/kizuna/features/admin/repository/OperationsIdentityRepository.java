package com.kizuna.features.admin.repository;
import com.kizuna.features.admin.dto.OperationsActor;
import com.kizuna.core.exception.BusinessException;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.*;
@Repository
public class OperationsIdentityRepository {
    private final JdbcTemplate jdbc;
    public OperationsIdentityRepository(@Qualifier("operationsIdentityJdbc") JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public OperationsActor find(String email, UUID id) {
        var rows = jdbc.query("SELECT u.user_id,u.full_name,u.email,r.role_name,u.must_change_password FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.deleted_at IS NULL AND u.account_status::text='ACTIVE' AND r.is_active=true AND " + (id == null ? "u.email=?" : "u.user_id=?"),
            (rs,n) -> new OperationsActor(rs.getObject(1,UUID.class), rs.getString(4), rs.getString(2), rs.getString(3), rs.getBoolean(5)), id == null ? email : id);
        if (rows.isEmpty()) throw new BusinessException(403,"Tài khoản hiện không có quyền sử dụng chức năng này.");
        return rows.get(0);
    }
    public String displayName(UUID id) {
        var names=jdbc.queryForList("SELECT full_name FROM users WHERE user_id=?",String.class,id);
        return names.isEmpty() ? "Tài khoản không còn được cung cấp" : names.get(0);
    }
    public String avatarUrl(UUID id) {
        var rows=jdbc.queryForList("SELECT avatar_url FROM users WHERE user_id=?",String.class,id);
        return rows.isEmpty() || rows.get(0)==null ? "" : rows.get(0);
    }
    public Map<String,Object> personalProfile(UUID id,String roleLabel) {
        return jdbc.queryForObject("SELECT full_name,email,phone,avatar_url,account_status::text,created_at FROM users WHERE user_id=? AND deleted_at IS NULL",(rs,n)->{
            var data=new LinkedHashMap<String,Object>();data.put("fullName",rs.getString(1));data.put("email",rs.getString(2));data.put("phone",rs.getString(3));data.put("avatarUrl",rs.getString(4));data.put("roleLabel",roleLabel);
            data.put("statusLabel",Map.of("ACTIVE","Đang hoạt động","SUSPENDED","Tạm khóa","BANNED","Đã bị khóa","PENDING","Chờ kích hoạt","INACTIVE","Ngừng hoạt động").getOrDefault(rs.getString(5),"Cần kiểm tra trạng thái"));data.put("createdAt",rs.getObject(6,java.time.OffsetDateTime.class).toString());return data;
        },id);
    }
    public Map<UUID,String> names(Collection<UUID> ids) {
        if(ids.isEmpty()) return Map.of();
        String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        var result=new HashMap<UUID,String>();
        jdbc.query("SELECT user_id,full_name FROM users WHERE user_id IN ("+placeholders+")",rs->{
            result.put(rs.getObject(1,UUID.class),rs.getString(2));
        },ids.toArray());
        return result;
    }
    public List<Map<String,Object>> admins() {
        return jdbc.query("SELECT u.user_id,u.full_name FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.deleted_at IS NULL AND u.account_status::text='ACTIVE' AND r.role_name='ROLE_ADMIN' AND r.is_active=true ORDER BY u.full_name,u.user_id",
                (rs,n) -> Map.of("value",rs.getObject(1),"label",rs.getString(2)));
    }
}
