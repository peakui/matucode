# service-course 接口文档

本文档基于 `service-course` 当前控制器、DTO 和 VO 生成，适用于前后端联调与接口对接。

## 1. 基础信息

- 服务名：`service-course`
- 基础路径：`/courses`
- 返回格式：统一使用 `ApiResponse<T>`
- 分页格式：`PageResponse<T>`
- 请求体：`application/json`
- 认证方式：基于登录态与角色权限控制（Sa-Token）

## 2. 通用说明

### 2.1 分页参数

- `pageNum`：页码，默认 `1`
- `pageSize`：每页条数，默认 `10`

### 2.2 课程状态

- `0`：草稿
- `1`：已发布
- `2`：已下架

### 2.3 课程等级

当前接口仅以 `Integer level` 传递，具体枚举由业务层定义。

### 2.4 常见错误场景

- 课程不存在
- 章节不存在
- 视频不存在
- 权限不足
- 非试看课程需要 VIP 才能访问

---

## 3. 接口列表

### 3.1 课程列表

- **接口**：`GET /courses`
- **说明**：分页查询课程列表

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| status | Integer | 否 | - | 课程状态；不传返回全部，`0` 草稿，`1` 已发布，`2` 下架/私密，`3` 删除 |
| categoryId | Long | 否 | - | 分类 ID |
| level | Integer | 否 | - | 课程等级 |
| freeOnly | Boolean | 否 | - | 是否仅免费课程 |
| keyword | String | 否 | - | 搜索关键词，匹配标题/副标题 |
| pageNum | Long | 否 | 1 | 页码 |
| pageSize | Long | 否 | 10 | 每页条数 |
| sortBy | String | 否 | latest | 排序方式：`latest` / `popular` / `rating` |

#### 响应数据 `CourseListItemVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 课程 ID |
| instructorId | Long | 讲师 ID |
| categoryId | Long | 分类 ID |
| title | String | 标题 |
| subtitle | String | 副标题 |
| coverUrl | String | 封面地址 |
| price | BigDecimal | 价格 |
| level | Integer | 课程等级 |
| studentCount | Integer | 学员数 |
| chapterCount | Integer | 章节数 |
| videoCount | Integer | 视频数 |
| totalDuration | Integer | 总时长（秒） |
| rating | BigDecimal | 评分 |
| isFree | Integer | 是否免费，`1` 是，`0` 否 |
| publishedAt | LocalDateTime | 发布时间 |

---

### 3.2 课程详情

- **接口**：`GET /courses/{courseId}`
- **说明**：获取课程详情

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| courseId | Long | 是 | 课程 ID |

#### 响应数据 `CourseDetailVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 课程 ID |
| instructorId | Long | 讲师 ID |
| categoryId | Long | 分类 ID |
| title | String | 标题 |
| subtitle | String | 副标题 |
| description | String | 课程描述 |
| coverUrl | String | 封面地址 |
| price | BigDecimal | 价格 |
| originalPrice | BigDecimal | 原价 |
| level | Integer | 课程等级 |
| language | String | 语言 |
| studentCount | Integer | 学员数 |
| chapterCount | Integer | 章节数 |
| videoCount | Integer | 视频数 |
| totalDuration | Integer | 总时长（秒） |
| rating | BigDecimal | 评分 |
| ratingCount | Integer | 评分次数 |
| isFree | Integer | 是否免费 |
| publishedAt | LocalDateTime | 发布时间 |
| vipRequired | Boolean | 是否需要 VIP 才可观看 |
| canWatch | Boolean | 当前用户是否可观看 |
| chapters | List<CourseChapterVO> | 章节列表 |
| articles | List<CourseArticleVO> | 文章列表 |

---

### 3.3 视频播放信息

- **接口**：`GET /courses/videos/{videoId}/play`
- **说明**：获取视频播放信息；若不是试看内容且无权限，`videoUrl` 为空

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| videoId | Long | 是 | 视频 ID |

#### 响应数据 `VideoPlayVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| courseId | Long | 课程 ID |
| chapterId | Long | 章节 ID |
| videoId | Long | 视频 ID |
| title | String | 视频标题 |
| videoUrl | String | 视频地址 |
| freePreview | Boolean | 是否试看 |
| vipRequired | Boolean | 是否需要 VIP |
| playable | Boolean | 是否可播放 |
| message | String | 提示信息 |

---

### 3.4 创建课程

- **接口**：`POST /courses`
- **说明**：创建课程，支持管理员/讲师/认证用户

#### 请求体 `CreateCourseRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| categoryId | Long | 否 | 分类 ID |
| title | String | 是 | 课程标题 |
| subtitle | String | 否 | 副标题 |
| description | String | 否 | 描述 |
| coverUrl | String | 否 | 封面地址 |
| price | BigDecimal | 否 | 价格 |
| originalPrice | BigDecimal | 否 | 原价 |
| level | Integer | 否 | 课程等级 |
| language | String | 否 | 语言，默认 `zh-CN` |
| isFree | Integer | 否 | 是否免费，`1` 是，`0` 否 |

