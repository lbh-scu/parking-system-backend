package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.Resident;
import com.smartparking.repository.ResidentRepository;
import com.smartparking.service.ExcelCommonService;
import com.smartparking.service.ResidentService;
import com.smartparking.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/residents")
public class ResidentController {
    @Autowired
    private ExcelCommonService excelCommonService;
    @Autowired
    private ResidentRepository residentRepository;

    /**
     * 1. 下载内置业主Excel模板（浏览器自动弹出下载栏）
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        String templatePath = "excel-template/resident_template.xlsx";
        byte[] bytes = ExcelUtil.getTemplateFile(templatePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=业主导入模板.xlsx")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(bytes);
    }

    /**
     * 2. 上传Excel批量导入业主（支持ID列）
     */
    @PostMapping("/import")
    public ApiResponse<String> importExcel(@RequestParam MultipartFile file) {
        try {
            int count = excelCommonService.importData(file, Resident.class, residentRepository);
            return ApiResponse.success("导入成功，共" + count + "条数据", null);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 3. 导出全部业主数据，浏览器下载Excel
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() throws Exception {
        byte[] bytes = excelCommonService.exportAllData(residentRepository, Resident.class);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=业主数据导出.xlsx")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(bytes);
    }
}
