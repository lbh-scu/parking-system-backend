package com.smartparking.service;

import com.smartparking.repository.ResidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResidentService {
    @Autowired
    private ResidentRepository residentRepository;

}
