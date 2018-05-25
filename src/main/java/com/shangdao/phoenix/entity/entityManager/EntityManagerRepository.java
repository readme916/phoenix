package com.shangdao.phoenix.entity.entityManager;

import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntityManagerRepository extends JpaRepository<EntityManager, Long>{
	EntityManager findByName(String name);
}
