/**
 * Phase 7 — Gate-In Management (HaBai screen).
 * fetchGateInRecords(): GET /admin/gate-in?page=&size=
 *
 * Backend GateInReceiptResponse fields:
 *   gateInId, containerId, voyageId, voyageNo,
 *   gateInTime, createdById, createdByUsername, operatorName, note,
 *   cargoTypeName, containerTypeName,
 *   yardName, zoneName,
 *   blockName, rowNo, bayNo, tier
 */
import { apiFetch } from './apiClient';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Rec = Record<string, any>;

export interface GateInRecord {
  id: number;
  containerCode: string;     // containerId field from backend
  voyageNo: string;
  cargoType: string;         // cargoTypeName
  containerType: string;     // containerTypeName (e.g. "20ft", "40ft")
  yardName: string;
  zoneName: string;
  blockName: string;
  rowNo: number;
  bayNo: number;
  tier: number;
  gateInTime: string;
  operator: string;          // operatorName / createdByUsername
  note: string;
}

export interface PageResult<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
}

function formatDateTime(raw: string): string {
  if (!raw) return '—';
  const d = new Date(raw);
  if (isNaN(d.getTime())) return raw;
  const dd = d.getDate().toString().padStart(2, '0');
  const mm = (d.getMonth() + 1).toString().padStart(2, '0');
  const hh = d.getHours().toString().padStart(2, '0');
  const mi = d.getMinutes().toString().padStart(2, '0');
  return `${dd}/${mm}/${d.getFullYear()} ${hh}:${mi}`;
}

export async function fetchGateInRecords(page: number, size = 20): Promise<PageResult<GateInRecord>> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sortBy: 'gateInTime',
    direction: 'desc',
  });
  const res = await apiFetch(`/admin/gate-in?${params}`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);

  const json: Rec = await res.json();
  // Backend wraps in { data: { content: [], totalPages: N, totalElements: N } }
  const data: Rec = json.data ?? json;
  const content: Rec[] = Array.isArray(data)
    ? data
    : Array.isArray(data.content) ? data.content : [];

  return {
    content: content.map((r: Rec) => ({
      id:            Number(r.gateInId   ?? r.id ?? 0),
      containerCode: String(r.containerId ?? ''),
      voyageNo:      String(r.voyageNo   ?? (r.voyageId ? `#${r.voyageId}` : '')),
      // Backend sets cargoTypeName via MapStruct from container.cargoType.cargoTypeName
      cargoType:     String(r.cargoTypeName     ?? ''),
      // Backend sets containerTypeName via MapStruct from container.containerType.containerTypeName
      containerType: String(r.containerTypeName ?? ''),
      // Set by controller after position lookup
      yardName:      String(r.yardName   ?? ''),
      zoneName:      String(r.zoneName   ?? ''),
      blockName:     String(r.blockName  ?? ''),
      rowNo:         Number(r.rowNo      ?? 0),
      bayNo:         Number(r.bayNo      ?? 0),
      tier:          Number(r.tier       ?? 0),
      gateInTime:    formatDateTime(String(r.gateInTime ?? '')),
      operator:      String(r.operatorName ?? r.createdByUsername ?? ''),
      note:          String(r.note ?? ''),
    })),
    totalPages:    Number(data.totalPages    ?? 1),
    totalElements: Number(data.totalElements ?? content.length),
  };
}
