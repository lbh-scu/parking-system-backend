package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.Fee;
import com.smartparking.repository.FeeRepository;
import com.smartparking.service.ExcelCommonService;
import com.smartparking.service.FeeService;
import com.smartparking.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/fees")
public class FeeController {

    @Autowired
    private FeeService feeService;

    @Autowired
    private ExcelCommonService excelCommonService;

    @Autowired
    private FeeRepository feeRepository;

    /**
     * 计算停车费用
     */
    @PostMapping("/calculate")
    public ApiResponse<Fee> calculateFee(@RequestParam String plateNumber) {
        try {
            Fee fee = feeService.calculateFee(plateNumber);
            return ApiResponse.success("费用计算成功", fee);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 支付费用
     */
    @PostMapping("/pay")
    public ApiResponse<Fee> payFee(@RequestParam Long feeId) {
        try {
            Fee fee = feeService.payFee(feeId);
            return ApiResponse.success("支付成功", fee);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取待结算车辆
     */
    @GetMapping("/pending")
    public ApiResponse<List<Fee>> getPendingFees() {
        List<Fee> pendingFees = feeService.getPendingFees();
        return ApiResponse.success(pendingFees);
    }

    /**
     * 收费统计
     */
    @GetMapping("/statistics")
    public ApiResponse<BigDecimal> getStatistics() {
        BigDecimal totalRevenue = feeService.getTotalRevenue();
        return ApiResponse.success(totalRevenue);
    }
    /**
     * 下载费用账单Excel导入模板
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        String templatePath = "excel-template/fee_template.xlsx";
        byte[] bytes = ExcelUtil.getTemplateFile(templatePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=费用账单导入模板.xlsx")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(bytes);
    }

    /**
     * 批量导入账单Excel
     */
    @PostMapping("/import")
    public ApiResponse<String> importExcel(@RequestParam MultipartFile file) {
        try {
            int count = excelCommonService.importData(file, Fee.class, feeRepository);
            return ApiResponse.success("账单导入成功，共" + count + "条数据", null);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 导出全部账单数据Excel（浏览器下载）
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() throws Exception {
        byte[] bytes = excelCommonService.exportAllData(feeRepository, Fee.class);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=费用账单数据导出.xlsx")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(bytes);
    }
}
