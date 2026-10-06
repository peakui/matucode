-- VIP-only interview questions.
--
-- is_locked = 1 and unlock_days = 0 means the content/answer are only returned to
-- VIP users (or admins); non-VIP callers get title only. unlock_days = 0 disables
-- the check-in-days fallback in InterviewSecurityService.canViewLockedContent.
--
-- Idempotent on the unique question_no, so re-applying (or Flyway re-running it)
-- is safe. Explicit ids stay in a small range that cannot collide with the
-- snowflake ids assigned by the application.

INSERT IGNORE INTO interview_questions
  (id, question_no, title, content, answer, category_id, company_id, publisher_id,
   position_tags, difficulty, frequency, view_count, collect_count, is_locked, unlock_days, status)
VALUES
  (1001, 'INTV-201-001',
   '结合 JVM 内存结构，说明一次 Full GC 的触发链路，并给出线上排查频繁 Full GC 的完整思路。',
   '线上服务出现周期性卡顿，监控显示每 30 秒触发一次 Full GC、单次停顿 800ms。请回答：\n1. 从对象分配、晋升、老年代填充的角度，说明一次 Full GC 的完整触发链路；\n2. 有哪些常见的对象"过早晋升"或"内存泄漏"场景会导致频繁 Full GC？\n3. 给出你的排查步骤：你会看哪些指标、开哪些参数、用什么工具定位到具体代码？\n4. 针对大对象、缓存、ThreadLocal 分别给出优化建议。',
   '## 触发链路\n正常对象在 Eden 分配，Minor GC 后存活对象进 Survivor，年龄达到阈值（默认 15，或动态年龄判定）后晋升老年代。当出现以下情况会触发 Full GC：\n- 老年代剩余空间不足，Minor GC 前的担保失败（promotion failed）；\n- 大对象直接进老年代，撑爆老年代；\n- 显式调用 System.gc()（未禁用时）；\n- CMS 的 concurrent mode failure；\n- Metaspace 扩容失败。\n\n## 频繁 Full GC 的常见根因\n- 过早晋升：Survivor 太小或 -XX:MaxTenuringThreshold 太小，短生命周期对象被推到老年代；\n- 内存泄漏：静态集合、未清理的 ThreadLocal、监听器/缓存无上限，对象只增不减；\n- 大对象/大数组频繁创建，如一次性查询几十万条记录、大 JSON 序列化。\n\n## 排查步骤\n1. 看监控：GC 次数、停顿、老年代使用率曲线、对象创建速率；\n2. 加参数：-XX:+PrintGCDetails -Xlog:gc*:file=gc.log，确认是 Full GC 还是并发模式失败；\n3. 抓内存：jmap -histo:live 看 top 对象类型，jmap -dump 或 MAT 分析支配树，定位泄漏点；\n4. 结合 jstack 看是否有线程持续持有大对象引用。\n\n## 优化建议\n- 大对象：分批查询、流式处理，避免一次性 load 全量；\n- 缓存：使用有上限的 Caffeine/Guava Cache，设置 expireAfterWrite；\n- ThreadLocal：务必在 finally 中 remove，避免线程池复用导致泄漏；\n- 参数：适当增大 Survivor、调整晋升阈值，或换 G1/ZGC 降低停顿。',
   2, NULL, 1, '["Java","JVM"]', 3, 0, 0, 0, 1, 0, 1),

  (1002, 'INTV-201-002',
   '谈谈 AQS 的设计思想，并说明 ReentrantLock 公平锁与非公平锁在实现和性能上的差异。',
   '请围绕 AbstractQueuedSynchronizer 回答：\n1. AQS 用哪两个核心成员描述同步状态与等待队列？state 的语义由谁定义？\n2. 独占模式与共享模式的区别是什么？\n3. ReentrantLock 公平锁与非公平锁在 tryAcquire 上的实现差异；\n4. 为什么非公平锁吞吐通常更高？它可能带来什么问题？\n5. 结合 CountDownLatch、Semaphore 说明 AQS 的复用方式。',
   '## 核心结构\nAQS 维护一个 volatile int state 和一个 CLH 双向队列（FIFO）。state 的语义完全由子类定义：ReentrantLock 中 state 表示重入次数，Semaphore 中表示可用许可数，CountDownLatch 中表示未完成的计数。队列节点 Node 用 waitStatus 表示唤醒状态，通过 LockSupport.park/unpark 阻塞与唤醒线程。\n\n## 独占 vs 共享\n- 独占（tryAcquire/tryRelease）：同一时刻只有一个线程能持有，如 ReentrantLock；\n- 共享（tryAcquireShared/tryReleaseShared）：多个线程可同时获取，如 Semaphore、CountDownLatch。\n\n## 公平 vs 非公平\n- 公平锁：tryAcquire 时先检查队列中是否有前驱节点，有则直接入队排队；\n- 非公平锁：新来的线程先 CAS 抢一次 state，抢不到才入队（插队）。\n\n## 性能差异\n非公平锁允许刚释放锁的线程立刻被新线程抢到，减少了线程上下文切换，因此吞吐通常更高。代价是可能造成队列中线程饥饿。\n\n## AQS 的复用\nCountDownLatch 在共享模式下，state 递减到 0 时释放所有等待线程；Semaphore 用 state 记录许可数，acquire 递减、release 递增，均通过 AQS 的模板方法实现，子类只需实现 tryXxx 系列。',
   2, NULL, 1, '["Java","并发"]', 3, 0, 0, 0, 1, 0, 1),

  (1003, 'INTV-202-001',
   '一条慢 SQL 从被记录到被优化，你会经历哪些步骤？请说明如何用执行计划定位问题。',
   '线上接口 P99 从 50ms 涨到 1.2s，怀疑是某条 SQL 导致。请说明：\n1. 你如何找到这条慢 SQL？（慢查询日志、performance_schema、APM 分别怎么看）\n2. 拿到 SQL 后，EXPLAIN 中哪些字段最关键？type、key、rows、Extra 该如何解读？\n3. 常见的索引失效场景有哪些？\n4. 如果加了索引仍然慢，还有哪些方向可以优化？\n5. 如何验证优化效果并避免回滚风险？',
   '## 定位慢 SQL\n- 慢查询日志：slow_query_log=ON、long_query_time=0.5，用 mysqldumpslow/pt-query-digest 聚合；\n- performance_schema：查 events_statements_summary_by_digest 按总耗时排序；\n- APM：把接口耗时拆到 SQL 维度，直接定位到调用点。\n\n## EXPLAIN 关键字段\n- type：ALL（全表）→ index（全索引扫描）→ range → ref → eq_ref → const，越靠右越好；\n- key：实际使用的索引，若为 NULL 说明没走索引；\n- rows：预估扫描行数，越大越差；\n- Extra：Using filesort / Using temporary 是危险信号，Using index 表示覆盖索引。\n\n## 索引失效场景\n- 对索引列做函数或运算：WHERE YEAR(create_time)=2024；\n- 隐式类型转换：字符串列传数字；\n- 前导模糊：LIKE ''%abc''；\n- 违反最左前缀：复合索引 (a,b) 跳过 a 直接查 b；\n- OR 连接非索引列、范围查询后续列失效。\n\n## 加索引仍慢的方向\n- 覆盖索引减少回表；\n- 深分页改游标（WHERE id > lastId LIMIT n）；\n- 拆分大事务、减少锁竞争；\n- 冷热数据分离或引入缓存。\n\n## 验证\n在预发用相同数据量对比执行计划与耗时；加索引用 ONLINE DDL（ALGORITHM=INPLACE）降低锁表风险；灰度观察。',
   12, NULL, 1, '["MySQL","数据库"]', 3, 0, 0, 0, 1, 0, 1),

  (1004, 'INTV-202-002',
   '什么是分库分表？它带来了哪些新问题，你如何解决跨分片的查询、排序和分布式事务？',
   '单表数据量达到 5000 万，查询明显变慢。你决定分库分表，请回答：\n1. 垂直拆分与水平拆分的区别和适用场景；\n2. 分片键如何选择？常见的分片算法有哪些？\n3. 分片后，跨分片的查询、分页、排序、聚合如何实现？\n4. 分布式事务有哪些方案？各自的一致性、性能、复杂度如何权衡？\n5. 如何做到不停机平滑迁移？',
   '## 垂直 vs 水平\n- 垂直拆分：按业务把不同表/字段拆到不同库，降低单库耦合；\n- 水平拆分：按分片键把同一张表的数据分散到多个库表，解决单表容量瓶颈。\n\n## 分片键与算法\n分片键要满足数据分布均匀、查询能命中单分片（如 user_id）。算法：range（范围，易热点）、hash（均匀，扩容需 rehash）、一致性哈希（扩容影响小）。\n\n## 跨分片问题\n- 查询：非分片键查询要广播到所有分片再内存合并；\n- 分页：各分片取前 N 条归并，深度分页代价高，可用"二阶段"或搜索引擎；\n- 排序/聚合：需在中间件层归并，建议引入 ES 做多维检索。\n\n## 分布式事务\n- 2PC/XA：强一致但性能差、易阻塞；\n- TCC：业务侵入大，需实现 Try/Confirm/Cancel；\n- 本地消息表/可靠消息最终一致：常见且务实；\n- Seata AT：自动补偿，适合多数场景。按一致性要求权衡。\n\n## 平滑迁移\n双写 + 数据同步（binlog/DataX）+ 灰度切读 + 校验一致性 + 回滚预案，先迁历史数据再切换读流量。',
   12, NULL, 1, '["MySQL","分布式"]', 3, 0, 0, 0, 1, 0, 1),

  (1005, 'INTV-203-001',
   '从用户态到内核态，简述一次阻塞式 read 系统调用的完整流程；再对比 select/poll/epoll 的差异。',
   '请回答：\n1. 一次阻塞式 read 从发起系统调用到数据返回，经历了哪些阶段？为什么会涉及两次数据拷贝？\n2. 阻塞 IO、非阻塞 IO、IO 多路复用、信号驱动、异步 IO 的区别；\n3. select、poll、epoll 在数据结构和时间复杂度上的差异；\n4. epoll 的 LT 与 ET 模式区别，使用 ET 需要注意什么？\n5. 为什么 epoll 在高并发下更高效？',
   '## 阻塞 read 流程\n1. 用户态调用 read，触发软中断/系统调用进入内核态；\n2. 内核检查数据是否就绪，未就绪则把当前线程挂起，等待数据到达（等待阶段）；\n3. 数据到达后从内核缓冲区拷贝到用户缓冲区（拷贝阶段），唤醒线程返回。\n两次拷贝指：网卡/磁盘 → 内核缓冲区，内核缓冲区 → 用户空间。\n\n## 五种 IO 模型\n阻塞 IO 在等待和拷贝阶段都阻塞；非阻塞 IO 等待阶段轮询、拷贝阶段阻塞；IO 多路复用用一个线程监听多个 fd；信号驱动由内核 SIGIO 通知；异步 IO 两个阶段都不阻塞。\n\n## select/poll/epoll\n- select：fd 集合是定长位图，默认 1024 上限，每次调用都要把集合拷进内核并线性扫描 O(n)；\n- poll：用链表去掉上限，仍是线性扫描 O(n)；\n- epoll：内核用红黑树管理 fd、就绪链表记录就绪事件，epoll_wait 只返回就绪的 fd，复杂度 O(1)。\n\n## LT vs ET\n- LT（水平触发）：只要缓冲区有数据就持续通知，编程简单，默认模式；\n- ET（边沿触发）：仅在状态变化时通知一次，必须配合非阻塞 fd 并循环读到 EAGAIN，否则会丢事件。\n\n## 为何高效\nepoll 把"注册"与"等待"分离，避免每次调用重复拷贝 fd 集合，且只返回就绪 fd，天然适合海量连接。',
   14, NULL, 1, '["操作系统","网络"]', 3, 0, 0, 0, 1, 0, 1),

  (1006, 'INTV-204-001',
   '完整描述浏览器从输入 URL 到页面可交互的渲染流程，并指出每一步常见的性能优化点。',
   '请按顺序说明：\n1. URL 解析、DNS、TCP/TLS、HTTP 请求到服务端响应的过程；\n2. 浏览器如何解析 HTML 构建 DOM、CSSOM，并生成渲染树；\n3. 布局（Layout）与绘制（Paint）、合成（Composite）的关系；\n4. 重排（reflow）与重绘（repaint）的区别，哪些操作会触发；\n5. 针对关键渲染路径，你有哪些优化手段？',
   '## 网络阶段\nURL 解析 → DNS 查询（可 dns-prefetch）→ TCP 三次握手 → TLS 握手（可 TLS1.3、会话复用）→ 发送 HTTP 请求 → 服务端响应。优化：CDN、HTTP/2 多路复用、强缓存/协商缓存、减少重定向。\n\n## 解析与渲染树\nHTML 解析成 DOM，CSS 解析成 CSSOM；二者合成渲染树（Render Tree），包含可见节点。CSS 会阻塞渲染，JS 默认阻塞 DOM 解析（可用 async/defer）。\n\n## 布局→绘制→合成\nLayout 计算几何位置 → Paint 生成绘制指令 → Composite 分层合成上屏。只改 transform/opacity 可走合成层，不触发重排重绘。\n\n## 重排 vs 重绘\n- 重排：几何属性变化（宽高、位置、display），代价高；\n- 重绘：外观变化（颜色、背景），代价较低。\n避免在循环里读写布局属性导致强制同步布局。\n\n## 关键渲染路径优化\n- 内联关键 CSS、异步非关键 CSS；\n- 图片懒加载、WebP、响应式尺寸；\n- 减少阻塞脚本、代码分割、骨架屏；\n- 使用 will-change / transform 做动画；\n- 首屏 SSR/预渲染，LCP/FID/CLS 为优化目标。',
   4, NULL, 1, '["前端","浏览器"]', 2, 0, 0, 0, 1, 0, 1),

  (1007, 'INTV-205-001',
   '请解释 RAG（检索增强生成）的完整链路，并说明如何评估与缓解向量检索带来的幻觉。',
   '请回答：\n1. RAG 相比纯微调的优势与适用场景；\n2. 完整链路：文档切分、向量化、入库、检索、重排、拼装提示词、生成；\n3. 切分策略（按长度/语义/结构）如何影响召回质量？\n4. 常见的失败模式：检索不到、检索到无关、检索到但模型无视，分别如何缓解？\n5. 如何客观评估 RAG 的效果？',
   '## 优势与场景\nRAG 把外部知识放在检索库里，无需重新训练即可更新知识、可溯源、成本低；适合知识频繁更新、需要引用出处的问答场景。微调更适合改变模型风格/格式或注入稳定领域能力。\n\n## 完整链路\n文档解析 → 切分（chunking）→ 向量化（embedding）→ 存入向量库（pgvector/Milvus）→ 查询向量化 → 相似度检索（top-k）→ 重排（rerank）→ 拼装上下文与提示词 → LLM 生成。\n\n## 切分策略\n按固定长度会切断语义；按标题/段落/句子等结构或语义切分能提升召回。chunk 过大引入噪声，过小丢上下文，常配合重叠（overlap）。\n\n## 失败模式与缓解\n- 检索不到：优化切分、加同义/多路召回（BM25+向量混合）、扩展查询；\n- 检索到无关：加重排模型、调 top-k 与相似度阈值；\n- 检索到但模型无视：在提示词中强调"仅依据给定资料回答"，要求引用；必要时把关键片段前置。\n\n## 评估\n构建带标准答案的评测集，指标包括检索层的 Recall@k、MRR 和生成层的忠实度（faithfulness，答案是否只依据资料）、答案相关性、引用正确率；可用 LLM-as-judge 结合人工抽检。',
   17, NULL, 1, '["人工智能","RAG"]', 3, 0, 0, 0, 1, 0, 1),

  (1008, 'INTV-206-001',
   '设计一个短链服务：请给出从发号、存储、跳转到高并发读的完整方案，并说明容量估算。',
   '请设计一个类似 t.cn 的短链系统，要求支撑 1 亿条短链、峰值 10 万 QPS 跳转：\n1. 短码如何生成？如何保证不重复且尽量短？\n2. 存储选型与表结构设计；\n3. 跳转如何做到低延迟、高并发？\n4. 如何处理热点短链和缓存穿透/击穿？\n5. 给出粗略的存储与带宽容量估算。',
   '## 短码生成\n- 自增 ID + 进制转换（如 62 进制），长度可控且无冲突，需预取号段避免依赖单点；\n- 哈希（MD5 取前 N 位）可能碰撞，需查重重试；\n- 推荐：号段模式发号（号段缓存）或 Snowflake 后转 62 进制，6 位可表达约 568 亿，够用。\n\n## 存储\nMySQL 存映射（short_code 唯一索引、original_url、创建时间、过期时间、状态），量级大按 short_code 分库分表；冷数据下沉。原始 URL 可压缩或单独存。\n\n## 跳转高并发\n读多写少，用缓存挡读：Redis 缓存 short_code → url，命中直接 302；未命中回源 DB 并回填（加互斥锁防击穿）。302 便于统计，301 更快但不可统计，按需选。\n\n## 热点与穿透\n- 热点：本地缓存（Caffeine）+ Redis 多级缓存，热点 key 复制到多节点；\n- 穿透：布隆过滤器拦不存在的 code；\n- 击穿：单飞/互斥重建。\n\n## 容量估算\n1 亿条 ×（约 100 字节/行）≈ 10GB，MySQL 轻松容纳。10 万 QPS 跳转几乎全部命中 Redis，Redis 单节点可达 10 万级 QPS，可用集群分片；带宽 ≈ 10 万 × 0.5KB ≈ 50MB/s，可控。发号写入按每天新增百万级，压力很小。',
   1, NULL, 1, '["系统设计","热门"]', 3, 0, 0, 0, 1, 0, 1);
