package com.example.campustrace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campustrace.model.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

    long countByStatusIgnoreCase(String status);

    @org.springframework.data.jpa.repository.Query("""
        select i from Item i where
        (:keyword = '' or locate(lower(:keyword), lower(i.itemName)) > 0
                       or locate(lower(:keyword), lower(i.description)) > 0)
        and (:type = '' or upper(i.type) = :type)
        and (:status = '' or upper(i.status) = :status)
        and (:owner = '' or i.postedBy = :owner)
        order by i.id desc
        """)
    List<Item> search(@org.springframework.data.repository.query.Param("keyword") String keyword,
        @org.springframework.data.repository.query.Param("type") String type,
        @org.springframework.data.repository.query.Param("status") String status,
        @org.springframework.data.repository.query.Param("owner") String owner);

}
