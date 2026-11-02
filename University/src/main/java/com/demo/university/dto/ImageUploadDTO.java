package com.demo.university.dto;

/** 图片上传入参：前端把图片转成 data:image/xxx;base64,... 直接提交，省掉 multipart 依赖 */
public record ImageUploadDTO(String dataUrl) {
}
