import { Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from './features/auth/LoginPage';
import SignupPage from './features/auth/SignupPage';
import FileUploader from './features/files/FileUploader';
import ProfilePage from './features/profile/ProfilePage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/files" element={<FileUploader />} />
      <Route path="/profile" element={<ProfilePage />} />
    </Routes>
  );
}
