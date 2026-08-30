package com.devcart.carrinhoservice.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

/**
 * Agregado do carrinho persistido no Redis.
 * A chave (id) e o identificador do usuario, obtido do header X-User-Id.
 * Chave efetiva no Redis: {@code carrinho:<usuarioId>}.
 */
@RedisHash(value = "carrinho", timeToLive = 604800) // 7 dias
public class Carrinho {

    @Id
    private String usuarioId;

    private List<ItemCarrinho> itens = new ArrayList<>();

    public Carrinho() {
    }

    public Carrinho(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public void setItens(List<ItemCarrinho> itens) {
        this.itens = itens;
    }
}