#### 响应数据

- `CourseDetailVO`

---

### 3.5 更新课程

- **接口**：`PUT /courses/{courseId}`
- **说明**：更新课程信息

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| courseId | Long | 是 | 课程 ID |

#### 请求体 `UpdateCourseRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| categoryId | Long | 否 | 分类 ID |
| title | String | 否 | 课程标题 |
| subtitle | String | 否 | 副标题 |
| description | String | 否 | 描述 |
| coverUrl | String | 否 | 封面地址 |
| price | BigDecimal | 否 | 价格 |
| originalPrice | BigDecimal | 否 | 原价 |
| level | Integer | 否 | 课程等级 |
| language | String | 否 | 语言 |
| isFree | Integer | 否 | 是否免费 |
| status | Integer | 否 | 状态，仅管理员可修改 |

#### 响应数据

- `CourseDetailVO`

---

### 3.6 发布课程

- **接口**：`POST /courses/{courseId}/publish`
- **说明**：将课程状态改为已发布

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| courseId | Long | 是 | 课程 ID |

#### 响应

- `ApiResponse<Void>`

---

### 3.7 下架课程

- **接口**：`POST /courses/{courseId}/offline`
- **说明**：仅管理员可用，将课程状态改为已下架

---

### 3.8 创建章节

- **接口**：`POST /courses/chapters`
- **说明**：为课程创建章节

#### 请求体 `CreateChapterRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| courseId | Long | 是 | 课程 ID |
| chapterTitle | String | 是 | 章节标题 |
| chapterDesc | String | 否 | 章节描述 |
| sortOrder | Integer | 否 | 排序值 |
| isFreePreview | Integer | 否 | 是否试看，`1` 是，`0` 否 |

#### 响应数据 `CourseChapterVO`

---

### 3.9 初始化视频断点续传

- **接口**：`POST /courses/videos/upload/init`
- **说明**：初始化视频分片上传

#### 请求体 `InitCourseVideoUploadRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| chapterId | Long | 是 | 章节 ID |
| originalName | String | 是 | 原始文件名 |
| fileType | String | 否 | 文件类型 |
| fileSize | Long | 是 | 文件大小 |
| fileMd5 | String | 否 | 文件 MD5 |
| chunkSize | Integer | 是 | 分片大小 |
| chunkCount | Integer | 是 | 分片数量 |

视频文件默认上传到配置的默认 Bucket 的 `video/` 目录。

#### 响应数据 `CourseVideoUploadInitVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| fileId | Long | 文件 ID |
| uploadId | String | 上传会话 ID |
| chunkCount | Integer | 分片总数 |
| chunkSize | Integer | 分片大小 |
| uploadedChunks | List<Integer> | 已上传分片 |
| chunkUploadUrl | String | 分片上传地址 |
| chunkStatusUrl | String | 分片状态查询地址 |
| completeUrl | String | 完成上传地址 |

---

### 3.10 完成视频上传并创建视频

- **接口**：`POST /courses/videos/upload/complete`
- **说明**：完成分片上传后创建课程视频

#### 请求体 `CompleteCourseVideoUploadRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| chapterId | Long | 是 | 章节 ID |
| fileId | Long | 是 | 文件 ID |
| videoTitle | String | 是 | 视频标题 |
| videoDesc | String | 否 | 视频描述 |
| coverUrl | String | 否 | 视频封面 |
| resolution | String | 否 | 分辨率 |
| sortOrder | Integer | 否 | 排序值 |
| isFreePreview | Integer | 否 | 是否试看 |

完成上传后，后端会根据 OSS 签名地址自动解析视频时长并写入课程视频，无需前端传 `duration`。

#### 响应数据 `CourseVideoVO`

---

### 3.11 上传/登记课程视频

- **接口**：`POST /courses/videos`
- **说明**：直接创建视频记录

#### 请求体 `CreateVideoRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| chapterId | Long | 是 | 章节 ID |
| videoTitle | String | 是 | 视频标题 |
| videoDesc | String | 否 | 视频描述 |
| videoUrl | String | 是 | 视频地址 |
| coverUrl | String | 否 | 封面地址 |
| duration | Integer | 否 | 时长（秒） |
| fileSize | Long | 否 | 文件大小 |
| resolution | String | 否 | 分辨率 |
| sortOrder | Integer | 否 | 排序值 |
| isFreePreview | Integer | 否 | 是否试看 |

#### 响应数据 `CourseVideoVO`

---

### 3.12 创建文字教程

- **接口**：`POST /courses/articles`
- **说明**：创建课程文章/图文教程

#### 请求体 `CreateArticleRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| courseId | Long | 是 | 课程 ID |
| chapterId | Long | 否 | 章节 ID |
| title | String | 是 | 文章标题 |
| content | String | 是 | 文章内容 |
| sortOrder | Integer | 否 | 排序值 |
| status | Integer | 否 | 状态 |

#### 响应数据 `CourseArticleVO`

---

### 3.13 更新学习进度

