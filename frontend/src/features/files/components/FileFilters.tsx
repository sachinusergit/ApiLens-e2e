import { useEffect, useRef, useState } from 'react';
import { api } from '../../../api/client';
import type { FileDto } from '../types';
import Spinner from './Spinner';

const CONTENT_TYPES = [
  { label: 'Text', value: 'text/plain' },
  { label: 'PNG Image', value: 'image/png' },
  { label: 'JPEG Image', value: 'image/jpeg' },
  { label: 'PDF', value: 'application/pdf' },
  { label: 'Markdown', value: 'text/markdown' },
];

const STATUSES = [
  { label: 'Available', value: 'AVAILABLE' },
  { label: 'Deleted', value: 'DELETED' },
  { label: 'Infected', value: 'INFECTED' },
];

/** Keep the loader visible long enough for the user to notice it. */
const MIN_LOADER_MS = 5000;

type Filter =
  | { kind: 'none' }
  | { kind: 'contentType'; value: string }
  | { kind: 'status'; value: string }
  | { kind: 'dateRange'; from: string; to: string };

interface SearchMeta {
  totalCount: number;
  filterApplied: string;
  executionTimeMs: number;
  orgId: string;
}

interface SearchSummary {
  totalFiles: number;
  totalSizeHuman: string;
  byCategory: Record<string, number>;
}

interface SearchResponse {
  meta: SearchMeta;
  summary: SearchSummary;
  files: FileDto[];
}

interface Props {
  onResults: (files: FileDto[] | null, label?: string) => void;
}

function describeFilter(filter: Filter): string {
  if (filter.kind === 'contentType') {
    const found = CONTENT_TYPES.find((type) => type.value === filter.value);
    return `${found?.label ?? filter.value} (${filter.value})`;
  }
  if (filter.kind === 'status') {
    return `Status: ${filter.value}`;
  }
  if (filter.kind === 'dateRange') {
    return 'Date range';
  }
  return 'All files';
}

function queryOf(filter: Filter): string {
  const params = new URLSearchParams();
  if (filter.kind === 'contentType') {
    params.set('contentType', filter.value);
  } else if (filter.kind === 'status') {
    params.set('status', filter.value);
  } else if (filter.kind === 'dateRange') {
    if (filter.from) {
      params.set('dateFrom', new Date(`${filter.from}T00:00:00Z`).toISOString());
    }
    if (filter.to) {
      params.set('dateTo', new Date(`${filter.to}T23:59:59Z`).toISOString());
    }
  }
  return params.toString();
}

