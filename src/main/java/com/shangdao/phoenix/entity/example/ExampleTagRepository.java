package com.shangdao.phoenix.entity.example;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExampleTagRepository extends JpaRepository<ExampleTag, Long> {
}
