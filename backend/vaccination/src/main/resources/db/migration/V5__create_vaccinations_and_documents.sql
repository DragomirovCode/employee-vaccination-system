CREATE TABLE vaccinations (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    vaccine_id UUID NOT NULL,
    performed_by UUID NOT NULL,
    vaccination_date DATE NOT NULL,
    dose_number INTEGER NOT NULL,
    batch_number VARCHAR(255) NULL,
    expiration_date DATE NULL,
    next_dose_date DATE NULL,
    revaccination_date DATE NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_vaccinations_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_vaccinations_vaccine FOREIGN KEY (vaccine_id) REFERENCES vaccines(id),
    CONSTRAINT fk_vaccinations_performed_by FOREIGN KEY (performed_by) REFERENCES users(id)
);

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    vaccination_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    uploaded_by UUID NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_documents_vaccination FOREIGN KEY (vaccination_id) REFERENCES vaccinations(id),
    CONSTRAINT fk_documents_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id)
);
