// Server sends UTC ISO strings; show them in the viewer's local time.
export const fmtTime = (iso) => (iso ? new Date(iso).toLocaleString() : '-')

export const fmtMoney = (n) =>
  n == null ? '-' : Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export const TRIGGER_LABELS = {
  BID_IN_WINDOW: 'Bid received in last X min',
  ANY_RANK_CHANGE: 'Any supplier rank change in last X min',
  L1_CHANGE: 'Lowest bidder (L1) change in last X min',
}

export const STATUS_LABELS = {
  UPCOMING: 'Upcoming',
  ACTIVE: 'Active',
  CLOSED: 'Closed',
  FORCE_CLOSED: 'Force Closed',
}

// <input type="datetime-local"> gives local time with no zone; convert to UTC ISO for the API.
export const localInputToIso = (value) => (value ? new Date(value).toISOString() : null)
