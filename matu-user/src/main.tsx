import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { Provider } from 'react-redux'
import { ConfigProvider, App as AntdApp, theme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import 'antd/dist/reset.css'
import './index.scss'
import App from './App'
import { ScrollToTop } from './components/ScrollToTop/index'
import { store } from './store'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <Provider store={store}>
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#2563EB',
          colorInfo: '#2563EB',
          borderRadius: 14,
          fontFamily: '"Noto Sans SC", "Microsoft YaHei", sans-serif',
          colorBgLayout: '#F8FAFC',
        },
      }}
    >
      <AntdApp>
        <BrowserRouter>
          <ScrollToTop />
          <App />
        </BrowserRouter>
      </AntdApp>
    </ConfigProvider>
  </Provider>,
)
