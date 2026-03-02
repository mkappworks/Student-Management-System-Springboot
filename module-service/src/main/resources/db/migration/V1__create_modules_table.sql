CREATE TABLE IF NOT EXISTS modules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    credits INTEGER NOT NULL DEFAULT 3,
    teacher_id UUID,
    max_students INTEGER,
    current_enrollment INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'FULL', 'COMPLETED', 'CANCELLED')),
    semester VARCHAR(20),
    academic_year VARCHAR(10),
    start_date DATE,
    end_date DATE,
    location VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_modules_code ON modules(code);
CREATE INDEX idx_modules_teacher_id ON modules(teacher_id);
CREATE INDEX idx_modules_status ON modules(status);
