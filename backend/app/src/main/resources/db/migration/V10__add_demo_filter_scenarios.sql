INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a26', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000005',
       'Артём', 'Кузнецов', 'Ильич', DATE '1991-07-12', 'Наладчик линии', DATE '2024-03-18',
       TIMESTAMP '2026-03-05 09:00:00', TIMESTAMP '2026-03-05 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a26');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a27', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000006',
       'Дарья', 'Фомина', 'Алексеевна', DATE '1994-02-26', 'Специалист по валидации', DATE '2023-09-04',
       TIMESTAMP '2026-03-05 09:05:00', TIMESTAMP '2026-03-05 09:05:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a27');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a28', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000007',
       'Кирилл', 'Егоров', 'Сергеевич', DATE '1988-10-03', 'Координатор поставок', DATE '2022-06-20',
       TIMESTAMP '2026-03-05 09:10:00', TIMESTAMP '2026-03-05 09:10:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a28');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a29', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000002',
       'Светлана', 'Жукова', 'Павловна', DATE '1990-12-14', 'Специалист по подбору', DATE '2024-08-12',
       TIMESTAMP '2026-03-05 09:15:00', TIMESTAMP '2026-03-05 09:15:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a29');

INSERT INTO employees (
    id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at
)
SELECT '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a30', NULL, '8c0a2b90-e2ad-4e8b-b5b8-000000000004',
       'Максим', 'Громов', 'Олегович', DATE '1997-05-08', 'Техник участка', DATE '2025-02-10',
       TIMESTAMP '2026-03-05 09:20:00', TIMESTAMP '2026-03-05 09:20:00'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a30');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000020', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a26', '4d8c9a54-1df5-4f65-91c0-000000000001',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2026-03-01', 1, 'FLU-2026-020',
       DATE '2026-09-01', NULL, DATE '2026-04-02', 'Демо-сценарий для фильтра по ближайшей ревакцинации',
       TIMESTAMP '2026-03-01 09:10:00', TIMESTAMP '2026-03-01 09:10:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000020');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000021', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a26', '4d8c9a54-1df5-4f65-91c0-000000000002',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2026-01-20', 1, 'HEP-2026-021',
       DATE '2029-01-19', DATE '2026-02-19', DATE '2029-01-20', 'Демо-сценарий для фильтра по вакцине и сотруднику',
       TIMESTAMP '2026-01-20 10:00:00', TIMESTAMP '2026-01-20 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000021');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000022', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a27', '4d8c9a54-1df5-4f65-91c0-000000000003',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2026-02-18', 1, 'TET-2026-022',
       DATE '2036-02-17', NULL, DATE '2036-02-18', 'Демо-сценарий для текущего покрытия по сотруднику',
       TIMESTAMP '2026-02-18 14:20:00', TIMESTAMP '2026-02-18 14:20:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000022');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000023', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a27', '4d8c9a54-1df5-4f65-91c0-000000000001',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2026-03-12', 1, 'FLU-2026-023',
       DATE '2026-09-11', NULL, DATE '2026-06-15', 'Демо-сценарий для фильтра по диапазону дат в реестре',
       TIMESTAMP '2026-03-12 11:05:00', TIMESTAMP '2026-03-12 11:05:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000023');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000024', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a28', '4d8c9a54-1df5-4f65-91c0-000000000001',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2026-03-25', 1, 'FLU-2026-024',
       DATE '2026-09-24', NULL, DATE '2026-04-05', 'Демо-сценарий для фильтра по дням до ревакцинации',
       TIMESTAMP '2026-03-25 08:40:00', TIMESTAMP '2026-03-25 08:40:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000024');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000025', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a29', '4d8c9a54-1df5-4f65-91c0-000000000002',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2026-01-12', 1, 'HEP-2026-025',
       DATE '2029-01-11', DATE '2026-02-11', DATE '2029-01-12', 'Демо-сценарий для покрытия по отделу кадров',
       TIMESTAMP '2026-01-12 12:15:00', TIMESTAMP '2026-01-12 12:15:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000025');

INSERT INTO vaccinations (
    id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number,
    expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at
)
SELECT '7b53d4b0-c198-4700-b23f-000000000026', '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a20', '4d8c9a54-1df5-4f65-91c0-000000000003',
       '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a12', DATE '2026-02-26', 1, 'TET-2026-026',
       DATE '2036-02-25', NULL, DATE '2036-02-26', 'Дополнительная запись для фильтра по вакцине АДС-М',
       TIMESTAMP '2026-02-26 16:30:00', TIMESTAMP '2026-02-26 16:30:00'
WHERE NOT EXISTS (SELECT 1 FROM vaccinations WHERE id = '7b53d4b0-c198-4700-b23f-000000000026');
