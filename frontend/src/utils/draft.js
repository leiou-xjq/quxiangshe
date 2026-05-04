const DRAFT_KEY = 'quxiangshe_note_draft'
const DEBOUNCE_DELAY = 2000

let saveTimer = null

export function saveDraft(data) {
  if (!data) return

  const filtered = {}
  for (const key of Object.keys(data)) {
    const value = data[key]
    if (Array.isArray(value)) {
      if (value.length > 0) {
        filtered[key] = value
      }
    } else if (value !== '' && value !== null && value !== undefined) {
      filtered[key] = value
    }
  }

  if (Object.keys(filtered).length === 0) {
    localStorage.removeItem(DRAFT_KEY)
    return
  }

  filtered.savedAt = new Date().toISOString()
  localStorage.setItem(DRAFT_KEY, JSON.stringify(filtered))
}

export function loadDraft() {
  try {
    const stored = localStorage.getItem(DRAFT_KEY)
    if (!stored) return null

    const draft = JSON.parse(stored)
    const savedAt = draft.savedAt ? new Date(draft.savedAt) : null

    const oneDay = 24 * 60 * 60 * 1000
    if (savedAt && Date.now() - savedAt.getTime() > oneDay * 7) {
      clearDraft()
      return null
    }

    delete draft.savedAt

    if (draft.images && draft.images.length > 0) {
      draft.imagesExpired = true
    }

    return draft
  } catch (e) {
    console.error('Load draft error:', e)
    return null
  }
}

export function clearDraft() {
  localStorage.removeItem(DRAFT_KEY)
}

export function debounceSave(data) {
  if (saveTimer) {
    clearTimeout(saveTimer)
  }
  saveTimer = setTimeout(() => {
    saveDraft(data)
  }, DEBOUNCE_DELAY)
}

export function getDraftInfo() {
  try {
    const stored = localStorage.getItem(DRAFT_KEY)
    if (!stored) return null

    const draft = JSON.parse(stored)
    if (!draft.savedAt) return null

    const savedAt = new Date(draft.savedAt)
    return {
      savedAt: savedAt.toLocaleString(),
      hasTitle: !!draft.title,
      hasContent: !!draft.content,
      hasImages: draft.images?.length > 0
    }
  } catch {
    return null
  }
}