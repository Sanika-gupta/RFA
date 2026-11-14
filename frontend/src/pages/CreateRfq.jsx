import { useState } from 'react'
import { api } from '../api.js'
import { localInputToIso, TRIGGER_LABELS } from '../format.js'

export default function CreateRfq() {
  const [form, setForm] = useState({
    name: '', startAt: '', bidCloseAt: '', forcedCloseAt: '', pickupDate: '',
    extensionWindowMinutes: 10, extensionMinutes: 5, triggerType: 'BID_IN_WINDOW',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const created = await api.createRfq({
        name: form.name,
        startAt: localInputToIso(form.startAt),
        bidCloseAt: localInputToIso(form.bidCloseAt),
        forcedCloseAt: localInputToIso(form.forcedCloseAt),
        pickupDate: localInputToIso(form.pickupDate),
        extensionWindowMinutes: Number(form.extensionWindowMinutes),
        extensionMinutes: Number(form.extensionMinutes),
        triggerType: form.triggerType,
      })
      window.location.hash = `#/rfq/${created.id}`
    } catch (err) {
      setError(err.message)   // the backend's validation message, e.g. forced close too early
    } finally {
      setBusy(false)
    }
  }

  return (
    <section>
      <h1>Create RFQ (British Auction)</h1>
      <form className="card" onSubmit={submit}>
        <div className="form-grid">
          <label>RFQ name / reference<input required value={form.name} onChange={set('name')} /></label>
          <label>Pickup / service date<input type="datetime-local" value={form.pickupDate} onChange={set('pickupDate')} /></label>
          <label>Bid start<input required type="datetime-local" value={form.startAt} onChange={set('startAt')} /></label>
          <label>Bid close<input required type="datetime-local" value={form.bidCloseAt} onChange={set('bidCloseAt')} /></label>
          <label>Forced bid close<input required type="datetime-local" value={form.forcedCloseAt} onChange={set('forcedCloseAt')} /></label>
          <label>Trigger window X (min)<input required type="number" min="1" value={form.extensionWindowMinutes} onChange={set('extensionWindowMinutes')} /></label>
          <label>Extension Y (min)<input required type="number" min="1" value={form.extensionMinutes} onChange={set('extensionMinutes')} /></label>
          <label>Extension trigger
            <select value={form.triggerType} onChange={set('triggerType')}>
              {Object.entries(TRIGGER_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
        </div>
        <p className="muted">Forced close must be later than bid close. The auction never extends past it.</p>
        <button disabled={busy}>{busy ? 'Creating…' : 'Create RFQ'}</button>
        {error && <p className="error">{error}</p>}
      </form>
    </section>
  )
}
