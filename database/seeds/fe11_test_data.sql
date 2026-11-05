-- Dữ liệu cho tester FE-11. Mọi bản ghi được đánh dấu để xóa bằng fe11_test_data_cleanup.sql:
-- email đuôi @test.kizuna.invalid, gói và sự cố có chữ [TEST], cổng thanh toán TEST.
-- Tài khoản test không đăng nhập được và thư gửi tới đuôi .invalid không đến ai.
BEGIN;
CREATE OR REPLACE FUNCTION pg_temp.fe11_test_incident(p_subject TEXT,p_severity TEXT,p_state TEXT,p_assignee UUID,p_actor UUID,p_days INT) RETURNS VOID AS $fn$
DECLARE
    id UUID := gen_random_uuid();
    started TIMESTAMPTZ := now()-make_interval(days=>p_days);
    seq INT := 1;
    final_status TEXT := CASE p_state WHEN 'REOPENED' THEN 'IN_PROGRESS' ELSE p_state END;
    state_new JSONB := jsonb_build_object('status','NEW','severity',p_severity,'assigneeId',NULL,'components',jsonb_build_array('APPLICATION'));
    state_at JSONB;
BEGIN
    INSERT INTO technical_incidents(incident_id,subject,description,severity,status,reporter_id,assignee_id,created_at,updated_at,version)
        VALUES (id,p_subject,'Dữ liệu test FE-11, vui lòng bỏ qua.',p_severity,final_status,p_actor,p_assignee,started,started,0);
    INSERT INTO incident_components(incident_id,component_code) VALUES (id,'APPLICATION');
    INSERT INTO technical_incident_events(event_id,incident_id,sequence_no,actor_id,occurred_at,action,new_state,detail)
        VALUES (gen_random_uuid(),id,seq,p_actor,started,'CREATE',state_new,'Dữ liệu test FE-11, vui lòng bỏ qua.');
    IF p_state='NEW' THEN RETURN; END IF;
    state_at := state_new||jsonb_build_object('status','IN_PROGRESS','assigneeId',p_assignee::text);
    seq := seq+1;
    INSERT INTO technical_incident_events(event_id,incident_id,sequence_no,actor_id,occurred_at,action,previous_state,new_state)
        VALUES (gen_random_uuid(),id,seq,p_actor,started+interval '1 minute','CLAIM',state_new,state_at);
    IF p_state IN ('RESOLVED','CLOSED','REOPENED') THEN
        seq := seq+1;
        INSERT INTO technical_incident_events(event_id,incident_id,sequence_no,actor_id,occurred_at,action,previous_state,new_state,detail,verification_text)
            VALUES (gen_random_uuid(),id,seq,p_actor,started+interval '2 minutes','RESOLVE',state_at,state_at||jsonb_build_object('status','RESOLVED'),'Kết quả test: đã khắc phục.','Đã kiểm tra lại trên môi trường test.');
    END IF;
    IF p_state='CLOSED' THEN
        seq := seq+1;
        INSERT INTO technical_incident_events(event_id,incident_id,sequence_no,actor_id,occurred_at,action,previous_state,new_state)
            VALUES (gen_random_uuid(),id,seq,p_actor,started+interval '3 minutes','CLOSE',state_at||jsonb_build_object('status','RESOLVED'),state_at||jsonb_build_object('status','CLOSED'));
    ELSIF p_state='REOPENED' THEN
        seq := seq+1;
        INSERT INTO technical_incident_events(event_id,incident_id,sequence_no,actor_id,occurred_at,action,previous_state,new_state,reason)
            VALUES (gen_random_uuid(),id,seq,p_actor,started+interval '3 minutes','RESUME',state_at||jsonb_build_object('status','RESOLVED'),state_at,'Lỗi test tái diễn.');
    END IF;
    UPDATE technical_incidents SET version=seq-1,updated_at=started+make_interval(mins=>seq-1) WHERE incident_id=id;
