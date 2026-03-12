INSERT INTO roles (code, name)
SELECT 'PERSON', 'PERSON'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PERSON');

INSERT INTO roles (code, name)
SELECT 'HR', 'HR'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'HR');

INSERT INTO roles (code, name)
SELECT 'MEDICAL', 'MEDICAL'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'MEDICAL');

INSERT INTO roles (code, name)
SELECT 'ADMIN', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ADMIN');

INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at)
SELECT '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10', 'admin@evs.local', 'seed-hash-admin', TRUE, TIMESTAMP '2026-03-01 09:00:00', TIMESTAMP '2026-03-01 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10');

INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at)
SELECT '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a11', 'hr@evs.local', 'seed-hash-hr', TRUE, TIMESTAMP '2026-03-01 09:05:00', TIMESTAMP '2026-03-01 09:05:00'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a11');

INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at)
SELECT '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', 'medical@evs.local', 'seed-hash-medical', TRUE, TIMESTAMP '2026-03-01 09:10:00', TIMESTAMP '2026-03-01 09:10:00'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12');

INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at)
SELECT '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a13', 'employee.one@evs.local', 'seed-hash-employee-1', TRUE, TIMESTAMP '2026-03-01 09:15:00', TIMESTAMP '2026-03-01 09:15:00'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a13');

INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at)
SELECT '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14', 'employee.two@evs.local', 'seed-hash-employee-2', TRUE, TIMESTAMP '2026-03-01 09:20:00', TIMESTAMP '2026-03-01 09:20:00'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14');

INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, TIMESTAMP '2026-03-01 09:00:00', NULL
FROM users u
JOIN roles r ON r.code = 'ADMIN'
WHERE u.id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10'
    AND NOT EXISTS (
        SELECT 1
        FROM user_roles ur
        WHERE ur.user_id = u.id AND ur.role_id = r.id
    );

INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, TIMESTAMP '2026-03-01 09:05:00', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10'
FROM users u
JOIN roles r ON r.code = 'HR'
WHERE u.id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a11'
    AND NOT EXISTS (
        SELECT 1
        FROM user_roles ur
        WHERE ur.user_id = u.id AND ur.role_id = r.id
    );

INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, TIMESTAMP '2026-03-01 09:10:00', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10'
FROM users u
JOIN roles r ON r.code = 'MEDICAL'
WHERE u.id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12'
    AND NOT EXISTS (
        SELECT 1
        FROM user_roles ur
        WHERE ur.user_id = u.id AND ur.role_id = r.id
    );

INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, TIMESTAMP '2026-03-01 09:15:00', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10'
FROM users u
JOIN roles r ON r.code = 'PERSON'
WHERE u.id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a13'
    AND NOT EXISTS (
        SELECT 1
        FROM user_roles ur
        WHERE ur.user_id = u.id AND ur.role_id = r.id
    );

INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, TIMESTAMP '2026-03-01 09:20:00', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10'
FROM users u
JOIN roles r ON r.code = 'PERSON'
WHERE u.id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14'
    AND NOT EXISTS (
        SELECT 1
        FROM user_roles ur
        WHERE ur.user_id = u.id AND ur.role_id = r.id
    );

INSERT INTO departments (id, name, parent_id, created_at)
SELECT '8c0a2b90-e2ad-4e8b-b5b8-000000000001', 'Head Office', NULL, TIMESTAMP '2026-03-01 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000001');

INSERT INTO departments (id, name, parent_id, created_at)
SELECT '8c0a2b90-e2ad-4e8b-b5b8-000000000002', 'HR Department', '8c0a2b90-e2ad-4e8b-b5b8-000000000001', TIMESTAMP '2026-03-01 08:05:00'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000002');

INSERT INTO departments (id, name, parent_id, created_at)
SELECT '8c0a2b90-e2ad-4e8b-b5b8-000000000003', 'Medical Service', '8c0a2b90-e2ad-4e8b-b5b8-000000000001', TIMESTAMP '2026-03-01 08:10:00'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000003');

INSERT INTO departments (id, name, parent_id, created_at)
SELECT '8c0a2b90-e2ad-4e8b-b5b8-000000000004', 'Production', '8c0a2b90-e2ad-4e8b-b5b8-000000000001', TIMESTAMP '2026-03-01 08:15:00'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000004');

