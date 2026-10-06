import { ReusablePostEditor } from '../../../components/ReusablePostEditor'

export function CheckInEditorPage() {
  return (
    <ReusablePostEditor
      mode="checkin"
      initialTitle=""
      initialMarkdown={`# Day 1 打卡\n\n## 今日完成\n\n- \n\n## 今日收获\n\n- \n\n## 明日计划\n\n- `}
      titlePlaceholder="请输入今日打卡标题"
      publishSuccessText="打卡发布成功"
      updateSuccessText="打卡更新成功"
      publishErrorText="打卡发布失败，请稍后重试"
      updateErrorText="打卡更新失败，请稍后重试"
      saveDraftText="保存打卡草稿"
      updateDraftText="更新打卡草稿"
    />
  )
}
