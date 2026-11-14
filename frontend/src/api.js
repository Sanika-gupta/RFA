// All calls to the Spring Boot API. The backend decides everything; the UI only displays.
const BASE = 'http://localhost:8080/api'

async function request(path, options = {}) {
  const res = await fetch(BASE + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(body?.error || `Request failed (${res.status})`)
  }
  return body
}

export const api = {
  listRfqs: () => request('/rfqs'),
  getRfq: (id) => request(`/rfqs/${id}`),
  createRfq: (data) => request('/rfqs', { method: 'POST', body: JSON.stringify(data) }),
  getBids: (id) => request(`/rfqs/${id}/bids`),
  getActivity: (id) => request(`/rfqs/${id}/activity`),
  submitBid: (id, data) => request(`/rfqs/${id}/bids`, { method: 'POST', body: JSON.stringify(data) }),
}