INSERT INTO departments (id, name, parent_id, created_at)
SELECT '8c0a2b90-e2ad-4e8b-b5b8-000000000005', 'Production Line A', '8c0a2b90-e2ad-4e8b-b5b8-000000000004', TIMESTAMP '2026-03-01 08:20:00'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000005');

INSERT INTO departments (id, name, parent_id, created_at)
SELECT '8c0a2b90-e2ad-4e8b-b5b8-000000000006', 'Quality Assurance', '8c0a2b90-e2ad-4e8b-b5b8-000000000001', TIMESTAMP '2026-03-01 08:25:00'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000006');

INSERT INTO departments (id, name, parent_id, created_at)
SELECT '8c0a2b90-e2ad-4e8b-b5b8-000000000007', 'Logistics', '8c0a2b90-e2ad-4e8b-b5b8-000000000001', TIMESTAMP '2026-03-01 08:30:00'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000007');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a10', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10', '8c0a2b90-e2ad-4e8b-b5b8-000000000001',
       'Alice', 'Admin', NULL, DATE '1988-04-12', 'System Administrator', DATE '2021-02-01',
       TIMESTAMP '2026-03-01 10:00:00', TIMESTAMP '2026-03-01 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a10');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a11', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a11', '8c0a2b90-e2ad-4e8b-b5b8-000000000002',
       'Helen', 'Hr', NULL, DATE '1990-08-21', 'HR Manager', DATE '2022-01-10',
       TIMESTAMP '2026-03-01 10:05:00', TIMESTAMP '2026-03-01 10:05:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a11');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a12', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', '8c0a2b90-e2ad-4e8b-b5b8-000000000003',
       'Megan', 'Medic', NULL, DATE '1985-11-05', 'Occupational Physician', DATE '2020-09-15',
       TIMESTAMP '2026-03-01 10:10:00', TIMESTAMP '2026-03-01 10:10:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a12');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a13', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a13', '8c0a2b90-e2ad-4e8b-b5b8-000000000005',
       'Peter', 'Person', NULL, DATE '1995-02-14', 'Machine Operator', DATE '2023-05-03',
       TIMESTAMP '2026-03-01 10:15:00', TIMESTAMP '2026-03-01 10:15:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a13');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a14', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14', '8c0a2b90-e2ad-4e8b-b5b8-000000000005',
       'Paula', 'Person', NULL, DATE '1993-06-18', 'Senior Operator', DATE '2022-11-18',
       TIMESTAMP '2026-03-01 10:20:00', TIMESTAMP '2026-03-01 10:20:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a14');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a15', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000004',
       'Victor', 'Visitor', NULL, DATE '1982-09-30', 'Warehouse Specialist', DATE '2024-01-09',
       TIMESTAMP '2026-03-01 10:25:00', TIMESTAMP '2026-03-01 10:25:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a15');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a20', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000006',
       'Olivia', 'Stone', NULL, DATE '1991-01-17', 'QA Engineer', DATE '2023-02-13',
       TIMESTAMP '2026-03-01 10:30:00', TIMESTAMP '2026-03-01 10:30:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a20');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a21', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000006',
       'Ethan', 'Cole', NULL, DATE '1989-07-09', 'Senior QA Analyst', DATE '2021-06-01',
       TIMESTAMP '2026-03-01 10:35:00', TIMESTAMP '2026-03-01 10:35:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a21');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a22', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000007',
       'Liam', 'Novak', NULL, DATE '1987-12-03', 'Forklift Driver', DATE '2020-04-20',
       TIMESTAMP '2026-03-01 10:40:00', TIMESTAMP '2026-03-01 10:40:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a22');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a23', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000005',
       'Sofia', 'Moroz', NULL, DATE '1994-03-11', 'Packaging Specialist', DATE '2024-02-01',
       TIMESTAMP '2026-03-01 10:45:00', TIMESTAMP '2026-03-01 10:45:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a23');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a24', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000003',
       'Nina', 'Petrova', NULL, DATE '1996-05-29', 'Lab Assistant', DATE '2025-01-15',
       TIMESTAMP '2026-03-01 10:50:00', TIMESTAMP '2026-03-01 10:50:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a24');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a25', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000002',
       'Roman', 'Sidorov', NULL, DATE '1992-10-08', 'Recruiter', DATE '2022-09-05',
       TIMESTAMP '2026-03-01 10:55:00', TIMESTAMP '2026-03-01 10:55:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a25');

