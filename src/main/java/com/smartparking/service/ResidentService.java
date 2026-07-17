package com.smartparking.service;

import com.smartparking.dto.ResidentDTO;
import com.smartparking.entity.Resident;
import com.smartparking.repository.ResidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResidentService {
    @Autowired
    private ResidentRepository residentRepository;

    public void addResident(ResidentDTO dto) {
        if (dto.getUserName() == null || dto.getUserName().isBlank()) {
            throw new RuntimeException("住户姓名不能为空");
        }
        // 校验车牌号是否已存在
        boolean exist = residentRepository.existsByPlateNumber(dto.getPlateNumber());
        if(exist){
            throw new RuntimeException("该车牌号已存在");
        }
        // 组装实体，id留空，数据库自增
        Resident resident = new Resident();
        resident.setUserName(dto.getUserName());
        resident.setPlateNumber(dto.getPlateNumber());
        // 保存
        residentRepository.save(resident);
    }

}
