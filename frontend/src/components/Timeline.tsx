import type { TimelineStep } from '../lib/requests'
import { dateTime } from '../lib/requests'

export function Timeline({ steps }: { steps: TimelineStep[] }) {
  return (
    <ol className="timeline" aria-label="Andamento da solicitação">
      {steps.map((step, index) => {
        const last = index === steps.length - 1
        const next = steps[index + 1]
        return (
          <li key={`${step.title}-${index}`}>
            <div className="rail">
              <span className={`dot ${step.state === 'done' ? '' : step.state}`} />
              {!last && <span className={`line ${next?.state === 'future' ? 'future' : ''}`} />}
            </div>
            <div className="step">
              <p className="step-title" style={{ fontWeight: step.state === 'current' ? 800 : step.state === 'future' ? 600 : 700, color: step.state === 'future' ? 'var(--muted-code)' : undefined }}>
                {step.title}
              </p>
              <p className="step-date">{step.date ? dateTime(step.date) : 'Quando for aprovada'}</p>
            </div>
          </li>
        )
      })}
    </ol>
  )
}
