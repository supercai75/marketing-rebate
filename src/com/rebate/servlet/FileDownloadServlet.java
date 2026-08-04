package com.rebate.servlet;

import com.rebate.util.FileUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Map;

/**
 * 通用文件下载
 */
public class FileDownloadServlet extends BaseServlet {

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, com.rebate.model.UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String path = req.getParameter("path");
        if (path == null || path.isEmpty()) { ResponseUtil.fail(resp, "path 必填"); return; }
        File f = FileUtil.toFile(path);
        if (!f.exists() || !f.isFile()) { ResponseUtil.fail(resp, "文件不存在"); return; }
        resp.setContentType("application/octet-stream");
        resp.setContentLengthLong(f.length());
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(f.getName(), "UTF-8") + "\"");
        try (FileInputStream in = new FileInputStream(f); OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }
    }
}
