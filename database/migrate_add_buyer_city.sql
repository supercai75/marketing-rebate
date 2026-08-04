-- 添加上游流向和下游流向的采购方城市字段

-- 上游流向表添加采购方城市
ALTER TABLE flow_upstream_record ADD COLUMN buyer_city VARCHAR(100) DEFAULT NULL COMMENT '采购方城市';

-- 下游流向表添加采购方城市
ALTER TABLE flow_downstream_record ADD COLUMN buyer_city VARCHAR(100) DEFAULT NULL COMMENT '采购方城市';
