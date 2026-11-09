/**
 * 图片上传：wx.uploadFile 只能 POST 到后端转存，DB 只落 objectKey
 * @author come-often
 * @date 2026-11-09
 */
import { API_BASE_URL } from '../config/env';
import { getToken, redirectToLogin } from './request';

const UPLOAD_PATH = '/file/upload';
const HTTP_OK = 200;
const HTTP_UNAUTHORIZED = 401;
const SUCCESS_CODE = 0;
const COMPRESS_QUALITY = 80;

/** 上传前先压一次：服务端不做图片处理，评价图体积由端上控制。 */
const compressImage = async (filePath) => {
  try {
    const result = await wx.compressImage({ src: filePath, quality: COMPRESS_QUALITY });
    return result?.tempFilePath ?? filePath;
  } catch (error) {
    return filePath;
  }
};

const rawUpload = (filePath, prefix, token) => new Promise((resolve, reject) => {
  wx.uploadFile({
    url: `${API_BASE_URL}${UPLOAD_PATH}`,
    filePath,
    name: 'file',
    formData: { prefix },
    header: token ? { Authorization: `Bearer ${token}` } : {},
    success: resolve,
    fail: reject,
  });
});

const parseBody = (raw) => {
  try {
    return JSON.parse(raw ?? '{}');
  } catch (error) {
    return {};
  }
};

/** 上传评价图，返回 objectKey；401 直接回登录页，不再静默续登。 */
export const uploadReviewImage = async (filePath) => {
  const compressed = await compressImage(filePath);
  const response = await rawUpload(compressed, 'review', getToken());
  if (response.statusCode === HTTP_UNAUTHORIZED) {
    redirectToLogin();
    throw new Error('登录已失效，请重新登录');
  }
  if (response.statusCode !== HTTP_OK) {
    throw new Error(`图片上传失败：${response.statusCode}`);
  }
  const body = parseBody(response.data);
  if (body.code !== SUCCESS_CODE) {
    throw new Error(body.message ?? '图片上传失败');
  }
  return body.data.objectKey;
};
