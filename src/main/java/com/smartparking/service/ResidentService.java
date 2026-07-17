package com.smartparking.service;

import com.smartparking.common.ApiResponse;
import com.smartparking.dto.ResidentDTO;
import com.smartparking.entity.Resident;
import com.smartparking.repository.ResidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResidentService {
    @Autowired
    private ResidentRepository residentRepository;

    public ApiResponse<Void> addResident(ResidentDTO dto) {
        // 姓名非空校验
        if (dto.getUserName() == null || dto.getUserName().isBlank()) {
            return ApiResponse.error("住户姓名不能为空");
        }
        // 车牌重复校验
        boolean exist = residentRepository.existsByPlateNumber(dto.getPlateNumber());
        if(exist){
            return ApiResponse.error("车牌号重复！");
        }
        // 组装保存
        Resident resident = new Resident();
        resident.setUserName(dto.getUserName());
        resident.setPlateNumber(dto.getPlateNumber());
        residentRepository.save(resident);
        // 成功返回
        return ApiResponse.success();
    }
}
