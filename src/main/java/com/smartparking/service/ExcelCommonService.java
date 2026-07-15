package com.smartparking.service;

import com.smartparking.repository.BaseRepository;
import com.smartparking.util.ExcelUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
public class ExcelCommonService {

    /**
     * 通用导入入库
     * 支持Excel带ID（空=自增，填值=指定主键）
     */
    public <T, ID> int importData(MultipartFile file, Class<T> clazz, BaseRepository<T, ID> repository) throws Exception {
        List<T> entityList = ExcelUtil.importExcel(file, clazz);
        try {
            repository.saveAll(entityList);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("导入失败：存在重复ID/唯一字段冲突，请检查Excel数据");
        }
        return entityList.size();
    }

    /**
     * 通用导出全表数据
     */
    public <T, ID> byte[] exportAllData(BaseRepository<T, ID> repository, Class<T> clazz) throws Exception {
        List<T> allData = repository.findAll();
        return ExcelUtil.exportExcel(allData, clazz);
    }

    /**
     * 启动自动导入专用
     */
    public <T, ID> int importDataByStream(InputStream inputStream, Class<T> clazz, BaseRepository<T, ID> repository) throws Exception {
        List<T> entityList = ExcelUtil.importExcelByStream(inputStream, clazz);
        try {
            repository.saveAll(entityList);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("导入失败：存在重复ID/唯一字段冲突，请检查Excel数据");
        }
        return entityList.size();
    }
}