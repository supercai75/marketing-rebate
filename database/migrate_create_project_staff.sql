-- 项目作业人员表
CREATE TABLE IF NOT EXISTS prj_project_staff (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    user_code VARCHAR(50),
    dept_name VARCHAR(200),
    position VARCHAR(100),
    work_type VARCHAR(20) NOT NULL DEFAULT 'FULL',
    labor_cost_ratio DECIMAL(10,4),
    expense_ratio DECIMAL(10,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_project_staff_project ON prj_project_staff(project_id);
