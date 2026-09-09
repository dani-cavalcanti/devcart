package com.devcart.carrinhoservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.devcart.carrinhoservice.model.Carrinho;

@Repository
public interface CarrinhoRepository extends CrudRepository<Carrinho, String> {
}
