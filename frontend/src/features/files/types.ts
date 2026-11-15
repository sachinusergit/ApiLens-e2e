export interface FileDto {
  id: string;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  checksumSha256: string;
  status: string;
  createdAt: string;
}
