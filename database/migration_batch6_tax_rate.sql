-- 批次6：应收/应付表新增税率字段
ALTER TABLE prj_receivable ADD COLUMN IF NOT EXISTS tax_rate DECIMAL(5,2) DEFAULT 0;
COMMENT ON COLUMN prj_receivable.tax_rate IS '税率(百分比,如13表示13%)';

ALTER TABLE prj_payable ADD COLUMN IF NOT EXISTS tax_rate DECIMAL(5,2) DEFAULT 0;
COMMENT ON COLUMN prj_payable.tax_rate IS '税率(百分比,如13表示13%)';
