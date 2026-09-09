package com.devcart.catalogoservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devcart.catalogoservice.model.Produto;

@Repository
public interface ProdutoRepository extends MongoRepository<Produto, String> {

    boolean existsByNomeIgnoreCase(String nome);
}
