/// 码途内容模型，按后端 VO 显式映射，不向页面暴露 JSON。
enum LearningKind { article, check, qa, course, interview }

/// 业务接口路径映射。
extension LearningKindPath on LearningKind {
  String get path => switch (this) {
    LearningKind.article => '/posts',
    LearningKind.check => '/checks',
    LearningKind.qa => '/qa/questions',
    LearningKind.course => '/courses',
    LearningKind.interview => '/interview/questions',
  };
}

/// JSON 字段转换统一入口。
String stringValue(dynamic value) => value?.toString() ?? '';

/// 普通计数转换，不用于 ID。
int countValue(dynamic value) => num.tryParse('$value')?.toInt() ?? 0;

/// 内容列表与详情的共享展示契约。
class LearningContent {
  /// 创建不可变内容。
  const LearningContent({
    required this.id,
    required this.title,
    this.summary = '',
    this.content = '',
    this.author = '',
    this.cover = '',
    this.category = '',
    this.date = '',
    this.answer = '',
    this.answerVisible = false,
    this.liked = false,
    this.collected = false,
    this.followed = false,
    this.likes = 0,
    this.difficulty = 0,
    this.images = const [],
    this.chapters = const [],
  });
  final String id,
      title,
      summary,
      content,
      author,
      cover,
      category,
      date,
      answer;
  final bool answerVisible, liked, collected, followed;
  final int likes, difficulty;
  final List<String> images;
  final List<CourseChapter> chapters;

  /// 从各域 VO 转为明确的展示字段。
  factory LearningContent.fromJson(Map<String, dynamic> j, LearningKind kind) =>
      LearningContent(
        id: stringValue(j['id']),
        title: stringValue(j['title']),
        summary: stringValue(
          kind == LearningKind.course ? j['subtitle'] : j['summary'],
        ),
        content: stringValue(
          kind == LearningKind.course ? j['description'] : j['content'],
        ),
        author: stringValue(switch (kind) {
          LearningKind.article => j['authorName'],
          LearningKind.course => j['instructorNickname'] ?? j['instructorName'],
          _ => j['username'],
        }),
        cover: stringValue(j['coverUrl']),
        category: stringValue(j['categoryName']),
        date: stringValue(j['createdAt'] ?? j['checkDate'] ?? j['publishedAt']),
        answer: j['answerVisible'] == true ? stringValue(j['answer']) : '',
        answerVisible: j['answerVisible'] == true,
        liked: j['liked'] == true,
        collected: j['collected'] == true,
        followed: j['followed'] == true,
        likes: countValue(j['likeCount']),
        difficulty: countValue(j['difficulty'] ?? j['level']),
        images: (j['imageUrls'] as List? ?? const []).map(stringValue).toList(),
        chapters: (j['chapters'] as List? ?? const [])
            .map(
              (e) =>
                  CourseChapter.fromJson(Map<String, dynamic>.from(e as Map)),
            )
            .toList(),
      );

  /// 收藏仅存元信息，不持久化答案或正文。
  Map<String, dynamic> toBookmark() => {
    'id': id,
    'title': title,
    'categoryName': category,
    'difficulty': difficulty,
  };
}

/// 分类数据。
class LearningCategory {
  const LearningCategory(this.id, this.name);
  final String id, name;
  factory LearningCategory.fromJson(Map<String, dynamic> j) => LearningCategory(
    stringValue(j['id']),
    stringValue(j['categoryName'] ?? j['name']),
  );
}

/// 课程章节。
class CourseChapter {
  const CourseChapter(this.title, this.videos);
  final String title;
  final List<CourseLesson> videos;
  factory CourseChapter.fromJson(Map<String, dynamic> j) => CourseChapter(
    stringValue(j['chapterTitle']),
    (j['videos'] as List? ?? const [])
        .map((e) => CourseLesson.fromJson(Map<String, dynamic>.from(e as Map)))
        .toList(),
  );
}

/// 课时不保留未经授权的播放 URL。
class CourseLesson {
  const CourseLesson(this.id, this.title, this.duration);
  final String id, title;
  final int duration;
  factory CourseLesson.fromJson(Map<String, dynamic> j) => CourseLesson(
    stringValue(j['id']),
    stringValue(j['videoTitle']),
    countValue(j['duration']),
  );
}

/// 评论与问答回答。
class LearningDiscussion {
  const LearningDiscussion(this.id, this.author, this.content);
  final String id, author, content;
  factory LearningDiscussion.fromJson(Map<String, dynamic> j) =>
      LearningDiscussion(
        stringValue(j['id']),
        stringValue(j['userName'] ?? j['username']),
        stringValue(j['content']),
      );
}

/// 登录用户及个人资料。
class MatuProfile {
  const MatuProfile({
    required this.id,
    this.nickname = '',
    this.avatar = '',
    this.signature = '',
  });
  final String id, nickname, avatar, signature;
  factory MatuProfile.fromJson(Map<String, dynamic> j) => MatuProfile(
    id: stringValue(j['userId'] ?? j['id']),
    nickname: stringValue(j['nickname'] ?? j['username']),
    avatar: stringValue(j['avatarUrl']),
    signature: stringValue(j['signature']),
  );
  Map<String, dynamic> toJson() => {
    'userId': id,
    'nickname': nickname,
    'avatarUrl': avatar,
    'signature': signature,
  };
}

/// 视频观看进度规则。
abstract final class LessonProgress {
  static int resume(int watched, int duration) =>
      duration > 0 && watched >= duration - 2
      ? 0
      : watched.clamp(0, duration > 0 ? duration : watched);
  static Map<String, dynamic>? snapshot(
    String videoId,
    Duration position,
    Duration duration, {
    bool completed = false,
  }) {
    if (videoId.isEmpty ||
        duration.inMilliseconds <= 0 ||
        position.inMilliseconds <= 0) {
      return null;
    }
    final percent = (position.inMilliseconds / duration.inMilliseconds * 100)
        .floor();
    return {
      'videoId': videoId,
      'watchedDuration': position.inSeconds.clamp(0, duration.inSeconds),
      'progressPercent': completed ? 100 : percent.clamp(0, 99),
    };
  }
}
