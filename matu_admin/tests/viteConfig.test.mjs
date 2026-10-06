import assert from 'node:assert/strict'
import { copyFile, mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { join, resolve } from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import { build, loadConfigFromFile, resolveConfig } from 'vite'

const projectDir = fileURLToPath(new URL('..', import.meta.url))

test('管理端根环境与公开配置契约', async (t) => {
  // 使用项目内临时夹具，不读取或改写用户实际的根 .env。
  const fixtureRoot = await mkdtemp(join(projectDir, 'node_modules', '.p01-config-'))
  const frontendDir = join(fixtureRoot, 'frontend')
  const configFile = join(frontendDir, 'vite.config.mts')
  const originalCwd = process.cwd()
  const envKeys = ['VITE_API_TARGET', 'VITE_API_BASE_URL', 'P01_BACKEND_SECRET', 'NODE_ENV']
  const previousEnv = Object.fromEntries(envKeys.map((key) => [key, process.env[key]]))
  for (const key of envKeys) delete process.env[key]

  try {
    await mkdir(frontendDir)
    await copyFile(join(projectDir, 'vite.config.ts'), configFile)
    await writeFile(join(frontendDir, '.env'), 'VITE_API_TARGET=http://wrong-project.example\nVITE_API_BASE_URL=/wrong-project\n')
    const load = async () => {
      const result = await loadConfigFromFile({ command: 'serve', mode: 'development' }, configFile)
      assert.ok(result)
      return result.config
    }

    await t.test('忽略项目内环境文件，缺省代理为 127.0.0.1:8080', async () => {
      process.chdir(frontendDir)
      const config = await load()
      assert.equal(resolve(config.envDir), resolve(fixtureRoot))
      assert.equal(config.envPrefix, 'VITE_')
      assert.equal(config.define, undefined)
      assert.equal(config.server.proxy['/api'].target, 'http://127.0.0.1:8080')
    })

    await writeFile(join(fixtureRoot, '.env'), [
      'VITE_API_TARGET=http://127.0.0.1:18080',
      'VITE_API_BASE_URL=/root-public-api',
      'P01_BACKEND_SECRET=private-p01-fixture-secret',
    ].join('\n'))

    await t.test('从根目录或项目目录启动均读取同一根环境与 API 代理配置', async () => {
      for (const cwd of [fixtureRoot, frontendDir]) {
        process.chdir(cwd)
        const config = await load()
        const proxy = config.server.proxy['/api']
        assert.equal(proxy.target, 'http://127.0.0.1:18080')
        assert.equal(proxy.changeOrigin, true)
        assert.equal(proxy.rewrite('/api/files/upload?x=1'), '/files/upload?x=1')
        assert.equal(proxy.configure, undefined)
      }
    })

    await t.test('公开变量来自根环境，非 VITE_ 变量不暴露', async () => {
      const config = await resolveConfig({ root: frontendDir, configFile, logLevel: 'silent' }, 'serve')
      assert.equal(config.env.VITE_API_BASE_URL, '/root-public-api')
      assert.equal(config.env.P01_BACKEND_SECRET, undefined)
      assert.ok(!Object.keys(config.define || {}).some((key) => key.startsWith('import.meta.env') || key.startsWith('process.env')))
    })

    await t.test('进程公开变量优先，空白代理配置回退默认值', async () => {
      process.env.VITE_API_TARGET = ' http://127.0.0.1:19090 '
      assert.equal((await load()).server.proxy['/api'].target, 'http://127.0.0.1:19090')
      process.env.VITE_API_TARGET = '   '
      assert.equal((await load()).server.proxy['/api'].target, 'http://127.0.0.1:8080')
      delete process.env.VITE_API_TARGET
    })

    await t.test('实际构建产物包含公开配置但不包含后端测试凭据', async () => {
      await writeFile(join(frontendDir, 'index.html'), '<html><body><script type="module" src="/main.js"></script></body></html>')
      await writeFile(join(frontendDir, 'main.js'), 'document.body.dataset.environment = JSON.stringify(import.meta.env)')
      const result = await build({ root: frontendDir, configFile, logLevel: 'silent', build: { write: false, minify: false } })
      const bundles = Array.isArray(result) ? result : [result]
      const output = bundles.flatMap((bundle) => bundle.output).map((item) => item.type === 'chunk' ? item.code : String(item.source)).join('\n')
      assert.ok(output.includes('/root-public-api'))
      assert.ok(!output.includes('private-p01-fixture-secret'))
      assert.ok(!output.includes('P01_BACKEND_SECRET'))
      assert.ok(!output.includes('/wrong-project'))
    })
  } finally {
    process.chdir(originalCwd)
    for (const [key, value] of Object.entries(previousEnv)) {
      if (value === undefined) delete process.env[key]
      else process.env[key] = value
    }
    await rm(fixtureRoot, { recursive: true, force: true })
  }
})
