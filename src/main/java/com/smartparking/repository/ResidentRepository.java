package com.smartparking.repository;

import com.smartparking.entity.Fee;
import com.smartparking.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResidentRepository extends BaseRepository<Resident, Long> {

}
