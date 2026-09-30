import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { SignInGate } from '@/components/SignInGate'
import { PerformanceRecordsClearIntervalMs, clearPerformanceRecordsEvery } from '@/application/dev/performance-records'

import "@/ag-grid-setup";  // ADD THIS FIRST, BEFORE ANY AG GRID COMPONENTS

// The development build's React fills the browser's performance timeline without end (performance-records.ts).
if (import.meta.env.DEV) {
  clearPerformanceRecordsEvery(PerformanceRecordsClearIntervalMs);
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SignInGate>
      <App />
    </SignInGate>
  </StrictMode>,
)
