-- Add an optional time window to classes (used by contests, type=2).
--
-- Modes: baseline-on-migrate=true, baseline-version=0. V1 is the no-op baseline,
-- V2 seeds problems, so the first real schema change lands here as V3.
ALTER TABLE classes
    ADD COLUMN start_time datetime NULL COMMENT '开始时间' AFTER status,
    ADD COLUMN end_time datetime NULL COMMENT '结束时间' AFTER start_time;
