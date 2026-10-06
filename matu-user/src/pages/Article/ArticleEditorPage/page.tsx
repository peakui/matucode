import { ReusablePostEditor } from '../../../components/ReusablePostEditor'

const initialMarkdown = `# 今日打卡：记录一下今天的学习进度

> 今天完成了什么？遇到了什么问题？明天准备继续做什么？

## 今日完成

- 完成 3 道算法题
- 复盘 1 个知识点
- 阅读 1 篇技术文章

## 遇到的问题

- 这里写今天卡住的点
- 这里写还没完全理解的内容

## 明日计划

- 继续推进当前学习任务
- 补齐今天没完成的部分
`

export function ArticleEditorPage() {
  return (
    <ReusablePostEditor
      mode="article"
      initialTitle=""
      initialMarkdown={initialMarkdown}
      titlePlaceholder="请输入文章标题"
      publishSuccessText="文章发布成功"
      updateSuccessText="文章更新成功"
      publishErrorText="文章发布失败，请稍后重试"
      updateErrorText="文章更新失败，请稍后重试"
    />
  )
}
