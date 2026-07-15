package com.smartparking.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtil {

    /**
     * 通用Excel导入：文件转实体集合
     */
    public static <T> List<T> importExcel(MultipartFile file, Class<T> clazz) throws Exception {
        List<T> dataList = new ArrayList<>();
        InputStream inputStream = file.getInputStream();
        EasyExcel.read(inputStream, clazz, new GenericExcelListener<>(dataList))
                .sheet()
                .doRead();
        inputStream.close();
        return dataList;
    }

    /**
     * 通用Excel导出：实体列表生成Excel字节流
     */
    public static <T> byte[] exportExcel(List<T> dataList, Class<T> clazz) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            EasyExcel.write(bos, clazz)
                    .sheet("数据表")
                    .doWrite(dataList);
            return bos.toByteArray();
        } catch (RuntimeException e) {
            throw new Exception("Excel导出失败", e);
        }
    }

    /**
     * 读取resources内置模板文件，返回字节流用于浏览器下载
     * @param templatePath 相对路径：excel-template/resident_template.xlsx
     */
    public static byte[] getTemplateFile(String templatePath) throws IOException {
        try (InputStream inputStream = ExcelUtil.class.getClassLoader().getResourceAsStream(templatePath);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new FileNotFoundException("模板文件不存在：" + templatePath);
            }
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }

    /**
     * 泛型通用读取监听器
     */
    static class GenericExcelListener<T> extends AnalysisEventListener<T> {
        private final List<T> cache;
        public GenericExcelListener(List<T> cache) {
            this.cache = cache;
        }
        @Override
        public void invoke(T entity, AnalysisContext context) {
            cache.add(entity);
        }
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}
    }
}
