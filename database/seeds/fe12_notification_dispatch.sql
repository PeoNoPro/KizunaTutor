SELECT 'technical_incidents' AS bang, count(*) FROM technical_incidents
UNION ALL SELECT 'technical_incident_events', count(*) FROM technical_incident_events
UNION ALL SELECT 'incident_components', count(*) FROM incident_components
UNION ALL SELECT 'staff_role_change_requests', count(*) FROM staff_role_change_requests
UNION ALL SELECT 'support_shifts', count(*) FROM support_shifts
UNION ALL SELECT 'support_shift_events', count(*) FROM support_shift_events;
