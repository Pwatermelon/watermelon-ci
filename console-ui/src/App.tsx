import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './layout/AppShell'
import DashboardPage from './pages/DashboardPage'
import DocsPage from './pages/DocsPage'
import HostsPage from './pages/HostsPage'
import LandingPage, { AuthCallbackPage, RequireWorkspace } from './pages/LandingPage'
import PipelinesPage from './pages/PipelinesPage'
import ReposPage, { RepoEditorPage } from './pages/ReposPage'
import ServicesPage from './pages/ServicesPage'

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <RequireWorkspace>
      <AppShell>{children}</AppShell>
    </RequireWorkspace>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<AppShell><LandingPage /></AppShell>} />
      <Route path="/login" element={<Navigate to="/" replace />} />
      <Route path="/auth/callback" element={<AppShell><AuthCallbackPage /></AppShell>} />
      <Route path="/app" element={<Shell><DashboardPage /></Shell>} />
      <Route path="/app/repos" element={<Shell><ReposPage /></Shell>} />
      <Route path="/app/repos/:owner/:repo" element={<Shell><RepoEditorPage /></Shell>} />
      <Route path="/app/pipelines" element={<Shell><PipelinesPage /></Shell>} />
      <Route path="/app/hosts" element={<Shell><HostsPage /></Shell>} />
      <Route path="/app/services" element={<Shell><ServicesPage /></Shell>} />
      <Route path="/app/docs" element={<AppShell><DocsPage embedded /></AppShell>} />
      <Route path="/app/studio" element={<Navigate to="/app/pipelines" replace />} />
      <Route path="/app/ide" element={<Navigate to="/app/pipelines" replace />} />
      <Route path="/app/fleet" element={<Navigate to="/app/hosts" replace />} />
      <Route path="/app/projects" element={<Navigate to="/app" replace />} />
      <Route path="/docs/karbyz" element={<Navigate to="/app/docs" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
