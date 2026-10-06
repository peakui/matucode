/// 列表摘要清理：保留文字含义，隐藏图片语法、裸链接与附件哈希。
abstract final class LearningPreviewText {
  /// 清理 [source] 的展示噪声，不改变服务端保存的正文。
  static String clean(String source) => source
      .replaceAll(RegExp(r'!\[[^\]]*\]\([^)]*\)'), '')
      .replaceAllMapped(RegExp(r'\[([^\]]+)\]\([^)]*\)'), (match) => match[1]!)
      .replaceAll(RegExp(r'<[^>]+>'), ' ')
      .replaceAll(RegExp(r'https?://\S+'), '')
      .replaceAll(RegExp(r'!?\s*\b[0-9a-fA-F]{24,}\b'), '')
      .replaceAll(
        RegExp(r'^\s{0,3}(?:#{1,6}\s+|>\s+|[-*+]\s+)', multiLine: true),
        '',
      )
      .replaceAllMapped(RegExp(r'(\*\*|__)(.*?)\1'), (match) => match[2]!)
      .replaceAllMapped(RegExp(r'`([^`]+)`'), (match) => match[1]!)
      .replaceAll('&nbsp;', ' ')
      .replaceAll('&amp;', '&')
      .replaceAll(RegExp(r'\s+'), ' ')
      .trim();
}
