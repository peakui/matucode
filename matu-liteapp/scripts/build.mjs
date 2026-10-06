import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const candidates = [
  process.env.HBUILDERX_HOME,
  'D:/HBuilderX3.99',
  'D:/HBuilderX2/HBuilderX',
  'D:/HBuilderX',
].filter(Boolean)
const home = candidates.find((dir) =>
  existsSync(path.join(dir, 'plugins/uniapp-cli-vite/node_modules/@dcloudio/vite-plugin-uni/bin/uni.js'))
)
if (!home) {
  console.error('未找到 HBuilderX Vue 3 编译器。请安装对应插件并设置 HBUILDERX_HOME。')
  process.exit(1)
}
const platform = process.argv[2] || 'mp-weixin'
if (!['mp-weixin', 'h5'].includes(platform)) throw new Error('仅支持 mp-weixin / h5')
const compiler = path.join(home, 'plugins/uniapp-cli-vite')
const result = spawnSync(
  process.execPath,
  [path.join(compiler, 'node_modules/@dcloudio/vite-plugin-uni/bin/uni.js'), 'build', '-p', platform],
  {
    cwd: compiler,
    stdio: 'inherit',
    env: {
      ...process.env,
      UNI_INPUT_DIR: root,
      UNI_OUTPUT_DIR: path.join(root, 'unpackage/dist/build', platform),
      UNI_CLI_CONTEXT: compiler,
    },
  }
)
if (result.error) console.error(result.error.message)
process.exit(result.status ?? 1)
