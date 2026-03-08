CREATE TABLE IF NOT EXISTS teachers (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    address TEXT,
    profile_picture VARCHAR(500),
    department VARCHAR(200) NOT NULL,
    qualification VARCHAR(200),
    specialization VARCHAR(200),
    years_of_experience INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    employment_type VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS teacher_modules (
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    module_id BIGINT NOT NULL,
    PRIMARY KEY (teacher_id, module_id)
);

CREATE INDEX idx_teachers_email ON teachers(email);
CREATE INDEX idx_teachers_department ON teachers(department);
