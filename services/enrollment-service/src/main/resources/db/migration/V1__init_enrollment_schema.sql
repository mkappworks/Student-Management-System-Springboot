CREATE TABLE IF NOT EXISTS enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    module_id BIGINT NOT NULL,
    module_name VARCHAR(200),
    module_code VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ENROLLED',
    academic_year VARCHAR(20),
    semester INTEGER,
    enrollment_date DATE,
    drop_date DATE,
    drop_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, module_id, academic_year, semester)
);

CREATE INDEX idx_enrollments_student_id ON enrollments(student_id);
CREATE INDEX idx_enrollments_module_id ON enrollments(module_id);
