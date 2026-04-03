openapi: 3.0.0
info:
  title: 趣享社笔记模块接口文档
  description: 趣享社笔记模块REST API接口规范，涵盖笔记发布/编辑/删除、列表查询、详情、点赞/收藏、搜索等功能
  version: 1.0.0
  contact:
    name: 趣享社开发团队
    url: https://quxiangshe.com

servers:
  - url: http://localhost:8080/api/v1
    description: 本地开发环境
  - url: https://api.quxiangshe.com/api/v1
    description: 生产环境

tags:
  - name: 笔记管理
    description: 笔记的创建、查询、删除、审核等操作
  - name: 互动功能
    description: 笔记的点赞和收藏功能
  - name: 搜索功能
    description: 笔记和用户的搜索功能

# ============================================
# 安全方案
# ============================================
security:
  - BearerAuth: []

components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: 使用JWT Token进行身份验证

  # 通用响应
  responses:
    SuccessResponse:
      description: 成功响应
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ApiResponse'
    ErrorResponse:
      description: 错误响应
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ApiResponse'

  schemas:
    # 通用API响应
    ApiResponse:
      type: object
      properties:
        code:
          type: integer
          description: 响应码，0表示成功
          example: 0
        message:
          type: string
          description: 响应信息
          example: success
        data:
          type: object
          description: 响应数据
          nullable: true

    # 笔记响应DTO
    NoteResponseDTO:
      type: object
      properties:
        noteId:
          type: integer
          format: int64
          description: 笔记ID
        userId:
          type: integer
          format: int64
          description: 发布者用户ID
        username:
          type: string
          description: 用户名
        nickname:
          type: string
          description: 用户昵称
        avatarUrl:
          type: string
          description: 用户头像URL
        title:
          type: string
          description: 标题
        content:
          type: string
          description: 正文内容
        coverImage:
          type: string
          description: 封面图片URL
        category:
          type: string
          description: 分类
        tags:
          type: array
          items:
            type: string
          description: 标签列表
        images:
          type: array
          items:
            type: string
          description: 图片列表
        likeCount:
          type: integer
          description: 点赞数
        commentCount:
          type: integer
          description: 评论数
        collectCount:
          type: integer
          description: 收藏数
        viewCount:
          type: integer
          description: 浏览数
        isLiked:
          type: boolean
          description: 当前用户是否点赞
        isCollected:
          type: boolean
          description: 当前用户是否收藏
        auditStatus:
          type: integer
          description: 审核状态，0=待审核，1=通过，2=拒绝
        rejectReason:
          type: string
          description: 拒绝原因
        createTime:
          type: string
          description: 创建时间

    # 笔记列表响应
    NoteListResponse:
      type: object
      properties:
        items:
          type: array
          items:
            $ref: '#/components/schemas/NoteResponseDTO'
          description: 笔记列表
        lastNoteId:
          type: integer
          format: int64
          description: 最后一条笔记ID，用于游标分页
        hasMore:
          type: boolean
          description: 是否有更多数据

    # 创建笔记请求
    NoteCreateRequest:
      type: object
      required:
        - title
        - content
      properties:
        title:
          type: string
          description: 标题（1-200字符）
          minLength: 1
          maxLength: 200
          example: 日本东京旅行攻略
        content:
          type: string
          description: 正文内容（1-10000字符）
          minLength: 1
          maxLength: 10000
          example: 东京是日本的首都，也是旅行热门目的地...
        coverImage:
          type: string
          description: 封面图片URL
          example: https://example.com/cover.jpg
        category:
          type: string
          description: 分类，默认"默认"
          example: 旅行
        tags:
          type: array
          items:
            type: string
          description: 标签数组
          example: ["风景", "日本", "东京"]
        images:
          type: array
          items:
            type: string
          description: 图片URL数组
          example: ["https://example.com/image1.jpg"]

    # 搜索响应DTO
    SearchResponseDTO:
      type: object
      properties:
        type:
          type: string
          description: 结果类型：all/note/user
          enum: [all, note, user]
        notes:
          type: array
          items:
            $ref: '#/components/schemas/NoteSearchResult'
          description: 笔记搜索结果
        users:
          type: array
          items:
            $ref: '#/components/schemas/UserSearchResult'
          description: 用户搜索结果
        totalCount:
          type: integer
          format: int64
          description: 总记录数
        page:
          type: integer
          description: 当前页码
        size:
          type: integer
          description: 每页数量
        hasMore:
          type: boolean
          description: 是否有更多
        costTime:
          type: integer
          format: int64
          description: 搜索耗时（毫秒）

    # 笔记搜索结果（带高亮）
    NoteSearchResult:
      type: object
      properties:
        noteId:
          type: integer
          format: int64
          description: 笔记ID
        userId:
          type: integer
          format: int64
          description: 发布者ID
        username:
          type: string
          description: 发布者用户名
        nickname:
          type: string
          description: 发布者昵称
        avatarUrl:
          type: string
          description: 发布者头像
        title:
          type: string
          description: 标题（原始）
        content:
          type: string
          description: 内容（原始）
        coverImage:
          type: string
          description: 封面图片URL
        category:
          type: string
          description: 分类
        tags:
          type: array
          items:
            type: string
          description: 标签
        likeCount:
          type: integer
          description: 点赞数
        commentCount:
          type: integer
          description: 评论数
        collectCount:
          type: integer
          description: 收藏数
        viewCount:
          type: integer
          description: 浏览数
        createTime:
          type: string
          description: 创建时间
        highlightTitle:
          type: string
          description: 高亮标题（带<em>标签）
        highlightContent:
          type: string
          description: 高亮内容（带<em>标签）

    # 用户搜索结果（带高亮）
    UserSearchResult:
      type: object
      properties:
        userId:
          type: integer
          format: int64
          description: 用户ID
        username:
          type: string
          description: 用户名（原始）
        nickname:
          type: string
          description: 昵称（原始）
        avatarUrl:
          type: string
          description: 头像
        bio:
          type: string
          description: 个人简介
        createTime:
          type: string
          description: 创建时间
        highlightUsername:
          type: string
          description: 高亮用户名（带<em>标签）
        highlightNickname:
          type: string
          description: 高亮昵称（带<em>标签）

