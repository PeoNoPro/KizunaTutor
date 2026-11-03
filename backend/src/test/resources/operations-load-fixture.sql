-- Test-only fixture; never used by application runtime.
DO $$ BEGIN IF current_database() !~ 'operations_test$' THEN RAISE EXCEPTION 'Dedicated operations_test database required'; END IF; END $$;
BEGIN;
TRUNCATE technical_incident_events,incident_components,technical_incidents;
INSERT INTO technical_incidents(incident_id,subject,description,severity,status,reporter_id,assignee_id,created_at,updated_at,version)
SELECT md5('fe11-load-'||n)::uuid,'Sự cố kiểm thử tải '||n,'Fixture cách ly, không phải dữ liệu vận hành',
    (ARRAY['LOW','NORMAL','HIGH','CRITICAL'])[n%4+1],(ARRAY['NEW','IN_PROGRESS','RESOLVED','CLOSED'])[n%4+1],
    '00000000-0000-0000-0000-000000000001',CASE WHEN n%4=0 THEN NULL ELSE '00000000-0000-0000-0000-000000000001'::uuid END,
    now()-interval '1 day',now()-interval '1 hour',n%4
FROM generate_series(1,1000) n;
INSERT INTO incident_components SELECT incident_id,'DATABASE' FROM technical_incidents;
INSERT INTO technical_incident_events(event_id,incident_id,sequence_no,actor_id,occurred_at,action,new_state)
SELECT gen_random_uuid(),i.incident_id,s,'00000000-0000-0000-0000-000000000001',i.created_at+s*interval '1 minute',
    (ARRAY['CREATE','CLAIM','RESOLVE','CLOSE'])[s],
    jsonb_build_object('status',(ARRAY['NEW','IN_PROGRESS','RESOLVED','CLOSED'])[s], 'severity',i.severity,
      'components',jsonb_build_array('DATABASE'),'assigneeId',CASE WHEN s=1 THEN NULL ELSE '00000000-0000-0000-0000-000000000001' END)
FROM technical_incidents i CROSS JOIN LATERAL generate_series(1,i.version::integer+1) s;
COMMIT;
