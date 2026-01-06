import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import GlobalProgressTracker from './GlobalProgressTracker';
import './Layout.css';

function Layout() {
  return (
    <div className="layout">
      <Sidebar />
      <main className="main-content">
        <Outlet />
      </main>
      {/* 전역 플로팅 진행도 트래커 */}
      <GlobalProgressTracker />
    </div>
  );
}

export default Layout;
