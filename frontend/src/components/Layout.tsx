import { NavLink, Outlet } from 'react-router-dom';

const navItems = [
  { to: '/', label: 'Events' },
  { to: '/metrics', label: 'Metrics' },
  { to: '/sources', label: 'Sources' },
  { to: '/dead-letters', label: 'Dead Letters' }
];

export function Layout() {
  return (
    <div className="shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">EventRelay</p>
          <h1>Webhook Operations Console</h1>
          <p className="sidebar-copy">
            Skeleton dashboard for event intake, delivery health, and replay workflows.
          </p>
        </div>
        <nav className="nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? 'nav-link nav-link-active' : 'nav-link')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