INSERT INTO diseases (name, description)
SELECT 'Influenza', 'Seasonal influenza prevention'
WHERE NOT EXISTS (SELECT 1 FROM diseases WHERE name = 'Influenza');

INSERT INTO diseases (name, description)
SELECT 'Hepatitis B', 'Prevention of hepatitis B infection'
WHERE NOT EXISTS (SELECT 1 FROM diseases WHERE name = 'Hepatitis B');

INSERT INTO diseases (name, description)
SELECT 'Tetanus', 'Booster protection for tetanus'
WHERE NOT EXISTS (SELECT 1 FROM diseases WHERE name = 'Tetanus');

INSERT INTO vaccines (id, name, manufacturer, validity_days, doses_required, days_between, is_active, created_at)
SELECT '4d8c9a54-1df5-4f65-91c0-000000000001', 'FluProtect', 'PharmaOne', 365, 1, NULL, TRUE, TIMESTAMP '2026-03-01 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccines WHERE id = '4d8c9a54-1df5-4f65-91c0-000000000001');

INSERT INTO vaccines (id, name, manufacturer, validity_days, doses_required, days_between, is_active, created_at)
SELECT '4d8c9a54-1df5-4f65-91c0-000000000002', 'HepaGuard', 'BioHealth', 1095, 3, 30, TRUE, TIMESTAMP '2026-03-01 11:05:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccines WHERE id = '4d8c9a54-1df5-4f65-91c0-000000000002');

INSERT INTO vaccines (id, name, manufacturer, validity_days, doses_required, days_between, is_active, created_at)
SELECT '4d8c9a54-1df5-4f65-91c0-000000000003', 'Tetavax', 'ImmuniLab', 3650, 1, NULL, TRUE, TIMESTAMP '2026-03-01 11:10:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccines WHERE id = '4d8c9a54-1df5-4f65-91c0-000000000003');

INSERT INTO vaccine_diseases (vaccine_id, disease_id)
SELECT '4d8c9a54-1df5-4f65-91c0-000000000001', d.id
FROM diseases d
WHERE d.name = 'Influenza'
    AND NOT EXISTS (
        SELECT 1
        FROM vaccine_diseases vd
        WHERE vd.vaccine_id = '4d8c9a54-1df5-4f65-91c0-000000000001' AND vd.disease_id = d.id
    );

INSERT INTO vaccine_diseases (vaccine_id, disease_id)
SELECT '4d8c9a54-1df5-4f65-91c0-000000000002', d.id
FROM diseases d
WHERE d.name = 'Hepatitis B'
    AND NOT EXISTS (
        SELECT 1
        FROM vaccine_diseases vd
        WHERE vd.vaccine_id = '4d8c9a54-1df5-4f65-91c0-000000000002' AND vd.disease_id = d.id
    );

INSERT INTO vaccine_diseases (vaccine_id, disease_id)
SELECT '4d8c9a54-1df5-4f65-91c0-000000000003', d.id
FROM diseases d
WHERE d.name = 'Tetanus'
    AND NOT EXISTS (
        SELECT 1
        FROM vaccine_diseases vd
        WHERE vd.vaccine_id = '4d8c9a54-1df5-4f65-91c0-000000000003' AND vd.disease_id = d.id
    );

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000001', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a13', '4d8c9a54-1df5-4f65-91c0-000000000001',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2025-10-01', 1, 'FLU-2025-001',
       DATE '2026-09-30', NULL, DATE '2026-10-01', 'Annual influenza shot',
       TIMESTAMP '2025-10-01 09:30:00', TIMESTAMP '2025-10-01 09:30:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000001');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000002', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a14', '4d8c9a54-1df5-4f65-91c0-000000000001',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2025-03-20', 1, 'FLU-2025-002',
       DATE '2026-03-19', NULL, DATE '2026-03-20', 'Employee due for revaccination',
       TIMESTAMP '2025-03-20 10:00:00', TIMESTAMP '2025-03-20 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000002');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000003', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a13', '4d8c9a54-1df5-4f65-91c0-000000000002',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2026-02-01', 1, 'HEP-2026-001',
       DATE '2027-01-31', DATE '2026-03-03', DATE '2029-02-01', 'Dose 1 of hepatitis B series',
       TIMESTAMP '2026-02-01 11:00:00', TIMESTAMP '2026-02-01 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000003');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000010', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a23', '4d8c9a54-1df5-4f65-91c0-000000000001',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2025-11-15', 1, 'FLU-2025-010',
       DATE '2026-11-14', NULL, DATE '2026-11-15', 'Seasonal influenza protection for packaging line',
       TIMESTAMP '2025-11-15 09:00:00', TIMESTAMP '2025-11-15 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000010');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000011', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a24', '4d8c9a54-1df5-4f65-91c0-000000000003',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2024-06-10', 1, 'TET-2024-011',
       DATE '2034-06-08', NULL, DATE '2034-06-10', 'Initial tetanus booster for lab onboarding',
       TIMESTAMP '2024-06-10 14:15:00', TIMESTAMP '2024-06-10 14:15:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000011');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000012', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a25', '4d8c9a54-1df5-4f65-91c0-000000000002',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2026-01-15', 1, 'HEP-2026-012',
       DATE '2029-01-14', DATE '2026-02-14', DATE '2029-01-15', 'Hepatitis B course started for recruiter',
       TIMESTAMP '2026-01-15 13:00:00', TIMESTAMP '2026-01-15 13:00:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000012');

