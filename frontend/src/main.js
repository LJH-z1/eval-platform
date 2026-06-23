import { createApp } from 'vue'
import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import 'element-plus/dist/index.css'
import './styles/global.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
// 全局注册所有 element-plus icons(让 <Loading /> / <Promotion /> 等能直接用)
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp)
}
app.mount('#app')