END;
$fn$ LANGUAGE plpgsql;
DO $$
DECLARE
    admin_id UUID;
    manager_id UUID;
    r_learner SMALLINT; r_tutor SMALLINT; r_academic SMALLINT; r_cs SMALLINT;
    plan_month UUID := gen_random_uuid();
    plan_year UUID := gen_random_uuid();
    tutor_rows CONSTANT TEXT[][] := ARRAY[
        ['01','Gia sư Test Chưa Gửi','UNVERIFIED','N3'],
        ['02','Gia sư Test Chờ Thẩm Định','PENDING','N2'],
        ['03','Gia sư Test Đã Duyệt','APPROVED','N1'],
        ['04','Gia sư Test Từ Chối','REJECTED','N4'],
        ['05','Gia sư Test Cần Bổ Sung','ADDITIONAL_INFO_REQUIRED','N2']];
    item TEXT[];
    uid UUID;
    tid UUID;
    payer UUID;
    target UUID;
    n INT;
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email LIKE '%@test.kizuna.invalid') THEN
        RAISE EXCEPTION 'Dữ liệu test đã có. Chạy fe11_test_data_cleanup.sql trước khi nạp lại.';
    END IF;
    SELECT u.user_id INTO admin_id FROM users u JOIN roles x ON x.role_id=u.role_id WHERE x.role_name='ROLE_ADMIN' AND u.account_status::text='ACTIVE' AND u.deleted_at IS NULL ORDER BY u.created_at LIMIT 1;
    SELECT u.user_id INTO manager_id FROM users u JOIN roles x ON x.role_id=u.role_id WHERE x.role_name='ROLE_MANAGER' AND u.account_status::text='ACTIVE' AND u.deleted_at IS NULL ORDER BY u.created_at LIMIT 1;
    IF admin_id IS NULL OR manager_id IS NULL THEN RAISE EXCEPTION 'Cần có sẵn một tài khoản Admin và một tài khoản Quản lý đang hoạt động.'; END IF;
    SELECT role_id INTO r_learner FROM roles WHERE role_name='ROLE_LEARNER';
    SELECT role_id INTO r_tutor FROM roles WHERE role_name='ROLE_TUTOR';
    SELECT role_id INTO r_academic FROM roles WHERE role_name='ROLE_ACADEMIC_OFFICER';
    SELECT role_id INTO r_cs FROM roles WHERE role_name='ROLE_CS_STAFF';

    -- Nhân viên: có đủ hai vai trò, một tài khoản đang khóa, và ngày tạo trải đều cho biểu đồ tài khoản mới.
    INSERT INTO users(user_id,role_id,email,full_name,phone,password_hash,account_status,must_change_password,created_at,updated_at) VALUES
        (gen_random_uuid(),r_academic,'hocvu01@test.kizuna.invalid','Học vụ Test Một','0900000101','TEST-KHONG-DANG-NHAP','ACTIVE',true,now(),now()),
        (gen_random_uuid(),r_academic,'hocvu02@test.kizuna.invalid','Học vụ Test Hai','0900000102','TEST-KHONG-DANG-NHAP','ACTIVE',false,now()-interval '1 day',now()),
        (gen_random_uuid(),r_academic,'hocvu03@test.kizuna.invalid','Học vụ Test Đang Khóa','0900000103','TEST-KHONG-DANG-NHAP','SUSPENDED',false,now()-interval '2 days',now()),
        (gen_random_uuid(),r_cs,'cskh01@test.kizuna.invalid','CSKH Test Một','0900000201','TEST-KHONG-DANG-NHAP','ACTIVE',true,now()-interval '3 days',now()),
        (gen_random_uuid(),r_cs,'cskh02@test.kizuna.invalid','CSKH Test Hai','0900000202','TEST-KHONG-DANG-NHAP','ACTIVE',false,now()-interval '5 days',now()),
        (gen_random_uuid(),r_cs,'cskh03@test.kizuna.invalid','CSKH Test Ba','0900000203','TEST-KHONG-DANG-NHAP','ACTIVE',false,now()-interval '10 days',now()),
        (gen_random_uuid(),r_learner,'hocvien01@test.kizuna.invalid','Học viên Test Một',NULL,'TEST-KHONG-DANG-NHAP','ACTIVE',false,now()-interval '20 days',now()),
        (gen_random_uuid(),r_learner,'hocvien02@test.kizuna.invalid','Học viên Test Chờ Kích Hoạt',NULL,'TEST-KHONG-DANG-NHAP','PENDING',false,now()-interval '40 days',now());

    -- Gia sư đủ năm trạng thái thẩm định. Giữ DRAFT_PROFILE để không lọt vào trang tìm gia sư công khai.
    FOREACH item SLICE 1 IN ARRAY tutor_rows LOOP
        uid := gen_random_uuid(); tid := gen_random_uuid();
        INSERT INTO users(user_id,role_id,email,full_name,password_hash,account_status,must_change_password,created_at,updated_at)
            VALUES (uid,r_tutor,'giasu'||item[1]||'@test.kizuna.invalid',item[2],'TEST-KHONG-DANG-NHAP','ACTIVE',false,now()-interval '6 days',now());
        INSERT INTO tutors(tutor_id,user_id,highest_jlpt_level,verification_status,tutor_state,submitted_at,verified_at)
            VALUES (tid,uid,item[4],item[3]::tutor_verification_status_enum,'DRAFT_PROFILE',
                CASE WHEN item[3]='UNVERIFIED' THEN NULL ELSE now()-interval '5 days' END,
                CASE WHEN item[3]='APPROVED' THEN now()-interval '2 days' END);
        IF item[3]<>'UNVERIFIED' THEN
            INSERT INTO tutor_credentials(tutor_id,credential_type,title,file_url,verified_status) VALUES
                (tid,'JLPT','[TEST] Chứng chỉ '||item[4],'https://example.invalid/test.pdf',(CASE WHEN item[3]='APPROVED' THEN 'VERIFIED' WHEN item[3]='REJECTED' THEN 'REJECTED' ELSE 'PENDING' END)::tutor_credential_status_enum);
        END IF;
    END LOOP;

    -- Gói tắt sẵn để gia sư thật không mua nhầm.
    INSERT INTO membership_plans(plan_id,plan_name,duration_months,price,is_active) VALUES
        (plan_month,'[TEST] Gói Tháng',1,199000,false),(plan_year,'[TEST] Gói Năm',12,1490000,false);
    SELECT user_id INTO payer FROM users WHERE email='giasu03@test.kizuna.invalid';
    INSERT INTO payment_transactions(user_id,plan_id,amount,payment_gateway,status,transaction_time) VALUES
        (payer,plan_month,199000,'TEST','SUCCESS',now()),
        (payer,plan_month,199000,'TEST','SUCCESS',now()-interval '3 days'),
        (payer,plan_year,1490000,'TEST','SUCCESS',now()-interval '10 days'),
        (payer,plan_month,199000,'TEST','SUCCESS',now()-interval '40 days'),
        (payer,plan_month,199000,'TEST','REFUNDED',now()-interval '5 days'),
        (payer,plan_month,199000,'TEST','PENDING',now()-interval '1 day'),
        (payer,plan_year,1490000,'TEST','FAILED',now()-interval '1 day');

    -- Hai đề xuất chờ duyệt: một để thử duyệt, một để thử từ chối.
    FOREACH item SLICE 1 IN ARRAY ARRAY[['hocvu02@test.kizuna.invalid','Đề xuất test để thử duyệt'],['cskh02@test.kizuna.invalid','Đề xuất test để thử từ chối']] LOOP
        SELECT user_id INTO target FROM users WHERE email=item[1];
        INSERT INTO staff_role_change_requests(request_id,target_user_id,requester_user_id,from_role_id,to_role_id,reason,status,created_at)
            SELECT gen_random_uuid(),target,manager_id,u.role_id,(SELECT role_id FROM roles WHERE role_name='ROLE_MANAGER'),item[2],'PENDING',now() FROM users u WHERE u.user_id=target;
    END LOOP;

    -- Sự cố đủ vòng đời, kèm đủ lịch sử để báo cáo tính đúng.
    PERFORM pg_temp.fe11_test_incident('[TEST] Sự cố mới ghi nhận','NORMAL','NEW',NULL,admin_id,1);
    PERFORM pg_temp.fe11_test_incident('[TEST] Sự cố đang xử lý','HIGH','IN_PROGRESS',admin_id,admin_id,2);
    PERFORM pg_temp.fe11_test_incident('[TEST] Sự cố đã giải quyết','HIGH','RESOLVED',admin_id,admin_id,3);
    PERFORM pg_temp.fe11_test_incident('[TEST] Sự cố đã đóng','LOW','CLOSED',admin_id,admin_id,12);
    PERFORM pg_temp.fe11_test_incident('[TEST] Sự cố mở lại, không còn kết quả','NORMAL','REOPENED',admin_id,admin_id,4);
    PERFORM pg_temp.fe11_test_incident('=1+1 [TEST] Tiêu đề giống công thức','CRITICAL','NEW',NULL,admin_id,0);
    -- Đủ nhiều để danh sách có trang 2, kiểm tra Excel lấy hết chứ không chỉ một trang.
    FOR n IN 1..22 LOOP
        PERFORM pg_temp.fe11_test_incident('[TEST] Sự cố phân trang '||lpad(n::text,2,'0'),'LOW','NEW',NULL,admin_id,20);
    END LOOP;
END $$;
DROP FUNCTION pg_temp.fe11_test_incident(TEXT,TEXT,TEXT,UUID,UUID,INT);
COMMIT;

-- Kết quả nạp
SELECT 'nhan_vien_hoc_vien_gia_su' AS muc, count(*) AS so_luong FROM users WHERE email LIKE '%@test.kizuna.invalid'
UNION ALL SELECT 'giao_dich', count(*) FROM payment_transactions WHERE payment_gateway='TEST'
UNION ALL SELECT 'de_xuat_cho_duyet', count(*) FROM staff_role_change_requests q JOIN users t ON t.user_id=q.target_user_id WHERE t.email LIKE '%@test.kizuna.invalid' AND q.status='PENDING'
UNION ALL SELECT 'su_co', count(*) FROM technical_incidents WHERE subject LIKE '%[TEST]%';
