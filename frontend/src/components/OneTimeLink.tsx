import { useState } from 'react'
import { AlertIcon, CopyIcon, MailIcon, ShareIcon } from './Icons'

interface OneTimeLinkProps {
  url: string
  intro: React.ReactNode
  warning: string
  subject: string
  message: string
}

function canUseShareSheet(): boolean {
  return typeof navigator !== 'undefined' && typeof navigator.share === 'function'
}

export function OneTimeLink({ url, intro, warning, subject, message }: OneTimeLinkProps) {
  const [copied, setCopied] = useState(false)

  async function copy() {
    try {
      await navigator.clipboard.writeText(url)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  async function share() {
    try {
      await navigator.share({ title: subject, text: message })
    } catch {
      return
    }
  }

  return (
    <div className="notice-box" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <p>{intro}</p>
      <p className="warning-box" role="alert">
        <AlertIcon />
        <span>{warning}</span>
      </p>
      <code style={{ minWidth: 0, overflowWrap: 'anywhere', background: 'var(--surface)', color: 'var(--text)', border: '1px solid var(--border)', padding: '10px 12px', borderRadius: 10, fontSize: 14, fontWeight: 400 }}>{url}</code>
      <div className="link-actions">
        <button type="button" className="btn btn-primary" onClick={copy}><CopyIcon />{copied ? 'Copiado' : 'Copiar link'}</button>
        {canUseShareSheet() ? (
          <button type="button" className="btn btn-quiet" onClick={share}><ShareIcon />Compartilhar</button>
        ) : (
          <>
            <a className="btn btn-quiet" href={`https://wa.me/?text=${encodeURIComponent(message)}`} target="_blank" rel="noopener noreferrer" aria-label="Enviar pelo WhatsApp">
              <ShareIcon />WhatsApp
            </a>
            <a className="btn btn-quiet" href={`mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(message)}`} aria-label="Enviar por e-mail">
              <MailIcon />E-mail
            </a>
          </>
        )}
      </div>
    </div>
  )
}
