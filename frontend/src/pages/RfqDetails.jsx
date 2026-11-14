import { useState } from 'react'
import { api } from '../api.js'
import { fmtMoney, fmtTime, STATUS_LABELS, TRIGGER_LABELS } from '../format.js'
import usePolling from '../usePolling.js'

const EMPTY_BID = {
  supplierName: '', freightCharge: '', originCharge: '', destinationCharge: '', transitTime: '', validity: '',
}

export default function RfqDetails({ id }) {
  const [rfq, setRfq] = useState(null)
  const [bids, setBids] = useState([])
  const [activity, setActivity] = useState([])
  const [error, setError] = useState('')

  const load = () =>
    Promise.all([api.getRfq(id), api.getBids(id), api.getActivity(id)])
      .then(([r, b, a]) => { setRfq(r); setBids(b); setActivity(a); setError('') })
      .catch((e) => setError(e.message))

  usePolling(load, 5000, [id])

  if (error && !rfq) return <p className="error">{error}</p>
  if (!rfq) return <p>Loading…</p>

  return (
    <section>
      <a href="#/" className="muted">← All auctions</a>
      <h1>
        {rfq.name} <span className="muted">#{rfq.id}</span>{' '}
        <span className={`badge ${rfq.status}`}>{STATUS_LABELS[rfq.status]}</span>
      </h1>

      <div className="grid">
        <div className="card">
          <h2>Auction timing</h2>
          <dl>
            <dt>Bid start</dt><dd>{fmtTime(rfq.startAt)}</dd>
            <dt>Current close</dt><dd><b>{fmtTime(rfq.bidCloseAt)}</b></dd>
            <dt>Forced close</dt><dd>{fmtTime(rfq.forcedCloseAt)}</dd>
            <dt>Pickup / service</dt><dd>{fmtTime(rfq.pickupDate)}</dd>
          </dl>
        </div>
        <div className="card">
          <h2>Configuration</h2>
          <dl>
            <dt>Trigger window (X)</dt><dd>{rfq.extensionWindowMinutes} min</dd>
            <dt>Extension (Y)</dt><dd>{rfq.extensionMinutes} min</dd>
            <dt>Trigger</dt><dd>{TRIGGER_LABELS[rfq.triggerType]}</dd>
          </dl>
        </div>
      </div>

      <h2>Supplier bids</h2>
      {bids.length === 0 ? <p className="muted">No bids yet.</p> : (
        <table>
          <thead>
            <tr>
              <th>Rank</th><th>Carrier</th><th>Freight</th><th>Origin</th><th>Destination</th>
              <th>Total</th><th>Transit (days)</th><th>Validity (days)</th><th>Bid time</th>
            </tr>
          </thead>
          <tbody>
            {bids.map((b) => (
              <tr key={b.bidId} className={b.rank === 'L1' ? 'l1' : ''}>
                <td><b>{b.rank}</b></td>
                <td>{b.supplierName}</td>
                <td>{fmtMoney(b.freightCharge)}</td>
                <td>{fmtMoney(b.originCharge)}</td>
                <td>{fmtMoney(b.destinationCharge)}</td>
                <td><b>{fmtMoney(b.total)}</b></td>
                <td>{b.transitTime}</td>
                <td>{b.validity}</td>
                <td>{fmtTime(b.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {rfq.status === 'ACTIVE' && <BidForm rfqId={id} onDone={load} />}

      <h2>Activity log</h2>
      {activity.length === 0 ? <p className="muted">No activity yet.</p> : (
        <ul className="log">
          {[...activity].reverse().map((a) => (
            <li key={a.id} className={a.eventType}>
              <span className="muted">{fmtTime(a.createdAt)}</span>{' '}
              {a.eventType === 'BID_PLACED' ? (
                <>Bid: {a.reason}</>
              ) : (
                <>
                  <b>Extended</b> {fmtTime(a.oldClose)} → {fmtTime(a.newClose)}. Reason: {a.reason}
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function BidForm({ rfqId, onDone }) {
  const [form, setForm] = useState(EMPTY_BID)
  const [message, setMessage] = useState(null)
  const [busy, setBusy] = useState(false)

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      const res = await api.submitBid(rfqId, {
        supplierName: form.supplierName,
        freightCharge: Number(form.freightCharge),
        originCharge: Number(form.originCharge),
        destinationCharge: Number(form.destinationCharge),
        transitTime: Number(form.transitTime),
        validity: Number(form.validity),
      })
      setMessage({
        ok: true,
        text: `Bid accepted: ${res.supplierName} is ${res.rank} at ${fmtMoney(res.total)}.` +
          (res.extended ? ` Auction extended to ${fmtTime(res.bidCloseAt)}.` : ''),
      })
      setForm({ ...EMPTY_BID, supplierName: form.supplierName })
      onDone()
    } catch (err) {
      setMessage({ ok: false, text: err.message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card" onSubmit={submit}>
      <h2>Submit a quote</h2>
      <div className="form-grid">
        <label>Carrier name<input required value={form.supplierName} onChange={set('supplierName')} /></label>
        <label>Freight charges<input required type="number" min="0" step="0.01" value={form.freightCharge} onChange={set('freightCharge')} /></label>
        <label>Origin charges<input required type="number" min="0" step="0.01" value={form.originCharge} onChange={set('originCharge')} /></label>
        <label>Destination charges<input required type="number" min="0" step="0.01" value={form.destinationCharge} onChange={set('destinationCharge')} /></label>
        <label>Transit time (days)<input required type="number" min="1" value={form.transitTime} onChange={set('transitTime')} /></label>
        <label>Quote validity (days)<input required type="number" min="1" value={form.validity} onChange={set('validity')} /></label>
      </div>
      <button disabled={busy}>{busy ? 'Submitting…' : 'Submit bid'}</button>
      {message && <p className={message.ok ? 'success' : 'error'}>{message.text}</p>}
    </form>
  )
}
