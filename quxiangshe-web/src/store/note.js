import { defineStore } from 'pinia'
import { noteApi } from '@/api/note'

export const useNoteStore = defineStore('note', {
  state: () => ({
    // 笔记列表
    notes: [],
    // 首页笔记
    homeNotes: [],
    // 用户笔记
    userNotes: [],
    // 搜索结果
    searchResults: {
      notes: [],
      users: [],
      totalCount: 0,
      hasMore: false,
      type: 'all'
    },
    // 当前笔记详情
    currentNote: null,
    // 分页状态
    lastNoteId: null,
    hasMore: false,
    loading: false,
    // 用户笔记分页
    userLastNoteId: null,
    userHasMore: false,
    userLoading: false
  }),

  actions: {
    // 获取首页笔记
    async fetchHomeNotes(lastId = null, size = 20) {
      if (this.loading) return
      this.loading = true
      try {
        const data = await noteApi.getHomeNotes(lastId, size)
        if (lastId) {
          this.homeNotes = [...this.homeNotes, ...data.items]
        } else {
          this.homeNotes = data.items || []
        }
        this.lastNoteId = data.lastNoteId
        this.hasMore = data.hasMore
      } finally {
        this.loading = false
      }
    },

    // 获取用户笔记
    async fetchUserNotes(userId, lastId = null, size = 20) {
      if (this.userLoading) return
      this.userLoading = true
      try {
        const data = await noteApi.getUserNotes(userId, lastId, size)
        if (lastId) {
          this.userNotes = [...this.userNotes, ...data.items]
        } else {
          this.userNotes = data.items || []
        }
        this.userLastNoteId = data.lastNoteId
        this.userHasMore = data.hasMore
      } finally {
        this.userLoading = false
      }
    },

    // 获取笔记详情
    async fetchNoteDetail(noteId) {
      this.loading = true
      try {
        this.currentNote = await noteApi.getNoteDetail(noteId)
        return this.currentNote
      } finally {
        this.loading = false
      }
    },

    // 创建笔记
    async createNote(noteData) {
      const result = await noteApi.createNote(noteData)
      const noteId = result.data?.noteId
      // 创建成功后刷新首页和用户笔记列表
      this.homeNotes = []
      this.lastNoteId = null
      this.userNotes = []
      this.userLastNoteId = null
      await this.fetchHomeNotes()
      return { ...result, noteId }
    },

    // 删除笔记
    async deleteNote(noteId) {
      await noteApi.deleteNote(noteId)
      // 从列表中移除
      this.homeNotes = this.homeNotes.filter(n => n.noteId !== noteId)
      this.userNotes = this.userNotes.filter(n => n.noteId !== noteId)
    },

    // 点赞
    async likeNote(noteId) {
      await noteApi.likeNote(noteId)
      // 更新本地状态
      this.updateNoteLiked(noteId, true)
    },

    // 取消点赞
    async unlikeNote(noteId) {
      await noteApi.unlikeNote(noteId)
      this.updateNoteLiked(noteId, false)
    },

    // 收藏
    async collectNote(noteId) {
      await noteApi.collectNote(noteId)
      this.updateNoteCollected(noteId, true)
    },

    // 取消收藏
    async uncollectNote(noteId) {
      await noteApi.uncollectNote(noteId)
      this.updateNoteCollected(noteId, false)
    },

    // 搜索
    async search(keyword, type = 'all', page = 1, size = 20) {
      this.loading = true
      try {
        let data
        if (type === 'all') {
          data = await noteApi.search(keyword, type, page, size)
        } else if (type === 'note') {
          data = await noteApi.searchNotes(keyword, null, page, size)
        } else if (type === 'user') {
          data = await noteApi.searchUsers(keyword, page, size)
        }
        this.searchResults = {
          notes: data.notes || [],
          users: data.users || [],
          totalCount: data.totalCount || 0,
          hasMore: data.hasMore || false,
          type: type
        }
        return this.searchResults
      } finally {
        this.loading = false
      }
    },

    // 辅助方法：更新笔记点赞状态
    updateNoteLiked(noteId, liked) {
      const updateNote = (notes) => {
        const note = notes.find(n => n.noteId === noteId)
        if (note) {
          note.isLiked = liked
          note.likeCount = (note.likeCount || 0) + (liked ? 1 : -1)
        }
      }
      updateNote(this.homeNotes)
      updateNote(this.userNotes)
      if (this.currentNote && this.currentNote.noteId === noteId) {
        this.currentNote.isLiked = liked
        this.currentNote.likeCount = (this.currentNote.likeCount || 0) + (liked ? 1 : -1)
      }
    },

    // 辅助方法：更新笔记收藏状态
    updateNoteCollected(noteId, collected) {
      const updateNote = (notes) => {
        const note = notes.find(n => n.noteId === noteId)
        if (note) {
          note.isCollected = collected
          note.collectCount = (note.collectCount || 0) + (collected ? 1 : -1)
        }
      }
      updateNote(this.homeNotes)
      updateNote(this.userNotes)
      if (this.currentNote && this.currentNote.noteId === noteId) {
        this.currentNote.isCollected = collected
        this.currentNote.collectCount = (this.collectCount || 0) + (collected ? 1 : -1)
      }
    },

    // 清空状态
    clearNotes() {
      this.homeNotes = []
      this.lastNoteId = null
      this.hasMore = false
    },

    clearSearch() {
      this.searchResults = {
        notes: [],
        users: [],
        totalCount: 0,
        hasMore: false,
        type: 'all'
      }
    }
  }
})
