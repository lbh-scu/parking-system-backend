package com.smartparking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    // 批量新增（所有表共用）
    <S extends T> List<S> saveAll(Iterable<S> entities);
}
