import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './pages/app/AppLayout'
import { BoardPage } from './pages/app/BoardPage'
import { NewRequestPage } from './pages/app/NewRequestPage'
import { ProjectsPage } from './pages/app/ProjectsPage'
import { RequestDetailPage } from './pages/app/RequestDetailPage'
import { LoginPage, RegisterPage } from './pages/auth/AuthPages'
import { PortalHomePage } from './pages/portal/PortalHomePage'
import { PortalLayout } from './pages/portal/PortalLayout'
import { PortalNewRequestPage } from './pages/portal/PortalNewRequestPage'
import { PortalRequestPage } from './pages/portal/PortalRequestPage'

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/app" replace />} />
      <Route path="/entrar" element={<LoginPage />} />
      <Route path="/criar-conta" element={<RegisterPage />} />
      <Route path="/app" element={<AppLayout />}>
        <Route index element={<BoardPage />} />
        <Route path="projetos" element={<ProjectsPage />} />
        <Route path="solicitacoes/nova" element={<NewRequestPage />} />
        <Route path="solicitacoes/:id" element={<RequestDetailPage />} />
      </Route>
      <Route path="/portal/:token" element={<PortalLayout />}>
        <Route index element={<PortalHomePage />} />
        <Route path="solicitacoes/nova" element={<PortalNewRequestPage />} />
        <Route path="solicitacoes/:id" element={<PortalRequestPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  )
}