INSERT INTO documents (id, vaccination_id, file_name, file_path, file_size, mime_type, uploaded_by, uploaded_at)
SELECT '2fd5c3ec-0ee2-48ec-9d7a-000000000001', '7b53d4b0-c198-4700-b23f-000000000001', 'flu-certificate-peter.pdf',
       'seed/flu-certificate-peter.pdf', 24576, 'application/pdf', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12',
       TIMESTAMP '2025-10-01 09:40:00'
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = '2fd5c3ec-0ee2-48ec-9d7a-000000000001');

INSERT INTO documents (id, vaccination_id, file_name, file_path, file_size, mime_type, uploaded_by, uploaded_at)
SELECT '2fd5c3ec-0ee2-48ec-9d7a-000000000002', '7b53d4b0-c198-4700-b23f-000000000002', 'flu-certificate-paula.pdf',
       'seed/flu-certificate-paula.pdf', 28672, 'application/pdf', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12',
       TIMESTAMP '2025-03-20 10:10:00'
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = '2fd5c3ec-0ee2-48ec-9d7a-000000000002');

INSERT INTO documents (id, vaccination_id, file_name, file_path, file_size, mime_type, uploaded_by, uploaded_at)
SELECT '2fd5c3ec-0ee2-48ec-9d7a-000000000010', '7b53d4b0-c198-4700-b23f-000000000010', 'flu-certificate-sofia.pdf',
       'seed/flu-certificate-sofia.pdf', 19876, 'application/pdf', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12',
       TIMESTAMP '2025-11-15 09:10:00'
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = '2fd5c3ec-0ee2-48ec-9d7a-000000000010');

