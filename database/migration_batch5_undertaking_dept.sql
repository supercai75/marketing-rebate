-- 批次5：项目表新增承接部门字段
ALTER TABLE prj_project ADD COLUMN IF NOT EXISTS undertaking_dept VARCHAR(100) DEFAULT NULL;
COMMENT ON COLUMN prj_project.undertaking_dept IS '承接部门';
