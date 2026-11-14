import { useEffect } from 'react'

// Runs `fn` now and then every `ms` milliseconds; stops when the page unmounts.
export default function usePolling(fn, ms, deps) {
  useEffect(() => {
    fn()
    const id = setInterval(fn, ms)
    return () => clearInterval(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)
}