export default function FileFilters({ onResults }: Props) {
  const [activeFilter, setActiveFilter] = useState<Filter>({ kind: 'none' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [applied, setApplied] = useState(false);
  const [appliedCount, setAppliedCount] = useState(0);
  const [appliedLabel, setAppliedLabel] = useState('');
  const [lastQuery, setLastQuery] = useState('');
  const [lastResponse, setLastResponse] = useState<SearchResponse | null>(null);
  const requestId = useRef(0);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    return () => abortRef.current?.abort();
  }, []);

  async function callApi(filter: Filter) {
    const id = ++requestId.current;

    // Cancel any in-flight filter so only the latest checkbox can answer.
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const fallbackLabel = describeFilter(filter);
    const query = queryOf(filter);
    const url =
      filter.kind === 'none'
        ? '/files'
        : `/files/search${query ? `?${query}` : ''}`;

    setError('');
    setApplied(false);
    setAppliedLabel(fallbackLabel);
    setLastResponse(null);
    setLoading(true);
    setLastQuery(`/api${url}`);

    const started = Date.now();

    try {
      if (filter.kind === 'none') {
        console.log('[FileFilters] GET /api/files');
        const res = await api.get<FileDto[]>('/files', { signal: controller.signal });
        if (id !== requestId.current) {
          return;
        }
        setAppliedLabel('All files');
        setAppliedCount(res.data.length);
        setApplied(true);
        onResults(null, '');
      } else {
        console.log('[FileFilters] GET', `/api${url}`);
        const res = await api.get<SearchResponse>(url, { signal: controller.signal });
        if (id !== requestId.current) {
          return;
        }

        const envelope = res.data;
        const label = envelope.meta?.filterApplied || fallbackLabel;
        const files = envelope.files ?? [];

        console.log(
          '[FileFilters] Response for',
          label,
          '-',
          envelope.meta?.totalCount,
          'file(s)',
          envelope.summary,
        );

        setLastResponse(envelope);
        setAppliedLabel(label);
        setAppliedCount(envelope.meta?.totalCount ?? files.length);
        setApplied(true);
        onResults(files, label);
      }
    } catch (err: any) {
      if (err?.code === 'ERR_CANCELED' || err?.name === 'CanceledError') {
        return;
      }
      if (id !== requestId.current) {
        return;
      }
      console.error('[FileFilters] Error', err);
      setError(err.friendlyMessage || err.message || 'Filter failed');
      onResults(null, '');
    } finally {
      const remaining = MIN_LOADER_MS - (Date.now() - started);
      if (remaining > 0) {
        await new Promise((resolve) => window.setTimeout(resolve, remaining));
      }
      if (id === requestId.current) {
        setLoading(false);
      }
    }
  }

  function onContentTypeClick(value: string) {
    const next: Filter =
      activeFilter.kind === 'contentType' && activeFilter.value === value
        ? { kind: 'none' }
        : { kind: 'contentType', value };
    setActiveFilter(next);
    void callApi(next);
  }

  function onStatusClick(value: string) {
    const next: Filter =
      activeFilter.kind === 'status' && activeFilter.value === value
        ? { kind: 'none' }
        : { kind: 'status', value };
    setActiveFilter(next);
    void callApi(next);
  }

  function onDateChange(which: 'from' | 'to', value: string) {
    const current =
      activeFilter.kind === 'dateRange'
        ? activeFilter
        : { kind: 'dateRange' as const, from: '', to: '' };
    const next: Filter = {
      kind: 'dateRange',
      from: which === 'from' ? value : current.from,
      to: which === 'to' ? value : current.to,
    };
    setActiveFilter(next);
    void callApi(next);
  }

  function clearAll() {
    requestId.current += 1;
    abortRef.current?.abort();
    abortRef.current = null;
    setActiveFilter({ kind: 'none' });
    setError('');
    setApplied(false);
    setAppliedCount(0);
    setAppliedLabel('');
    setLastQuery('');
    setLastResponse(null);
    setLoading(false);
    onResults(null, '');
  }

  const isContentChecked = (v: string) =>
    activeFilter.kind === 'contentType' && activeFilter.value === v;

  const isStatusChecked = (v: string) =>
    activeFilter.kind === 'status' && activeFilter.value === v;

  const dateFrom = activeFilter.kind === 'dateRange' ? activeFilter.from : '';
  const dateTo = activeFilter.kind === 'dateRange' ? activeFilter.to : '';

  return (
    <div>
      <h3>Filter files (single-select)</h3>

      <div className="filter-group">
        <b>Content type</b>
        <div className="checkbox-row">
          {CONTENT_TYPES.map((type) => {
            const checked = isContentChecked(type.value);
            return (
              <label key={type.value} className="checkbox-item">
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => onContentTypeClick(type.value)}
                />
                {type.label}
                {checked && <small className="api-note">- active</small>}
              </label>
            );
          })}
        </div>
      </div>

      <div className="filter-group">
        <b>Status</b>
        <div className="checkbox-row">
          {STATUSES.map((status) => {
            const checked = isStatusChecked(status.value);
            return (
              <label key={status.value} className="checkbox-item">
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => onStatusClick(status.value)}
                />
                {status.label}
                {checked && <small className="api-note">- active</small>}
              </label>
            );
          })}
        </div>
      </div>

      <div className="date-row">
        <label>
          From
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => onDateChange('from', e.target.value)}
          />
        </label>
        <label>
          To
          <input
            type="date"
            value={dateTo}
            onChange={(e) => onDateChange('to', e.target.value)}
          />
        </label>
        <button className="secondary" onClick={clearAll}>
          Clear all filters
        </button>
      </div>

      {loading && (
        <div className="loading-box" role="status" aria-live="polite">
          <Spinner size={28} color="#2563eb" />
          <div>
            <div style={{ fontWeight: 'bold', color: '#1e40af' }}>
              Fetching results for {appliedLabel}...
            </div>
            <div style={{ fontSize: 12, color: '#6b7280' }}>
              {lastQuery || 'GET /api/files/search'}
            </div>
          </div>
        </div>
      )}

      {!loading && applied && (
        <div className="success">
          Filter applied - {appliedLabel} - {appliedCount} file(s)
          {lastResponse ? `, ${lastResponse.summary.totalSizeHuman}` : ''}. Check DevTools
          Network.
        </div>
      )}

      {lastResponse && (
        <details className="response-preview">
          <summary>
            Response preview - {appliedLabel} ({appliedCount} file(s))
          </summary>
          <pre>
            {JSON.stringify(
              {
                meta: lastResponse.meta,
                summary: lastResponse.summary,
                files: lastResponse.files.slice(0, 3),
              },
              null,
              2,
            )}
          </pre>
        </details>
      )}

      {error && <div className="error">{error}</div>}
      <p className="hint">
        DevTools -&gt; Network -&gt;{' '}
        <code>GET /api/files/search?contentType=...</code> returns{' '}
        <code>meta</code>, <code>summary</code> and <code>files</code>.
      </p>
    </div>
  );
}
