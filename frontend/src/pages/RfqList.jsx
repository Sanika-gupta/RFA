import { useState } from 'react'
import { api } from '../api.js'
import { fmtMoney, fmtTime, STATUS_LABELS } from '../format.js'
import usePolling from '../usePolling.js'

export default function RfqList() {
  const [rfqs, setRfqs] = useState(null)
  const [error, setError] = useState('')

  usePolling(() => {
    api.listRfqs().then((data) => { setRfqs(data); setError('') }).catch((e) => setError(e.message))
  }, 5000, [])

  if (error) return <p className="error">{error}</p>
  if (!rfqs) return <p>Loading…</p>

  return (
    <section>
      <h1>British Auctions</h1>
      {rfqs.length === 0 ? (
        <p className="muted">No RFQs yet. <a href="#/new">Create one</a>.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th><th>RFQ name</th><th>Lowest bid</th>
              <th>Current close</th><th>Forced close</th><th>Status</th>
            </tr>
          </thead>
          <tbody>
            {rfqs.map((r) => (
              <tr key={r.id} className="clickable" onClick={() => (window.location.hash = `#/rfq/${r.id}`)}>
                <td>#{r.id}</td>
                <td><a href={`#/rfq/${r.id}`}>{r.name}</a></td>
                <td>{r.lowestBid == null ? '-' : `${fmtMoney(r.lowestBid)} (${r.l1Supplier})`}</td>
                <td>{fmtTime(r.bidCloseAt)}</td>
                <td>{fmtTime(r.forcedCloseAt)}</td>
                <td><span className={`badge ${r.status}`}>{STATUS_LABELS[r.status]}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