# ============================================
# 路径定义
# ============================================
paths:
  # ========================================
  # 笔记管理接口
  # ========================================

  # 创建笔记
  /notes:
    post:
      tags:
        - 笔记管理
      summary: 创建笔记
      description: 用户创建新的笔记，支持标题、正文、封面、分类、标签、图片等功能
      operationId: createNote
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/NoteCreateRequest'
      parameters:
        - name: Authorization
          in: header
          required: true
          description: Bearer Token
          schema:
            type: string
            example: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
      responses:
        '200':
          description: 创建成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                    example: 0
                  message:
                    type: string
                    example: success
                  data:
                    $ref: '#/components/schemas/NoteResponseDTO'
              example:
                code: 0
                message: success
                data:
                  noteId: 123456789
                  title: 日本东京旅行攻略
                  content: 东京是日本的首都
                  auditStatus: 1
                  rejectReason: null
                  createTime: "2026-04-03T10:30:00"
        '400':
          description: 参数校验失败
          content:
            application/json:
              example:
                code: 400
                message: 标题不能为空
                data: null
        '429':
          description: 请求过于频繁
          content:
            application/json:
              example:
                code: 429
                message: 请求过于频繁，请稍后再试
                data: null

  # 获取首页笔记列表
  /notes:
    get:
      tags:
        - 笔记管理
      summary: 获取首页笔记列表
      description: 获取首页推荐的笔记列表，支持游标分页
      operationId: getHomeNotes
      parameters:
        - name: lastId
          in: query
          required: false
          description: 游标，最后一条笔记ID
          schema:
            type: integer
            format: int64
        - name: size
          in: query
          required: false
          description: 每页数量，默认20
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                    example: 0
                  message:
                    type: string
                    example: success
                  data:
                    $ref: '#/components/schemas/NoteListResponse'

  # 获取笔记详情
  /notes/{noteId}:
    get:
      tags:
        - 笔记管理
      summary: 获取笔记详情
      description: 根据笔记ID获取笔记详情，返回完整笔记信息及当前用户的点赞/收藏状态
      operationId: getNoteDetail
      parameters:
        - name: noteId
          in: path
          required: true
          description: 笔记ID
          schema:
            type: integer
            format: int64
          example: 123456789
        - name: Authorization
          in: header
          required: false
          description: Bearer Token（可选，用于获取当前用户点赞/收藏状态）
          schema:
            type: string
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                    example: 0
                  message:
                    type: string
                    example: success
                  data:
                    $ref: '#/components/schemas/NoteResponseDTO'
              example:
                code: 0
                message: success
                data:
                  noteId: 123456789
                  userId: 100001
                  username: user001
                  nickname: 旅行达人
                  avatarUrl: https://example.com/avatar.jpg
                  title: 日本东京旅行攻略
                  content: 东京是日本的首都...
                  coverImage: https://example.com/cover.jpg
                  category: 旅行
                  tags: ["风景", "日本"]
                  images: ["https://example.com/image1.jpg"]
                  likeCount: 100
                  commentCount: 20
                  collectCount: 15
                  viewCount: 1000
                  isLiked: false
                  isCollected: false
                  auditStatus: 1
                  createTime: "2026-04-03T10:30:00"
        '404':
          description: 笔记不存在
          content:
            application/json:
              example:
                code: 404
                message: 笔记不存在
                data: null

  # 删除笔记
  /notes/{noteId}:
    delete:
      tags:
        - 笔记管理
      summary: 删除笔记
      description: 删除指定的笔记，只有笔记作者才能删除
      operationId: deleteNote
      security:
        - BearerAuth: []
      parameters:
        - name: noteId
          in: path
          required: true
          description: 笔记ID
          schema:
            type: integer
            format: int64
          example: 123456789
        - name: Authorization
          in: header
          required: true
          description: Bearer Token
          schema:
            type: string
      responses:
        '200':
          description: 删除成功
          content:
            application/json:
              example:
                code: 0
                message: success
                data: null
        '403':
          description: 无权限操作
          content:
            application/json:
              example:
                code: 403
                message: 无权限操作
                data: null
        '404':
          description: 笔记不存在
          content:
            application/json:
              example:
                code: 404
                message: 笔记不存在
                data: null

  # 获取用户笔记列表
  /users/{userId}/notes:
    get:
      tags:
        - 笔记管理
      summary: 获取用户笔记列表
      description: 获取指定用户发布的所有笔记列表
      operationId: getUserNotes
      parameters:
        - name: userId
          in: path
          required: true
          description: 用户ID
          schema:
            type: integer
            format: int64
          example: 100001
        - name: lastId
          in: query
          required: false
          description: 游标，最后一条笔记ID
          schema:
            type: integer
            format: int64
        - name: size
          in: query
          required: false
          description: 每页数量，默认20
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                    example: 0
                  message:
                    type: string
                    example: success
                  data:
                    $ref: '#/components/schemas/NoteListResponse'

  # 搜索笔记
  /notes/search:
    get:
      tags:
        - 搜索功能
      summary: 搜索笔记
      description: 使用ES搜索笔记，支持关键词、分类筛选、分页
      operationId: searchNotes
      parameters:
        - name: keyword
          in: query
          required: false
          description: 搜索关键词
          schema:
            type: string
          example: 东京
        - name: category
          in: query
          required: false
          description: 分类筛选
          schema:
            type: string
          example: 旅行
        - name: page
          in: query
          required: false
          description: 页码，默认1
          schema:
            type: integer
            default: 1
        - name: size
          in: query
          required: false
          description: 每页数量，默认20
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                    example: 0
                  message:
                    type: string
                    example: success
                  data:
                    $ref: '#/components/schemas/NoteListResponse'

  # ========================================
  # 互动功能接口
  # ========================================

  # 点赞笔记
  /notes/{noteId}/like:
    post:
      tags:
        - 互动功能
      summary: 点赞笔记
      description: 为指定的笔记点赞，同一用户对同一笔记只能点赞一次
      operationId: likeNote
      security:
        - BearerAuth: []
      parameters:
        - name: noteId
          in: path
          required: true
          description: 笔记ID
          schema:
            type: integer
            format: int64
          example: 123456789
        - name: Authorization
          in: header
          required: true
          description: Bearer Token
          schema:
            type: string
      responses:
        '200':
          description: 点赞成功
          content:
            application/json:
              example:
                code: 0
                message: success
                data: null
        '400':
          description: 已点赞
          content:
            application/json:
              example:
                code: 400
                message: 已点赞
                data: null
        '404':
          description: 笔记不存在
          content:
            application/json:
              example:
                code: 404
                message: 笔记不存在
                data: null
        '429':
          description: 请求过于频繁
          content:
            application/json:
              example:
                code: 429
                message: 请求过于频繁，请稍后再试
                data: null

  # 取消点赞
  /notes/{noteId}/like:
    delete:
      tags:
        - 互动功能
      summary: 取消点赞
      description: 取消对笔记的点赞
      operationId: unlikeNote
      security:
        - BearerAuth: []
      parameters:
        - name: noteId
          in: path
          required: true
          description: 笔记ID
          schema:
            type: integer
            format: int64
          example: 123456789
        - name: Authorization
          in: header
          required: true
          description: Bearer Token
          schema:
            type: string
      responses:
        '200':
          description: 取消成功
          content:
            application/json:
              example:
                code: 0
                message: success
                data: null
        '400':
          description: 未点赞
          content:
            application/json:
              example:
                code: 400
                message: 未点赞
                data: null

  # 收藏笔记
  /notes/{noteId}/collect:
    post:
      tags:
        - 互动功能
      summary: 收藏笔记
      description: 收藏指定的笔记，同一用户对同一笔记只能收藏一次
      operationId: collectNote
      security:
        - BearerAuth: []
      parameters:
        - name: noteId
          in: path
          required: true
          description: 笔记ID
          schema:
            type: integer
            format: int64
          example: 123456789
        - name: Authorization
          in: header
          required: true
          description: Bearer Token
          schema:
            type: string
      responses:
        '200':
          description: 收藏成功
          content:
            application/json:
              example:
                code: 0
                message: success
                data: null
        '400':
          description: 已收藏
          content:
            application/json:
              example:
                code: 400
                message: 已收藏
                data: null
        '404':
          description: 笔记不存在
          content:
            application/json:
              example:
                code: 404
                message: 笔记不存在
                data: null

  # 取消收藏
  /notes/{noteId}/collect:
    delete:
      tags:
        - 互动功能
      summary: 取消收藏
      description: 取消对笔记的收藏
      operationId: uncollectNote
      security:
        - BearerAuth: []
      parameters:
        - name: noteId
          in: path
          required: true
          description: 笔记ID
          schema:
            type: integer
            format: int64
          example: 123456789
        - name: Authorization
          in: header
          required: true
          description: Bearer Token
          schema:
            type: string
      responses:
        '200':
          description: 取消成功
          content:
            application/json:
              example:
                code: 0
                message: success
                data: null
        '400':
          description: 未收藏
          content:
            application/json:
              example:
                code: 400
                message: 未收藏
                data: null

  # ========================================
  # 统一搜索接口
  # ========================================

  # 统一搜索（混合搜索笔记和用户）
  /search:
    get:
      tags:
        - 搜索功能
      summary: 统一搜索
      description: 支持笔记和用户的混合搜索，返回带高亮的结果
      operationId: search
      parameters:
        - name: keyword
          in: query
          required: false
          description: 搜索关键词
          schema:
            type: string
          example: 东京
        - name: type
          in: query
          required: false
          description: 搜索类型，all-全部(默认)，note-仅笔记，user-仅用户
          schema:
            type: string
            enum: [all, note, user]
            default: all
        - name: page
          in: query
          required: false
          description: 页码，默认1
          schema:
            type: integer
            default: 1
        - name: size
          in: query
          required: false
          description: 每页数量，默认20，最大50
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                    example: 0
                  message:
                    type: string
                    example: success
                  data:
                    $ref: '#/components/schemas/SearchResponseDTO'
              example:
                code: 0
                message: success
                data:
                  type: all
                  notes:
                    - noteId: 123456789
                      title: 日本东京旅行攻略
                      content: 东京是日本的首都
                      highlightTitle: 日本<em>东京</em>旅行攻略
                      highlightContent: <em>东京</em>是日本的首都
                  users:
                    - userId: 100001
                      username: traveler
                      nickname: 旅行达人
                      highlightUsername: <em>travel</em>er
                      highlightNickname: <em>旅行</em>达人
                  totalCount: 25
                  page: 1
                  size: 20
                  hasMore: true
                  costTime: 15

  # 仅搜索笔记
  /search/notes:
    get:
      tags:
        - 搜索功能
      summary: 搜索笔记
      description: 仅搜索笔记，返回带高亮的结果
      operationId: searchNotes
      parameters:
        - name: keyword
          in: query
          required: false
          description: 搜索关键词
          schema:
            type: string
        - name: category
          in: query
          required: false
          description: 分类筛选
          schema:
            type: string
        - name: page
          in: query
          required: false
          description: 页码，默认1
          schema:
            type: integer
            default: 1
        - name: size
          in: query
          required: false
          description: 每页数量，默认20
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                    example: 0
                  message:
                    type: string
                    example: success
                  data:
                    $ref: '#/components/schemas/SearchResponseDTO'

  # 仅搜索用户
  /search/users:
    get:
      tags:
        - 搜索功能
      summary: 搜索用户
      description: 仅搜索用户，返回带高亮的结果
      operationId: searchUsers
      parameters:
        - name: keyword
          in: query
          required: false
          description: 搜索关键词
          schema:
            type: string
        - name: page
          in: query
          required: false
          description: 页码，默认1
          schema:
            type: integer
            default: 1
        - name: size
          in: query
          required: false
          description: 每页数量，默认20
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                    example: 0
                  message:
                    type: string
                    example: success
                  data:
                    $ref: '#/components/schemas/SearchResponseDTO'

  # ========================================
  # 管理接口
  # ========================================

  # 审核笔记
  /admin/notes/{noteId}/review:
    post:
      tags:
        - 管理功能
      summary: 审核笔记
      description: 管理员审核笔记，通过或拒绝
      operationId: reviewNote
      security:
        - BearerAuth: []
      parameters:
        - name: noteId
          in: path
          required: true
          description: 笔记ID
          schema:
            type: integer
            format: int64
        - name: approved
          in: query
          required: true
          description: 审核结果，true=通过，false=拒绝
          schema:
            type: boolean
        - name: Authorization
          in: header
          required: true
          description: Bearer Token（需要管理员权限）
          schema:
            type: string
      responses:
        '200':
          description: 审核成功
          content:
            application/json:
              example:
                code: 0
                message: success
                data: null