INSERT INTO documents (id, vaccination_id, file_name, file_path, file_size, mime_type, uploaded_by, uploaded_at)
SELECT '2fd5c3ec-0ee2-48ec-9d7a-000000000011', '7b53d4b0-c198-4700-b23f-000000000012', 'hepatitis-dose1-roman.pdf',
       'seed/hepatitis-dose1-roman.pdf', 22104, 'application/pdf', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12',
       TIMESTAMP '2026-01-15 13:12:00'
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = '2fd5c3ec-0ee2-48ec-9d7a-000000000011');

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, read_at, payload)
SELECT '6f1f9d6c-65ec-4706-8233-000000000001', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a13', 'REVACCINATION_DUE',
       'Vaccination series continuation', 'Next hepatitis B dose is scheduled for 2026-03-03.', TRUE,
       TIMESTAMP '2026-03-12 08:00:00', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE id = '6f1f9d6c-65ec-4706-8233-000000000001');

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, read_at, payload)
SELECT '6f1f9d6c-65ec-4706-8233-000000000002', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a13', 'SYSTEM',
       'Medical document available', 'Your influenza vaccination certificate is available for download.', FALSE,
       TIMESTAMP '2026-03-11 09:45:00', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE id = '6f1f9d6c-65ec-4706-8233-000000000002');

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, read_at, payload)
SELECT '6f1f9d6c-65ec-4706-8233-000000000003', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a13', 'SYSTEM',
       'Profile synced', 'Your employee profile was synced with the updated demo organization tree.', TRUE,
       TIMESTAMP '2026-03-10 18:00:00', TIMESTAMP '2026-03-10 18:15:00', NULL
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE id = '6f1f9d6c-65ec-4706-8233-000000000003');

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, read_at, payload)
SELECT '6f1f9d6c-65ec-4706-8233-000000000004', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a13', 'SYSTEM',
       'Health check reminder', 'Please confirm your annual occupational health check this week.', FALSE,
       TIMESTAMP '2026-03-09 10:30:00', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE id = '6f1f9d6c-65ec-4706-8233-000000000004');

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, read_at, payload)
SELECT '6f1f9d6c-65ec-4706-8233-000000000005', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a13', 'SYSTEM',
       'Vaccination room booking', 'Your follow-up appointment is reserved for 2026-03-17 at 10:00.', FALSE,
       TIMESTAMP '2026-03-08 15:20:00', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE id = '6f1f9d6c-65ec-4706-8233-000000000005');

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, read_at, payload)
SELECT '6f1f9d6c-65ec-4706-8233-000000000006', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14', 'REVACCINATION_DUE',
       'Revaccination due soon', 'Influenza revaccination is due on 2026-03-20.', FALSE,
       TIMESTAMP '2026-03-12 08:00:00', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE id = '6f1f9d6c-65ec-4706-8233-000000000006');

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, read_at, payload)
SELECT '6f1f9d6c-65ec-4706-8233-000000000007', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14', 'SYSTEM',
       'Certificate uploaded', 'Your influenza vaccination certificate has been attached to the record.', TRUE,
       TIMESTAMP '2026-03-11 08:35:00', TIMESTAMP '2026-03-11 09:00:00', NULL
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE id = '6f1f9d6c-65ec-4706-8233-000000000007');

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, read_at, payload)
SELECT '6f1f9d6c-65ec-4706-8233-000000000008', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14', 'SYSTEM',
       'Reminder settings updated', 'Demo reminder settings were enabled for your vaccination schedule.', FALSE,
       TIMESTAMP '2026-03-10 12:20:00', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE id = '6f1f9d6c-65ec-4706-8233-000000000008');

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, read_at, payload)
SELECT '6f1f9d6c-65ec-4706-8233-000000000009', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14', 'SYSTEM',
       'Medical office reminder', 'Please bring your previous vaccination certificate to the next visit.', FALSE,
       TIMESTAMP '2026-03-09 16:40:00', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE id = '6f1f9d6c-65ec-4706-8233-000000000009');

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, read_at, payload)
SELECT '6f1f9d6c-65ec-4706-8233-000000000010', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14', 'SYSTEM',
       'Appointment confirmed', 'Your revaccination appointment is booked for 2026-03-18 at 09:30.', TRUE,
       TIMESTAMP '2026-03-02 08:00:00', TIMESTAMP '2026-03-02 12:00:00', NULL
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE id = '6f1f9d6c-65ec-4706-8233-000000000010');

INSERT INTO audit_log (id, user_id, action, entity_type, entity_id, old_value, new_value, created_at)
SELECT '5a4e9828-89c8-4609-a4d3-000000000001', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10', 'CREATE', 'USER',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a14', NULL, NULL, TIMESTAMP '2026-03-01 09:21:00'
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE id = '5a4e9828-89c8-4609-a4d3-000000000001');

INSERT INTO audit_log (id, user_id, action, entity_type, entity_id, old_value, new_value, created_at)
SELECT '5a4e9828-89c8-4609-a4d3-000000000002', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', 'CREATE', 'VACCINATION',
       '7b53d4b0-c198-4700-b23f-000000000002', NULL, NULL, TIMESTAMP '2025-03-20 10:01:00'
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE id = '5a4e9828-89c8-4609-a4d3-000000000002');

INSERT INTO audit_log (id, user_id, action, entity_type, entity_id, old_value, new_value, created_at)
SELECT '5a4e9828-89c8-4609-a4d3-000000000010', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a11', 'CREATE', 'EMPLOYEE',
       '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a25', NULL, NULL, TIMESTAMP '2026-03-01 10:56:00'
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE id = '5a4e9828-89c8-4609-a4d3-000000000010');

INSERT INTO audit_log (id, user_id, action, entity_type, entity_id, old_value, new_value, created_at)
SELECT '5a4e9828-89c8-4609-a4d3-000000000011', '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', 'CREATE', 'VACCINATION',
       '7b53d4b0-c198-4700-b23f-000000000010', NULL, NULL, TIMESTAMP '2025-11-15 09:01:00'
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE id = '5a4e9828-89c8-4609-a4d3-000000000011');
