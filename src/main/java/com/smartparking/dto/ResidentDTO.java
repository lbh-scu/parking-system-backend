package com.smartparking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增住户接口入参DTO
 */
@Data
public class ResidentDTO {

    @NotBlank(message = "住户姓名不能为空")
    private String userName;

    @NotBlank(message = "车牌号不能为空")
    private String plateNumber;
}