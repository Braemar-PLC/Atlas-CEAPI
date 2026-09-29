import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { SignInGate } from '@/components/SignInGate'

import "@/ag-grid-setup";  // ADD THIS FIRST, BEFORE ANY AG GRID COMPONENTS


createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SignInGate>
      <App />
    </SignInGate>
  </StrictMode>,
)
