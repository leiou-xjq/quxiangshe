# 趣享社前端设计规范

## 1. 设计概述

趣享社(QuXiangShe)是一款社交平台前端，采用蓝色主题的现代设计语言。

## 2. 色彩规范

### 主色系
| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| 主色 | #3B82F6 | 品牌色，按钮，图标 |
| 浅蓝 | #EFF6FF | 高亮背景，标签 |
| 渐变终点 | #60A5FA | 渐变结束色 |

### 中性色系
| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| 深色文字 | #333333 | 标题，主要内容 |
| 中色文字 | #666666 | 副标题，描述 |
| 浅色文字 | #999999 | 提示，辅助信息 |
| 占位符 | #CCCCCC | 输入框占位符 |
| 边框 | #EEEEEE | 分割线，边框 |
| 背景色 | #F5F5F5 | 页面背景 |
| 卡片白 | #FFFFFF | 卡片背景 |

## 3. 圆角规范

| 元素 | 圆角值 |
|------|--------|
| 卡片 | 12px |
| 按钮 | 24px (大), 12px (小) |
| 输入框 | 24px |
| 头像 | 50% (圆形) |
| 图片 | 8px |

## 4. 阴影规范

```css
/* 轻阴影 - 卡片 */
box-shadow: 0 2px 8px rgba(0,0,0,0.04);

/* 中阴影 - 按钮悬浮 */
box-shadow: 0 4px 16px rgba(255, 36, 66, 0.3);

/* 强阴影 - 重要按钮 */
box-shadow: 0 8px 20px rgba(255, 36, 66, 0.4);
```

## 5. 字体规范

```css
font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
```

| 元素 | 大小 | 粗细 |
|------|------|------|
| 页面标题 | 20px | 600 |
| 卡片标题 | 15px | 500 |
| 正文 | 14px | 400 |
| 辅助文字 | 12px | 400 |
| 按钮 | 16px | 600 |

## 6. 间距规范

| 元素 | 间距 |
|------|------|
| 页面边距 | 16px (移动端), 20px (桌面) |
| 卡片间距 | 12px |
| 内部间距 | 16px |
| 元素间距 | 8px / 12px |

## 7. 响应式断点

```css
/* 移动端 */
@media (max-width: 480px) { }

/* 平板 */
@media (min-width: 481px) and (max-width: 768px) { }

/* 桌面 */
@media (min-width: 769px) { }
```

## 8. 组件规范

### 按钮
```css
.btn-primary {
  background: linear-gradient(135deg, #FF2442 0%, #FF6B6B 100%);
  color: white;
  border: none;
  border-radius: 24px;
  padding: 12px 24px;
  font-weight: 600;
  transition: all 0.3s ease;
}
```

### 输入框
```css
.input-field {
  border: 1px solid #EEEEEE;
  border-radius: 24px;
  padding: 14px 20px;
  font-size: 15px;
  background: #FAFAFA;
  transition: all 0.3s ease;
}

.input-field:focus {
  border-color: #FF2442;
  background: white;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}
```

### 卡片
```css
.card {
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
```

### 导航栏
- 固定底部，高度60px
- 5个导航项：首页、搜索、发布(突出)、AI、我的
- 选中状态：#FF2442
- 未选中状态：#999999

## 9. 动画规范

```css
/* 淡入动画 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 悬浮动画 */
@keyframes hoverUp {
  to { transform: translateY(-2px); }
}

/* 加载动画 */
@keyframes spin {
  to { transform: rotate(360deg); }
}
```

过渡时间：0.2s - 0.3s

## 10. 页面列表

| 页面 | 路径 | 说明 |
|------|------|------|
| 首页 | / | 瀑布流Feed |
| 登录 | /login | 登录页 |
| 注册 | /register | 注册页 |
| 搜索 | /search | 搜索结果页 |
| 动态详情 | /post/:id/comments | 评论页 |
| 个人中心 | /profile | 用户主页 |
| AI摘要 | /ai-summary | AI摘要页 |

## 11. 通用图标

使用 Element Plus Icons:
- 首页: HomeFilled
- 搜索: Search
- 发布: Plus
- AI: ChatDotRound
- 我的: User
- 点赞: Star
- 评论: ChatDotRound
- 分享: Share
- 返回: ArrowLeft
- 更多: MoreFilled