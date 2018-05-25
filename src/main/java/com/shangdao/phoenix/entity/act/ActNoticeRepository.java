package com.shangdao.phoenix.entity.act;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ActNoticeRepository extends JpaRepository<ActNotice, Long> {
}
