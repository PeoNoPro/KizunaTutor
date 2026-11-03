-- Test-only fixture. The runner MUST verify *_ticket_test before executing.
BEGIN;
DO $$ BEGIN IF current_database() !~ '_ticket_test$' THEN RAISE EXCEPTION 'Test database required'; END IF; END $$;
INSERT INTO roles(role_name,is_active) VALUES('ROLE_LEARNER',true) ON CONFLICT(role_name) DO NOTHING;
INSERT INTO users(user_id,role_id,email,password_hash,full_name,account_status)
SELECT md5('fe10-load-user-'||i)::uuid,r.role_id,'fe10-load-'||i||'@test.invalid','disabled-test-login','Kiểm thử tải '||i,'ACTIVE'
FROM generate_series(1,100) i CROSS JOIN roles r WHERE r.role_name='ROLE_LEARNER' ON CONFLICT DO NOTHING;
INSERT INTO support_tickets(ticket_id,user_id,subject,category,priority,status,is_escalated,version,created_at,updated_at)
SELECT md5('fe10-load-ticket-'||i)::uuid,md5('fe10-load-user-'||(((i-1)/10)+1))::uuid,'Yêu cầu kiểm thử tải '||i,'OTHER','MEDIUM','OPEN',false,0,now(),now()
FROM generate_series(1,1000) i ON CONFLICT DO NOTHING;
INSERT INTO ticket_messages(message_id,ticket_id,sender_id,message_text,sent_at,sequence_no)
SELECT md5('fe10-load-message-'||i||'-'||s)::uuid,md5('fe10-load-ticket-'||i)::uuid,md5('fe10-load-user-'||(((i-1)/10)+1))::uuid,'Trao đổi ban đầu trong dữ liệu kiểm thử',now(),s
FROM generate_series(1,1000) i CROSS JOIN generate_series(1,2) s ON CONFLICT DO NOTHING;
INSERT INTO ticket_audit_events(event_id,ticket_id,actor_user_id,event_type,ticket_version,before_values,after_values)
SELECT md5('fe10-load-audit-'||i)::uuid,md5('fe10-load-ticket-'||i)::uuid,md5('fe10-load-user-'||(((i-1)/10)+1))::uuid,'CREATE',0,'{}','{"status":"OPEN","version":0}'
FROM generate_series(1,1000) i ON CONFLICT DO NOTHING;
COMMIT;
