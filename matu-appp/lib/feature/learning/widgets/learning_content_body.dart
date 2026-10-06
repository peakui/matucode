import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_html/flutter_html.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:get/get.dart';
import '../../../core/env/env.dart';
import '../../../core/network/media_url_resolver.dart';
import '../../../core/design_system/theme/app_metrics.dart';
import '../../../core/design_system/theme/app_tokens.dart';
import '../../../core/design_system/ui/ui.dart';
import '../../../core/ui/preview/app_preview_annotations.dart';
import '../localization/learning_strings.dart';

/// 富文本、图片预览与主动外链复制，不执行正文脚本。
class LearningContentBody extends StatelessWidget {
  const LearningContentBody(this.content, {super.key});
  final String content;
  static String assetUrl(String value) {
    return MediaUrlResolver.resolve(value, baseUrl: Env.baseUrl)?.toString() ??
        '';
  }

  static void previewImage(String url) {
    final safe = assetUrl(url);
    if (safe.isEmpty) return;
    Get.dialog<void>(
      Scaffold(
        appBar: AppBar(),
        body: Center(
          child: InteractiveViewer(
            child: Image.network(
              safe,
              errorBuilder: (_, _, _) =>
                  const Icon(Icons.broken_image_outlined),
            ),
          ),
        ),
      ),
    );
  }

  static Future<void> link(String? value) async {
    if (value == null || assetUrl(value).isEmpty) return;
    await Get.dialog<void>(
      AlertDialog(
        content: SelectableText(assetUrl(value)),
        actions: [
          TextButton(
            onPressed: () => Get.back<void>(),
            child: const Text(LearningStrings.cancel),
          ),
          TextButton(
            onPressed: () async {
              await Clipboard.setData(ClipboardData(text: assetUrl(value)));
              Get.back<void>();
            },
            child: const Text(LearningStrings.copyLink),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    final bodyStyle = TextStyle(
      fontSize: AppTypography.body,
      height: AppTypography.readingHeight,
      color: tokens.labelPrimary,
    );
    if (RegExp(
      r'<(?:p|div|h[1-6]|ul|ol|table|img)\b',
      caseSensitive: false,
    ).hasMatch(content)) {
      return Html(
        data: content,
        style: {
          'body': Style(
            margin: Margins.zero,
            padding: HtmlPaddings.zero,
            fontSize: FontSize(AppTypography.body),
            lineHeight: const LineHeight(AppTypography.readingHeight),
            color: tokens.labelPrimary,
          ),
          'h1': Style(
            fontSize: FontSize(20),
            fontWeight: FontWeight.w600,
            lineHeight: const LineHeight(1.45),
          ),
          'h2': Style(
            fontSize: FontSize(18),
            fontWeight: FontWeight.w600,
            lineHeight: const LineHeight(1.45),
          ),
          'h3': Style(
            fontSize: FontSize(AppTypography.sectionTitle),
            fontWeight: FontWeight.w600,
          ),
          'p': Style(margin: Margins.only(bottom: AppSpacing.lg)),
          'a': Style(color: tokens.accent),
          'code': Style(
            fontSize: FontSize(AppTypography.caption),
            fontFamily: 'monospace',
            backgroundColor: tokens.fillSecondary,
          ),
          'pre': Style(
            fontSize: FontSize(AppTypography.caption),
            padding: HtmlPaddings.all(AppSpacing.md),
            backgroundColor: tokens.fillSecondary,
            whiteSpace: WhiteSpace.pre,
          ),
          'blockquote': Style(
            border: Border(
              left: BorderSide(color: tokens.accentSoft, width: AppSpacing.xs),
            ),
            padding: HtmlPaddings.only(left: AppSpacing.md),
            color: tokens.labelSecondary,
          ),
        },
        extensions: [
          OnImageTapExtension(
            onImageTap: (url, _, _) {
              if (url != null) previewImage(url);
            },
          ),
        ],
        doNotRenderTheseTags: const {
          'script',
          'iframe',
          'object',
          'embed',
          'video',
          'audio',
          'form',
        },
        onLinkTap: (url, _, _) => link(url),
      );
    }
    return MarkdownBody(
      data: content,
      selectable: true,
      softLineBreak: true,
      styleSheet: MarkdownStyleSheet(
        p: bodyStyle,
        h1: bodyStyle.copyWith(
          fontSize: 20,
          height: 1.45,
          fontWeight: FontWeight.w600,
        ),
        h2: bodyStyle.copyWith(
          fontSize: 18,
          height: 1.45,
          fontWeight: FontWeight.w600,
        ),
        h3: bodyStyle.copyWith(
          fontSize: AppTypography.sectionTitle,
          height: 1.5,
          fontWeight: FontWeight.w600,
        ),
        h1Padding: const EdgeInsets.only(
          top: AppSpacing.md,
          bottom: AppSpacing.sm,
        ),
        h2Padding: const EdgeInsets.only(
          top: AppSpacing.md,
          bottom: AppSpacing.sm,
        ),
        h3Padding: const EdgeInsets.only(
          top: AppSpacing.sm,
          bottom: AppSpacing.xs,
        ),
        a: bodyStyle.copyWith(color: tokens.accent),
        code: TextStyle(
          fontFamily: 'monospace',
          fontSize: AppTypography.caption,
          height: 1.65,
          color: tokens.labelPrimary,
          backgroundColor: tokens.fillSecondary,
        ),
        codeblockPadding: const EdgeInsets.all(AppSpacing.md),
        codeblockDecoration: BoxDecoration(
          color: tokens.fillSecondary,
          borderRadius: BorderRadius.circular(AppRadius.input),
          border: Border.all(color: tokens.hairline),
        ),
        blockquote: bodyStyle.copyWith(color: tokens.labelSecondary),
        blockquotePadding: const EdgeInsets.all(AppSpacing.md),
        blockquoteDecoration: BoxDecoration(
          color: tokens.accentSoft.withValues(alpha: 0.5),
          border: Border(
            left: BorderSide(
              color: tokens.accent.withValues(alpha: 0.45),
              width: AppSpacing.xs,
            ),
          ),
          borderRadius: BorderRadius.circular(AppRadius.input),
        ),
        blockSpacing: AppSpacing.lg,
        listBullet: bodyStyle.copyWith(color: tokens.labelSecondary),
        tableBody: bodyStyle.copyWith(fontSize: AppTypography.summary),
        tableHead: bodyStyle.copyWith(
          fontSize: AppTypography.summary,
          fontWeight: FontWeight.w600,
        ),
      ),
      onTapLink: (_, url, _) => link(url),
      sizedImageBuilder: (config) => GestureDetector(
        onTap: () => previewImage(config.uri.toString()),
        child: Image.network(
          assetUrl(config.uri.toString()),
          errorBuilder: (_, _, _) => const Icon(Icons.broken_image_outlined),
        ),
      ),
    );
  }
}

/// 学习模块统一错误反馈，复用设计系统错误态。
class LearningErrorNotice extends StatelessWidget {
  /// 创建错误提示
  const LearningErrorNotice(this.message, {super.key, this.retry});

  /// 错误文案，为空时完全不占位
  final String message;

  /// 重试回调
  final VoidCallback? retry;

  @override
  Widget build(BuildContext context) => AppErrorState(
    message: message,
    retryLabel: LearningStrings.retry,
    onRetry: retry,
  );
}

@WidgetPreview()
Widget previewLearningBody() =>
    const LearningContentBody('## 学习笔记\n每一次练习，都是进步。');
