package com.rebate.util;

import com.rebate.config.AppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * 文件上传工具
 */
public class FileUtil {

    public static String getBaseDir() {
        String d = AppConfig.get("upload.base.dir", "/opt/rebate/uploads");
        File f = new File(d);
        if (!f.exists()) f.mkdirs();
        return f.getAbsolutePath();
    }

    /**
     * 保存文件到 baseDir/subDir，返回相对 baseDir 的路径
     */
    public static String save(InputStream in, String subDir, String originalName) throws Exception {
        File dir = new File(getBaseDir(), subDir);
        if (!dir.exists()) dir.mkdirs();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        String fname = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ext;
        File out = new File(dir, fname);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
        }
        return subDir + "/" + fname;
    }

    public static File toFile(String relativePath) {
        return new File(getBaseDir(), relativePath);
    }
}
