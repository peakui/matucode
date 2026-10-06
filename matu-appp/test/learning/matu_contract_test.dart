import 'package:flutter_test/flutter_test.dart';
import 'package:matu_appp/feature/learning/models/learning_preview_text.dart';
import 'package:matu_appp/core/network/matu_response_codec.dart';
import 'package:matu_appp/core/model/matu/learning_models.dart';

/// 响应、精度、答案权限和观看进度契约回归。
void main() {
  test('文章摘要隐藏媒体语法并保留编程语言与比较运算符', () {
    expect(
      LearningPreviewText.clean(
        '![image](https://example.test/a.png) C# 的 **泛型** 与 x > y',
      ),
      'C# 的 泛型 与 x > y',
    );
    expect(
      LearningPreviewText.clean(
        '! b52ea68eb057b6843820591bdb533f794986 https://example.test/a.png',
      ),
      '',
    );
    expect(
      LearningPreviewText.clean(
        '## Dart\n[文档](https://example.test) 与 `async`',
      ),
      'Dart 文档 与 async',
    );
  });
  test('长整型 ID 在解包前保留精度且不改写正文数字', () {
    final data =
        MatuResponseCodec.unwrap(
              200,
              '{"code":0,"data":{"id":1878038194130255899,"text":"1878038194130255899","ratio":1.25}}',
            )
            as Map;
    expect(data['id'], '1878038194130255899');
    expect(data['text'], '1878038194130255899');
    expect(data['ratio'], 1.25);
  });
  test('HTTP 和业务错误不可误判为成功', () {
    expect(
      () => MatuResponseCodec.unwrap(200, '{"code":401,"message":"登录已失效"}'),
      throwsA(isA<MatuApiException>().having((e) => e.code, 'code', 401)),
    );
    expect(
      () => MatuResponseCodec.unwrap(401, ''),
      throwsA(isA<MatuApiException>().having((e) => e.code, 'code', 401)),
    );
    expect(
      () => MatuResponseCodec.unwrap(500, '{"code":0,"data":{}}'),
      throwsA(isA<MatuApiException>()),
    );
    expect(
      () => MatuResponseCodec.unwrap(200, '<html>'),
      throwsA(isA<MatuApiException>()),
    );
  });
  test('未授权答案不进入展示模型与收藏', () {
    final q = LearningContent.fromJson({
      'id': '1',
      'answer': 'secret',
      'answerVisible': false,
      'isLocked': 1,
    }, LearningKind.interview);
    expect(q.answer, '');
    expect(q.answerVisible, false);
    expect(q.toBookmark().containsKey('answer'), false);
    expect(
      LearningContent.fromJson({
        'id': '1',
        'answer': 'secret',
      }, LearningKind.interview).answerVisible,
      false,
    );
  });
  test('视频只在真实完成时保存 100%，接近结束重新从头播放', () {
    expect(
      LessonProgress.snapshot(
        '1',
        const Duration(milliseconds: 9999),
        const Duration(seconds: 10),
      )!['progressPercent'],
      99,
    );
    expect(
      LessonProgress.snapshot(
        '1',
        const Duration(seconds: 10),
        const Duration(seconds: 10),
      )!['progressPercent'],
      99,
    );
    expect(
      LessonProgress.snapshot(
        '1',
        const Duration(seconds: 10),
        const Duration(seconds: 10),
        completed: true,
      )!['progressPercent'],
      100,
    );
    expect(LessonProgress.snapshot('1', Duration.zero, Duration.zero), null);
    expect(LessonProgress.resume(99, 100), 0);
    expect(LessonProgress.resume(30, 100), 30);
  });
  test('课程模型使用服务端章节与课时字段，不持有详情直链', () {
    final c = CourseChapter.fromJson({
      'chapterTitle': '第一章',
      'videos': [
        {
          'id': '123',
          'videoTitle': '第一节',
          'duration': 90,
          'videoUrl': 'private',
        },
      ],
    });
    expect(c.title, '第一章');
    expect(c.videos.single.title, '第一节');
  });
}
