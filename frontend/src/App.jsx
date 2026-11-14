import { useEffect, useState } from 'react'
import RfqList from './pages/RfqList.jsx'
import RfqDetails from './pages/RfqDetails.jsx'
import CreateRfq from './pages/CreateRfq.jsx'
import './App.css'

// Tiny hash router: #/ (list), #/new (create), #/rfq/5 (details). No extra library needed.
function useHashRoute() {
  const [hash, setHash] = useState(window.location.hash || '#/')
  useEffect(() => {
    const onChange = () => setHash(window.location.hash || '#/')
    window.addEventListener('hashchange', onChange)
    return () => window.removeEventListener('hashchange', onChange)
  }, [])
  return hash
}

export default function App() {
  const hash = useHashRoute()
  const detailsMatch = hash.match(/^#\/rfq\/(\d+)$/)

  let page
  if (hash === '#/new') page = <CreateRfq />
  else if (detailsMatch) page = <RfqDetails id={detailsMatch[1]} />
  else page = <RfqList />

  return (
    <div className="container">
      <header>
        <a href="#/" className="brand">British Auction RFQ</a>
        <nav>
          <a href="#/">Auctions</a>
          <a href="#/new" className="btn">+ New RFQ</a>
        </nav>
      </header>
      {page}
    </div>
  )
}
