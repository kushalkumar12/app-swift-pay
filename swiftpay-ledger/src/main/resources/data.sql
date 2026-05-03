INSERT INTO account (acc_seq_id, user_id, balance)
VALUES (1, 'U1001', 100000.00)
ON CONFLICT (acc_seq_id) DO NOTHING;

INSERT INTO account (acc_seq_id, user_id, balance)
VALUES (2, 'U1002', 50000.00)
ON CONFLICT (acc_seq_id) DO NOTHING;

INSERT INTO account (acc_seq_id, user_id, balance)
VALUES (3, 'U1003', 0.00)
ON CONFLICT (acc_seq_id) DO NOTHING;

INSERT INTO account (acc_seq_id, user_id, balance)
VALUES (4, 'U1004', 750000000.50)
ON CONFLICT (acc_seq_id) DO NOTHING;

INSERT INTO account (acc_seq_id, user_id, balance)
VALUES (5, 'U1005', 00.50)
ON CONFLICT (acc_seq_id) DO NOTHING;