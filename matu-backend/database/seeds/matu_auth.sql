-- Reference roles only. Run on a fresh bootstrap; no users or passwords are seeded.
INSERT INTO roles (id, role_code, role_name, role_desc, status)
VALUES (1, 'USER', '普通用户', '平台基础用户', 1),
       (2, 'ADMIN', '管理员', '平台后台管理', 1),
       (3, 'TEACHER', '讲师', '发布并管理自己的课程', 1),
       (4, 'VIP', '会员', '会员角色，实际权益仍校验有效期', 1);
