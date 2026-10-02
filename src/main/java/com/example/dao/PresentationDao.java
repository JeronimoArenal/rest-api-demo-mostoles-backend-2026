package com.example.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entities.Presentation;

import java.util.Optional;

public interface PresentationDao extends JpaRepository<Presentation, Integer> {
    Optional<Presentation> findByName(String name);

}
