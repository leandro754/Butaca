import { useState } from "react";
import { Routes, Route } from "react-router-dom";
import AppShell from "./components/layout/AppShell";
import HomePage from "./pages/HomePage";
import SearchPage from "./pages/SearchPage";
import EventDetailPage from "./pages/EventDetailPage";
import CheckoutPage from "./pages/CheckoutPage";
import ConfirmationPage from "./pages/ConfirmationPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import DashboardPage from "./pages/DashboardPage";
import OrganizerEventPage from "./pages/OrganizerEventPage";
import {
  clearAuthenticatedUser,
  getAuthenticatedUser,
  saveAuthenticatedUser,
} from "./utils/authStorage";

function App() {
  const [currentUser, setCurrentUser] = useState(() => getAuthenticatedUser());

  function handleLogin(user) {
    saveAuthenticatedUser(user);
    setCurrentUser(user);
  }

  function handleLogout() {
    clearAuthenticatedUser();
    setCurrentUser(null);
  }

  return (
    <AppShell currentUser={currentUser} onLogout={handleLogout}>
      <Routes>
        <Route path="/" element={<HomePage currentUser={currentUser} />} />
        <Route path="/buscar" element={<SearchPage />} />
        <Route path="/eventos/:eventId" element={<EventDetailPage />} />
        <Route
          path="/checkout"
          element={<CheckoutPage currentUser={currentUser} />}
        />
        <Route path="/confirmacion" element={<ConfirmationPage />} />
        <Route
          path="/login"
          element={
            <LoginPage currentUser={currentUser} onLogin={handleLogin} />
          }
        />
        <Route path="/registro" element={<RegisterPage />} />
        <Route
          path="/dashboard"
          element={<DashboardPage currentUser={currentUser} />}
        />
        <Route
          path="/dashboard/eventos/:eventId"
          element={<OrganizerEventPage currentUser={currentUser} />}
        />
      </Routes>
    </AppShell>
  );
}

export default App;
