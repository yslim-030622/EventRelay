import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { Layout } from './components/Layout';
import { DeadLettersPage } from './pages/DeadLettersPage';
import { EventDetailPage } from './pages/EventDetailPage';
import { EventsPage } from './pages/EventsPage';
import { MetricsPage } from './pages/MetricsPage';
import { SourcesPage } from './pages/SourcesPage';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <EventsPage /> },
      { path: 'events/:id', element: <EventDetailPage /> },
      { path: 'metrics', element: <MetricsPage /> },
      { path: 'sources', element: <SourcesPage /> },
      { path: 'dead-letters', element: <DeadLettersPage /> }
    ]
  }
]);

export default function App() {
  return <RouterProvider router={router} />;
}
