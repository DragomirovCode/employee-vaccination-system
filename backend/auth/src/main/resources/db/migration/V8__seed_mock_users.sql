INSERT INTO roles (code, name)
SELECT 'PERSON', 'PERSON'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PERSON');

INSERT INTO roles (code, name)
SELECT 'HR', 'HR'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'HR');

INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at)
SELECT '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a01', 'mock.user1@example.com', 'mock-hash-1', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a01');

INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at)
SELECT '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a02', 'mock.user2@example.com', 'mock-hash-2', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a02');

INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by)
SELECT
    u.id,
    r.id,
    CURRENT_TIMESTAMP,
    NULL
FROM users u
JOIN roles r ON r.code = 'PERSON'
WHERE u.id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a01'
    AND NOT EXISTS (
        SELECT 1
        FROM user_roles ur
        WHERE ur.user_id = u.id AND ur.role_id = r.id
    );

INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by)
SELECT
    u.id,
    r.id,
    CURRENT_TIMESTAMP,
    NULL
FROM users u
JOIN roles r ON r.code = 'HR'
WHERE u.id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a02'
    AND NOT EXISTS (
        SELECT 1
        FROM user_roles ur
        WHERE ur.user_id = u.id AND ur.role_id = r.id
    );
