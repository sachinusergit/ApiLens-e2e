import { ChangeEvent, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, getAccessToken, setAccessToken } from '../../api/client';
import type { FileDto } from './types';
import Slider from './components/Slider';
import FileFilters from './components/FileFilters';
import SearchBox from './components/SearchBox';
import Banner from './components/Banner';
import ConfirmDialog from './components/ConfirmDialog';
import Toast from './components/Toast';
import type { ToastData } from './components/Toast';
import PromoModal from './components/PromoModal';

export default function FileUploader() {
  const nav = useNavigate();
  const [files, setFiles] = useState<File[]>([]);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [list, setList] = useState<FileDto[]>([]);
  const [filtered, setFiltered] = useState<FileDto[] | null>(null);
  const [filterLabel, setFilterLabel] = useState('');
  const [loading, setLoading] = useState(false);
  const [confirmTarget, setConfirmTarget] = useState<FileDto | null>(null);
  const [confirmBusy, setConfirmBusy] = useState(false);
  const [toast, setToast] = useState<ToastData | null>(null);

  const visible = filtered ?? list;

  useEffect(() => {
    if (!getAccessToken()) {
      nav('/login');
      return;
    }
    refreshList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!toast) return;
    const handle = window.setTimeout(() => setToast(null), 5000);
    return () => window.clearTimeout(handle);
  }, [toast]);

  async function refreshList() {
    try {
      const res = await api.get<FileDto[]>('/files');
      setList(res.data);
    } catch (err: any) {
      if (err.response?.status === 401) nav('/login');
    }
  }

  async function showToast(type: ToastData['type'], message: string) {
    setToast({ type, message });
    try {
      await api.post('/notifications', { type, message });
    } catch {
      /* notification persistence is best-effort */
    }
  }

  function onFileChange(e: ChangeEvent<HTMLInputElement>) {
    if (e.target.files) setFiles(Array.from(e.target.files));
  }

  async function upload() {
    if (files.length === 0) return;
    const count = files.length;
    setError('');
    setSuccess('');
    setLoading(true);
    setProgress(0);
    try {
      for (const file of files) {
        const fd = new FormData();
        fd.append('file', file);
        await api.post('/files/upload', fd, {
          headers: { 'Content-Type': 'multipart/form-data' },
          onUploadProgress: (e) => {
            if (e.total) setProgress(Math.round((e.loaded * 100) / e.total));
          },
        });
      }
      setSuccess(`Uploaded ${count} file(s)`);
      setFiles([]);
      setProgress(0);
      setFiltered(null);
      setFilterLabel('');
      await refreshList();
      await showToast('success', `Uploaded ${count} file(s)`);
    } catch (err: any) {
      const message = err.friendlyMessage || 'Upload failed';
      setError(message);
      await showToast('error', message);
    } finally {
      setLoading(false);
    }
  }

  async function download(file: FileDto) {
    try {
      const res = await api.get<Blob>(`/files/${file.id}`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = file.originalName;
      a.click();
      URL.revokeObjectURL(url);
      await showToast('info', `Downloaded ${file.originalName}`);
    } catch (err: any) {
      await showToast('error', err.friendlyMessage || 'Download failed');
    }
  }

  async function decide(choice: 'YES' | 'NO') {
    const target = confirmTarget;
    if (!target) return;

    setConfirmBusy(true);
    try {
      const res = await api.post<{ message: string }>('/confirmations', {
        action: 'DELETE_FILE',
        choice,
        note: target.originalName,
      });

      if (choice === 'YES') {
        await api.delete(`/files/${target.id}`);
        setFiltered(null);
        setFilterLabel('');
        await refreshList();
      }

      await showToast(choice === 'YES' ? 'success' : 'info', res.data.message);
    } catch (err: any) {
      await showToast('error', err.friendlyMessage || 'Action failed');
    } finally {
      setConfirmBusy(false);
      setConfirmTarget(null);
    }
  }

  function logout() {
    setAccessToken(null);
    nav('/login');
  }

  return (
    <div className="container">
      <header>
        <h1>Files</h1>
        <div>
          <Link to="/profile" className="nav-link">Profile</Link>
          {filtered && (
            <button
              className="secondary"
              onClick={() => {
                setFiltered(null);
                setFilterLabel('');
              }}
            >
              Show all
            </button>
          )}
          <button onClick={logout}>Logout</button>
        </div>
      </header>

      <Banner />

      <div className="two-col">
        <div className="card">
          <SearchBox
            onPick={(file) => {
              setFiltered([file]);
              setFilterLabel('Search result');
            }}
          />
        </div>
        <div className="card">
          <FileFilters
            onResults={(files, label) => {
              setFiltered(files);
              setFilterLabel(files ? (label ?? '') : '');
            }}
          />
        </div>
      </div>

      <div className="card">
        <Slider />
      </div>

      <div className="card">
        <h3>Upload</h3>
        <input type="file" multiple onChange={onFileChange} />
        <button onClick={upload} disabled={files.length === 0 || loading}>
          {loading ? 'Uploading...' : 'Upload'}
        </button>
        {progress > 0 && <progress value={progress} max={100} />}
        {loading && (
          <div className="loading-box" role="status" aria-live="polite">
            <span className="spinner" />
            <span>Uploading {files.length} file(s)...</span>
          </div>
        )}
        {error && <div className="error">{error}</div>}
        {success && <div className="success">{success}</div>}
        <p className="hint">
          DevTools → Network → <code>POST /api/files/upload</code> with Bearer + multipart payload.
        </p>
      </div>

      <div className="card">
        <h3>
          {filtered
            ? `Filtered files${filterLabel ? ` - ${filterLabel}` : ''} (${visible.length})`
            : `My Files (${visible.length})`}
        </h3>
        {visible.length === 0 && <p>No files yet.</p>}
        {visible.map((file) => (
          <div key={file.id} className="file-item">
            <div>
              <div>
                <b>{file.originalName}</b>
              </div>
              <small>
                {file.contentType} · {file.sizeBytes} bytes · {file.status}
              </small>
            </div>
            <div>
              <button onClick={() => download(file)}>Download</button>
              <button className="danger" onClick={() => setConfirmTarget(file)}>
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>

      <ConfirmDialog
        open={confirmTarget !== null}
        title="Delete file?"
        message={
          confirmTarget
            ? `Delete "${confirmTarget.originalName}"? Your choice (Yes/No) is written to the confirmations table.`
            : ''
        }
        busy={confirmBusy}
        onDecision={decide}
      />

      <Toast toast={toast} onClose={() => setToast(null)} />
      <PromoModal intervalMs={120000} />
    </div>
  );
}
