import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { SpikeManager } from './SpikeManager'
import './index.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <SpikeManager />
  </StrictMode>,
)
