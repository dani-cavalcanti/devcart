# pedidos-service — Relatórios de qualidade

Diretório de relatórios do módulo. O relatório **HTML do PITest** (teste de mutação)
é gerado sob `target/pit-reports/` por:

```bash
mvn -pl pedidos-service org.pitest:pitest-maven:mutationCoverage
```

Último resultado do PITest (alvo `PedidoService`, mutadores `STRONGER`):
`22 mutações geradas · 22 mortas · 0 sobreviventes · test strength 100%`.

---

## jqwik — Property-Based Testing (`PedidoCalculoPropertyTest`)

Executado por `mvn -pl pedidos-service test`. Regras de domínio validadas:
cálculo do valor total (`Pedido.recalcularTotal()` + `ItemPedido.getSubtotal()`) e
validação de quantidade/preço no construtor de `ItemPedido`.

### Geradores `@Provide`

| Gerador | Domínio injetado |
|---|---|
| `quantidadesNaoPositivas()` | `0`, `Integer.MIN_VALUE`, inteiros negativos |
| `quantidadesPositivas()`    | `1`, `Integer.MAX_VALUE`, inteiros grandes positivos |
| `precosNaoNegativos()`      | `BigDecimal` — unscaled `[0, 10^24]` × escala `[0, 50]` (zero, decimais extensos, valores grandes) |
| `precosNegativos()`         | idem, negativado |
| `itensValidos()`            | lista `0..15` de `(id, qtd>0, preço>=0)` |

### Propriedades — última execução (PASSOU)

```
PedidoCalculoPropertyTest:oValorTotalNuncaEhNegativoNemNaN
  tries = 1000 | checks = 1000 | seed = 6194511699368772989

PedidoCalculoPropertyTest:subtotalDoItemEhPrecoVezesQuantidadeENuncaNegativo
  tries = 1000 | checks = 1000 | seed = 2410405265917037872

PedidoCalculoPropertyTest:quantidadeMenorOuIgualAzeroDisparaIllegalArgumentException
  tries = 1000 | checks = 1000 | seed = 3113923074967858003

PedidoCalculoPropertyTest:precoNegativoDisparaIllegalArgumentException
  tries = 1000 | checks = 1000 | seed = 953765468409515234

Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

### Counterexample (demonstração)

Removendo temporariamente a guarda `if (quantidade <= 0)` de `ItemPedido`, a
propriedade `quantidadeMenorOuIgualAzeroDisparaIllegalArgumentException` falha e o
jqwik reporta o contraexemplo — `Original Sample` (amostra aleatória que quebrou)
e `Shrunk Sample` (contraexemplo mínimo após shrinking):

```
PedidoCalculoPropertyTest:quantidadeMenorOuIgualAzeroDisparaIllegalArgumentException =
  java.lang.AssertionError:
    Expecting code to raise a throwable.

tries = 1                     | # of calls to property
checks = 1                    | # of not rejected calls
generation = RANDOMIZED       | parameters are randomly generated
after-failure = SAMPLE_FIRST  | try previously failed sample, then previous seed
edge-cases#mode = MIXIN       | edge cases are mixed in
seed = -7749451678622953286   | random seed to reproduce generated values

Shrunk Sample (3 steps)
-----------------------
  quantidade: 0
  preco: 0

Original Sample
---------------
  quantidade: -2147483648
  preco: 0.007667
```

Com a guarda no lugar (estado atual do código) todas as 4 propriedades passam
com 1000 tentativas cada.