# ============================================
# 错误码定义
# ============================================
x-error-codes:
  common:
    - code: 0
      message: 成功
      description: 请求成功
    - code: 400
      message: 参数错误
      description: 请求参数校验失败
    - code: 401
      message: 未授权
      description: 未登录或Token无效
    - code: 403
      message: 无权限
      description: 没有权限执行此操作
    - code: 404
      message: 资源不存在
      description: 请求的资源不存在
    - code: 429
      message: 请求过于频繁
      description: 触发限流，请稍后再试
    - code: 500
      message: 服务器内部错误
      description: 服务器内部错误
  note:
    - code: 400
      message: 已点赞
      description: 重复点赞
    - code: 400
      message: 未点赞
      description: 取消点赞时未点赞
    - code: 400
      message: 已收藏
      description: 重复收藏
    - code: 400
      message: 未收藏
      description: 取消收藏时未收藏

# ============================================
# 限流规则
# ============================================
x-rate-limits:
  - path: POST /notes
    limit: 10
    window: 60s
    description: 创建笔记，每分钟10次
  - path: POST /notes/{noteId}/like
    limit: 20
    window: 60s
    description: 点赞，每分钟20次

# ============================================
# 搜索字段权重
# ============================================
x-search-weights:
  note:
    - field: title
      weight: 2
      description: 标题权重最高
    - field: content
      weight: 1
      description: 内容权重
  user:
    - field: username
      weight: 2
      description: 用户名权重最高
    - field: nickname
      weight: 2
      description: 昵称权重最高
    - field: bio
      weight: 1
      description: 个人简介权重

# ============================================
# 审核状态说明
# ============================================
x-audit-status:
  - value: 0
    name: 待审核
    description: 笔记待管理员审核
  - value: 1
    name: 通过
    description: 审核通过，笔记已发布
  - value: 2
    name: 拒绝
    description: 审核拒绝，笔记未发布
