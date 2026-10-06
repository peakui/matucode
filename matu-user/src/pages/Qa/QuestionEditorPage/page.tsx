import { ReusablePostEditor } from '../../../components/ReusablePostEditor'

export function QuestionEditorPage() {
  return (
    <ReusablePostEditor
      mode="question"
      initialTitle=""
      initialMarkdown={`# 问题背景\n\n我在开发过程中遇到了下面这个问题：\n\n## 已尝试方案\n\n- \n\n## 预期结果\n\n- \n\n## 当前报错 / 现象\n\n- \n\n## 希望得到的帮助\n\n- `}
      titlePlaceholder="请输入问题标题"
      publishSuccessText="提问发布成功"
      updateSuccessText="问题更新成功"
      publishErrorText="提问发布失败，请稍后重试"
      updateErrorText="问题更新失败，请稍后重试"
      saveDraftText="保存问题草稿"
      updateDraftText="更新问题草稿"
    />
  )
}
