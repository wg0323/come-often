const FILE_BASE = '/api/public/file';

/**
 * DB 只存 objectKey，展示地址统一在这里拼；换域名只改这一处。
 */
export const buildImageUrl = (objectKey: string | null | undefined): string => {
  if (!objectKey) {
    return '';
  }
  return `${FILE_BASE}?objectKey=${encodeURIComponent(objectKey)}`;
};