- **接口**：`POST /courses/{courseId}/progress`
- **说明**：更新用户对指定课程的视频学习进度

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| courseId | Long | 是 | 课程 ID |

#### 请求体 `UpdateProgressRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| videoId | Long | 是 | 视频 ID |
| progressPercent | BigDecimal | 是 | 学习进度，0~100 |
| watchedDuration | Integer | 否 | 已观看时长（秒） |

#### 响应数据 `CourseProgressVO`

---

### 3.14 我的课程进度

- **接口**：`GET /courses/{courseId}/progress/mine`
- **说明**：查询当前登录用户在指定课程下的进度列表

---

### 3.15 创建学习笔记

- **接口**：`POST /courses/notes`
- **说明**：为课程或视频创建学习笔记

#### 请求体 `CreateNoteRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| courseId | Long | 是 | 课程 ID |
| videoId | Long | 否 | 视频 ID |
| content | String | 是 | 笔记内容 |
| timestamp | Integer | 否 | 视频时间点（秒） |
| isPublic | Integer | 否 | 是否公开 |

#### 响应数据 `CourseNoteVO`

---

### 3.16 学习笔记列表

- **接口**：`GET /courses/notes`
- **说明**：查询课程笔记列表

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| courseId | Long | 是 | - | 课程 ID |
| videoId | Long | 否 | - | 视频 ID |
| onlyPublic | Boolean | 否 | true | 是否仅查看公开笔记 |
| pageNum | Long | 否 | 1 | 页码 |
| pageSize | Long | 否 | 10 | 每页条数 |

#### 响应数据

- `PageResponse<CourseNoteVO>`

---

### 3.17 发表评价

- **接口**：`POST /courses/reviews`
- **说明**：对课程发表评价

#### 请求体 `CreateReviewRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| courseId | Long | 是 | 课程 ID |
| rating | Integer | 是 | 评分，1~5 |
| content | String | 否 | 评价内容 |

#### 响应数据 `CourseReviewVO`

---

### 3.18 课程评价列表

- **接口**：`GET /courses/{courseId}/reviews`
- **说明**：查询课程评价分页列表

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| pageNum | Long | 否 | 1 | 页码 |
| pageSize | Long | 否 | 10 | 每页条数 |

#### 响应数据

- `PageResponse<CourseReviewVO>`

---

### 3.19 发放课程证书

- **接口**：`POST /courses/{courseId}/certificate`
- **说明**：为当前登录用户发放课程证书

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| courseId | Long | 是 | 课程 ID |

#### 响应数据 `CourseCertificateVO`

---

## 4. 主要响应体说明

### 4.1 CourseChapterVO

- `id`
- `courseId`
- `chapterTitle`
- `chapterDesc`
- `sortOrder`
- `videoCount`
- `duration`
- `isFreePreview`
- `videos`

### 4.2 CourseVideoVO

- `id`
- `chapterId`
- `videoTitle`
- `videoDesc`
- `videoUrl`
- `coverUrl`
- `duration`
- `fileSize`
- `resolution`
- `sortOrder`
- `playCount`
- `isFreePreview`

### 4.3 CourseArticleVO

- `id`
- `courseId`
- `chapterId`
- `title`
- `content`
- `wordCount`
- `readTime`
- `viewCount`
- `sortOrder`

### 4.4 CourseProgressVO

- `id`
- `courseId`
- `videoId`
- `progressPercent`
- `watchedDuration`
- `lastWatchTime`
- `isCompleted`
- `completedAt`

### 4.5 CourseNoteVO

- `id`
- `userId`
- `courseId`
- `videoId`
- `content`
- `timestamp`
- `isPublic`
- `likeCount`
- `createdAt`

### 4.6 CourseReviewVO

- `id`
- `userId`
- `courseId`
- `rating`
- `content`
- `likeCount`
- `isVerifiedPurchase`
- `createdAt`

### 4.7 CourseCertificateVO

- `id`
- `userId`
- `courseId`
- `certificateNo`
- `certificateUrl`
- `issueDate`
- `expireDate`

---

## 5. 权限说明

- 创建课程：需要管理员、讲师或认证用户角色
- 更新课程：课程拥有者或管理员
- 发布课程：课程拥有者或管理员
- 下架课程：仅管理员
- 创建章节/视频/文章：课程拥有者或具备上传权限角色
- 学习进度、笔记、评价、证书：基于当前登录用户

---

## 6. 联调建议

建议前端优先对接以下接口：

1. `GET /courses` 课程列表
2. `GET /courses/{courseId}` 课程详情
3. `GET /courses/videos/{videoId}/play` 播放信息
4. `POST /courses/{courseId}/progress` 学习进度
5. `POST /courses/notes` 学习笔记
6. `POST /courses/reviews` 课程评价

---

如需，我还可以继续为 `service-course` 补一份 **OpenAPI/Swagger 注解版接口说明**，或者把这份文档拆成更适合后端维护的 `接口总览 + DTO 字段表 + 错误码表` 三部分。