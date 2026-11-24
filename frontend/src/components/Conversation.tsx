import { useRef, useState } from 'react'
import type { RequestScope } from '../api/api'
import { thread } from '../api/api'
import type { ActorType, Attachment, Comment } from '../api/types'
import { fileSize, relativeDay } from '../lib/requests'
import { messageOf } from '../lib/useLoad'
import { ClipIcon, CloseIcon, DownloadIcon, FileIcon, ImageIcon } from './Icons'

interface Props {
  scope: RequestScope
  viewer: ActorType
  comments: Comment[]
  attachments: Attachment[]
  composerLabel: string
  disabled?: boolean
  onChanged: () => void
}

const ACCEPT = '.png,.jpg,.jpeg,.webp,.gif,.pdf,.txt,.csv,.zip,.docx,.xlsx,.pptx'

export function Conversation({ scope, viewer, comments, attachments, composerLabel, disabled, onChanged }: Props) {
  const [body, setBody] = useState('')
  const [files, setFiles] = useState<File[]>([])
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const fileInput = useRef<HTMLInputElement>(null)

  const loose = attachments.filter((a) => !a.commentId || !comments.some((c) => c.id === a.commentId))
  const items = [
    ...comments.map((c) => ({ kind: 'comment' as const, at: c.createdAt, comment: c })),
    ...loose.map((a) => ({ kind: 'file' as const, at: a.createdAt, attachment: a })),
  ].sort((x, y) => x.at.localeCompare(y.at))

  async function download(attachment: Attachment) {
    try {
      const { downloadUrl } = await thread.downloadUrl(scope, attachment.id)
      window.open(downloadUrl, '_blank', 'noopener,noreferrer')
    } catch (reason) {
      setError(messageOf(reason))
    }
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!body.trim() && files.length === 0) return
    setSending(true)
    setError(null)
    try {
      const comment = body.trim() ? await thread.addComment(scope, body.trim()) : null
      for (const file of files) {
        await thread.upload(scope, file, comment?.id)
      }
      setBody('')
      setFiles([])
      onChanged()
    } catch (reason) {
      setError(messageOf(reason))
      onChanged()
    } finally {
      setSending(false)
    }
  }

  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <h2 style={{ fontSize: 20, fontWeight: 800 }}>Conversa</h2>
      {items.length === 0 && <p style={{ color: 'var(--muted)' }}>Ainda não há mensagens nesta solicitação.</p>}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {items.map((item) => {
          if (item.kind === 'comment') {
            const c = item.comment
            const own = c.authorType === viewer
            const files = attachments.filter((a) => a.commentId === c.id)
            return (
              <div key={c.id} className={`bubble ${own ? 'mine' : 'theirs'}`}>
                <p>{c.body}</p>
                {files.length > 0 && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 10 }}>
                    {files.map((a) => <FileTile key={a.id} attachment={a} onOpen={download} />)}
                  </div>
                )}
                <p className="bubble-meta">{own ? 'Você' : c.authorName}, {relativeDay(c.createdAt)}</p>
              </div>
            )
          }
          const a = item.attachment
          const own = a.uploadedByType === viewer
          return (
            <div key={a.id} className={`bubble ${own ? 'mine' : 'theirs'}`}>
              <FileTile attachment={a} onOpen={download} />
              <p className="bubble-meta">{own ? 'Você' : a.uploadedByName}, {relativeDay(a.createdAt)}</p>
            </div>
          )
        })}
      </div>

      {!disabled && (
        <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 8 }}>
          <div className="field">
            <label htmlFor="mensagem">{composerLabel}</label>
            <textarea
              id="mensagem"
              className="input"
              rows={3}
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Ex.: a foto ficou ótima, só deixe um pouco mais clara"
              maxLength={5000}
            />
          </div>
          {files.length > 0 && (
            <ul className="list-plain" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {files.map((file) => (
                <li key={file.name} style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 15 }}>
                  <FileIcon size={16} />
                  <span style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{file.name}</span>
                  <span style={{ color: 'var(--muted)' }}>{fileSize(file.size)}</span>
                  <button type="button" className="btn btn-quiet" style={{ minHeight: 36, padding: '0 10px' }} aria-label={`Remover ${file.name}`} onClick={() => setFiles(files.filter((f) => f !== file))}>
                    <CloseIcon size={16} />
                  </button>
                </li>
              ))}
            </ul>
          )}
          {error && <p className="error-box" role="alert">{error}</p>}
          <div style={{ display: 'flex', gap: 10 }}>
            <input
              ref={fileInput}
              type="file"
              multiple
              accept={ACCEPT}
              className="visually-hidden"
              id="anexos"
              onChange={(e) => {
                setFiles([...files, ...Array.from(e.target.files ?? [])])
                e.target.value = ''
              }}
            />
            <button type="button" className="btn btn-quiet btn-icon" aria-label="Anexar arquivo" onClick={() => fileInput.current?.click()}>
              <ClipIcon size={20} />
            </button>
            <button type="submit" className="btn btn-primary" style={{ flex: 1 }} disabled={sending || (!body.trim() && files.length === 0)}>
              {sending ? 'Enviando…' : 'Enviar mensagem'}
            </button>
          </div>
        </form>
      )}
    </section>
  )
}

function FileTile({ attachment, onOpen }: { attachment: Attachment; onOpen: (a: Attachment) => void }) {
  const image = attachment.contentType.startsWith('image/')
  return (
    <button type="button" className="file-tile" onClick={() => onOpen(attachment)}>
      <span className="file-thumb" aria-hidden="true">{image ? <ImageIcon size={22} /> : <FileIcon size={22} />}</span>
      <span style={{ flex: 1, minWidth: 0 }}>
        <span className="file-name">{attachment.originalName}</span>
        <span className="file-size">{fileSize(attachment.sizeBytes)}</span>
      </span>
      <DownloadIcon size={20} label={`Baixar ${attachment.originalName}`} />
    </button>
  )
}
