UPDATE departments
SET name = 'Головной офис'
WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000001';

UPDATE departments
SET name = 'Отдел кадров'
WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000002';

UPDATE departments
SET name = 'Медицинская служба'
WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000003';

UPDATE departments
SET name = 'Производство'
WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000004';

UPDATE departments
SET name = 'Производственная линия А'
WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000005';

UPDATE departments
SET name = 'Отдел контроля качества'
WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000006';

UPDATE departments
SET name = 'Логистика'
WHERE id = '8c0a2b90-e2ad-4e8b-b5b8-000000000007';

UPDATE employees
SET first_name = 'Алина',
    last_name = 'Воронина',
    middle_name = 'Сергеевна',
    position = 'Системный администратор',
    updated_at = TIMESTAMP '2026-03-28 10:00:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a10';

UPDATE employees
SET first_name = 'Елена',
    last_name = 'Морозова',
    middle_name = 'Андреевна',
    position = 'Менеджер по персоналу',
    updated_at = TIMESTAMP '2026-03-28 10:01:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a11';

UPDATE employees
SET first_name = 'Марта',
    last_name = 'Лебедева',
    middle_name = 'Викторовна',
    position = 'Врач-профпатолог',
    updated_at = TIMESTAMP '2026-03-28 10:02:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a12';

UPDATE employees
SET first_name = 'Пётр',
    last_name = 'Орлов',
    middle_name = 'Алексеевич',
    position = 'Оператор станка',
    updated_at = TIMESTAMP '2026-03-28 10:03:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a13';

UPDATE employees
SET first_name = 'Полина',
    last_name = 'Смирнова',
    middle_name = 'Игоревна',
    position = 'Старший оператор',
    updated_at = TIMESTAMP '2026-03-28 10:04:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a14';

UPDATE employees
SET first_name = 'Виктор',
    last_name = 'Белов',
    middle_name = 'Павлович',
    position = 'Специалист склада',
    updated_at = TIMESTAMP '2026-03-28 10:05:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a15';

UPDATE employees
SET first_name = 'Ольга',
    last_name = 'Каменева',
    middle_name = 'Романовна',
    position = 'Инженер по качеству',
    updated_at = TIMESTAMP '2026-03-28 10:06:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a20';

UPDATE employees
SET first_name = 'Илья',
    last_name = 'Колесников',
    middle_name = 'Дмитриевич',
    position = 'Старший аналитик качества',
    updated_at = TIMESTAMP '2026-03-28 10:07:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a21';

UPDATE employees
SET first_name = 'Леонид',
    last_name = 'Новиков',
    middle_name = 'Олегович',
    position = 'Водитель погрузчика',
    updated_at = TIMESTAMP '2026-03-28 10:08:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a22';

UPDATE employees
SET first_name = 'София',
    last_name = 'Морозова',
    middle_name = 'Михайловна',
    position = 'Специалист по упаковке',
    updated_at = TIMESTAMP '2026-03-28 10:09:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a23';

UPDATE employees
SET first_name = 'Нина',
    last_name = 'Петрова',
    middle_name = 'Петровна',
    position = 'Лаборант',
    updated_at = TIMESTAMP '2026-03-28 10:10:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a24';

UPDATE employees
SET first_name = 'Роман',
    last_name = 'Сидоров',
    middle_name = 'Сергеевич',
    position = 'Рекрутер',
    updated_at = TIMESTAMP '2026-03-28 10:11:00'
WHERE id = '018f4fd2-85f8-7f2e-b95e-9df7ac8e3a25';

UPDATE vaccines
SET name = 'Гриппол Плюс',
    manufacturer = 'Петровакс Фарм'
WHERE id = '4d8c9a54-1df5-4f65-91c0-000000000001';

UPDATE vaccines
SET name = 'Регевак B',
    manufacturer = 'Биннофарм'
WHERE id = '4d8c9a54-1df5-4f65-91c0-000000000002';

UPDATE vaccines
SET name = 'АДС-М',
    manufacturer = 'Микроген'
WHERE id = '4d8c9a54-1df5-4f65-91c0-000000000003';
