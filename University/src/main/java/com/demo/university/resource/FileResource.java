package com.demo.university.resource;

import com.demo.university.common.ApiException;
import com.demo.university.common.Result;
import com.demo.university.config.DataSourceProvider;
import com.demo.university.dto.ImageUploadDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 图片上传/读取。
 * 前端把图片压缩后转成 data:image/xxx;base64,... 提交，避免引入 multipart 依赖；
 * 文件落盘到 upload.dir（默认 catalina.base/uploads），重新部署 WAR 不会丢图。
 */
@Path("/files")
public class FileResource {

    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final long MAX_SIZE = 2L * 1024 * 1024;
    private static final Pattern FILE_NAME =
            Pattern.compile("^[A-Za-z0-9]{8,40}\\.(png|jpg|jpeg|gif|webp)$");

    @POST
    @Path("/image")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Result<Map<String, String>> upload(ImageUploadDTO dto) {
        String dataUrl = dto == null ? null : dto.dataUrl();
        if (dataUrl == null || !dataUrl.startsWith("data:image/")) {
            throw ApiException.badRequest("请上传图片文件");
        }
        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            throw ApiException.badRequest("图片格式不正确");
        }
        String meta = dataUrl.substring(5, comma).toLowerCase(Locale.ROOT);
        if (!meta.endsWith(";base64")) {
            throw ApiException.badRequest("图片需使用 base64 编码");
        }
        // meta 示例: "image/jpeg;base64" → ext 应为 "jpeg"
        String ext = meta.substring(0, meta.indexOf(';'));
        if (ext.startsWith("image/")) {
            ext = ext.substring(6); // 去掉 "image/" 前缀
        }
        if ("jpeg".equals(ext)) {
            ext = "jpg";
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw ApiException.badRequest("仅支持 png / jpg / gif / webp 格式");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("图片数据损坏，请重新选择");
        }
        if (bytes.length == 0) {
            throw ApiException.badRequest("图片内容为空");
        }
        if (bytes.length > MAX_SIZE) {
            throw ApiException.badRequest("图片不能超过 2MB");
        }

        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.write(uploadDir().resolve(name), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("图片保存失败", e);
        }
        Map<String, String> data = new HashMap<>();
        data.put("url", "api/files/" + name);
        return Result.ok(data);
    }

    @GET
    @Path("/{name}")
    public Response load(@PathParam("name") String name) throws IOException {
        if (name == null || !FILE_NAME.matcher(name).matches()) {
            throw ApiException.notFound("文件不存在");
        }
        java.nio.file.Path dir = uploadDir();
        java.nio.file.Path file = dir.resolve(name).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            throw ApiException.notFound("文件不存在");
        }
        String suffix = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String contentType = switch (suffix) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
        return Response.ok(Files.readAllBytes(file), contentType)
                .header("Cache-Control", "public, max-age=86400")
                .build();
    }

    private java.nio.file.Path uploadDir() {
        String configured = DataSourceProvider.getProperty("upload.dir", "");
        String dir = configured;
        if (dir == null || dir.isBlank()) {
            String base = System.getProperty("catalina.base", System.getProperty("user.dir"));
            dir = base + "/uploads";
        }
        java.nio.file.Path path = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + path, e);
        }
        return path;
    }
}
