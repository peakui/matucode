import Editor from '@monaco-editor/react'
import { useMemo } from 'react'

type CodeEditorProps = {
  language: string
  value: string
  onChange: (value: string) => void
  height?: number | string
}

const monacoLanguageMap: Record<string, string> = {
  c: 'c',
  cpp: 'cpp',
  java: 'java',
  javascript: 'javascript',
  json: 'json',
  mysql: 'sql',
  python: 'python',
  shell: 'shell',
  sql: 'sql',
  typescript: 'typescript',
}

export function CodeEditor({ language, value, onChange, height = '860px' }: CodeEditorProps) {
  const editorLanguage = useMemo(() => monacoLanguageMap[language] || 'plaintext', [language])

  return (
    <div className="code-editor-shell">
      <Editor
        height={height}
        defaultLanguage="java"
        language={editorLanguage}
        value={value}
        onChange={(nextValue) => onChange(nextValue ?? '')}
        theme="vs-dark"
        options={{
          automaticLayout: true,
          fontSize: 14,
          lineHeight: 22,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          roundedSelection: true,
          padding: { top: 16 },
          tabSize: 2,
          wordWrap: 'on',
          quickSuggestions: true,
          suggestOnTriggerCharacters: true,
          formatOnPaste: true,
          formatOnType: true,
        }}
      />
    </div>
  )
}
