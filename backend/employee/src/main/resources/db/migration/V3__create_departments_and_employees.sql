CREATE TABLE departments (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id UUID NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_departments_parent FOREIGN KEY (parent_id) REFERENCES departments(id)
);

CREATE TABLE employees (
    id UUID PRIMARY KEY,
    user_id UUID NULL,
    department_id UUID NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NULL,
    birth_date DATE NULL,
    position VARCHAR(255) NULL,
    hire_date DATE NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT uk_employees_user UNIQUE (user_id)
);
