/**
 * 图片地址构建：DB 只存 objectKey，换域名只改 config/env.js
 * @author come-often
 * @date 2026-11-08
 */
import { FILE_BASE_URL } from '../config/env';

export const buildImageUrl = (objectKey) => {
  if (!objectKey) {
    return '';
  }
  return `${FILE_BASE_URL}?objectKey=${encodeURIComponent(objectKey)}`;
};
