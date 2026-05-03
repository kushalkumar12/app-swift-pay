INSERT INTO users (user_seq_id, user_id, name, is_active, created_on, updated_on)
VALUES (1, 'U1001', 'Kushal', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (user_seq_id) DO NOTHING;

INSERT INTO users (user_seq_id, user_id, name, is_active, created_on, updated_on)
VALUES (2, 'U1002', 'Amit', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (user_seq_id) DO NOTHING;

INSERT INTO users (user_seq_id, user_id, name, is_active, created_on, updated_on)
VALUES (3, 'U1003', 'Priya', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (user_seq_id) DO NOTHING;

INSERT INTO users (user_seq_id, user_id, name, is_active, created_on, updated_on)
VALUES (4, 'U1004', 'Rahul', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (user_seq_id) DO NOTHING;

INSERT INTO users (user_seq_id, user_id, name, is_active, created_on, updated_on)
VALUES (5, 'U1005', 'Vishnu', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (user_seq_id) DO NOTHING;