# Reproduzindo a demonstração da palestra, passo a passo

Este guia refaz, na sua máquina, a demonstração ao vivo da palestra
**"Barreiras de Qualidade Avançadas em CI/CD acelerado por IA"** (slides em
[`Dani Cavalcanti-palestra.pptx`](Dani%20Cavalcanti-palestra.pptx)).

A ideia é simples: pegar o mesmo código em **dois estados** e passar os dois pelos **4 gates de
qualidade** do projeto.

| Estado | O que é |
|---|---|
| **regressão** | O que uma IA plausivelmente entrega: suíte unitária verde, **100 % de cobertura** de linha e de ramo, SonarQube sem bugs — e **6 defeitos de lógica** escondidos no `pedidos-service`. |
| **corrigido** | O que está hoje no repositório: os 6 defeitos consertados e os gates avançados funcionando como barreira contra regressão. |

Ao final você terá visto, com seus próprios olhos, que **cobertura mede execução, não verificação** —
e como mutação, property-based e contrato fecham essa lacuna.

> Para o significado de cada ferramenta e conceito (JaCoCo, PITest, jqwik, Pact, mutante, shrinking…),
> veja a seção **"Conceitos e ferramentas"** do [README](../README.md#4-conceitos-e-ferramentas).

---

## Pré-requisitos

- **JDK 21** e **Maven 3.9+** (`java -version`, `mvn -version`)
- **git**
- Opcional: **Docker**, só se quiser rodar o SonarQube localmente

Todos os comandos abaixo são executados **na raiz do repositório**. Nenhum deles precisa de banco,
fila ou dos microsserviços no ar — são todos testes.

```bash
git clone https://github.com/dani-cavalcanti/devcart.git
cd devcart
mvn -q -DskipTests package      # baixa as dependências uma vez (os passos seguintes ficam mais rápidos)
```

---

## Passo 1 · Colocar o código no estado "regressão"

O repositório vem no estado **corrigido**. Para reproduzir o cenário da palestra, aplique o patch
abaixo: ele reintroduz os 6 defeitos no `pedidos-service` e "enfraquece" dois testes unitários, do
jeito que uma sessão de geração de código costuma deixá-los — continuam verdes e continuam cobrindo
100 % das linhas, mas usam entradas "bem-comportadas" (quantidade `1`, preço positivo, um produto por
pedido).

Os 6 defeitos introduzidos:

| # | Onde | Defeito |
|---|---|---|
| 1 | `ItemPedido` | a guarda de quantidade usa `quantidade < 0` — aceita **quantidade 0** |
| 2 | `ItemPedido` | a guarda de preço só checa `null` — aceita **preço negativo** |
| 3 | `Pedido.recalcularTotal()` | **acumula** sobre o total anterior em vez de recalcular |
| 4 | `ItemPedidoResponse.fromEntity` | troca os campos `precoUnitario` ↔ `subtotal` |
| 5 | `PedidoCriadoEvent.fromEntity` | `quantidadeItens` conta **linhas**, não unidades |
| 6 | `PedidoService.criarPedido` | valida estoque **linha a linha** — o mesmo produto em duas linhas "fura" o estoque |

Copie e cole o bloco inteiro no terminal (ele termina na linha `PATCH`):

<details>
<summary><strong>Mostrar o comando que aplica o patch de regressão</strong></summary>

```bash
git apply <<'PATCH'
--- a/pedidos-service/src/main/java/com/devcart/pedidosservice/model/ItemPedido.java
+++ b/pedidos-service/src/main/java/com/devcart/pedidosservice/model/ItemPedido.java
@@ -40,14 +40,13 @@
     }
 
     public ItemPedido(String produtoId, String nomeProduto, int quantidade, BigDecimal precoUnitario) {
-        // Guarda de dominio: quantidade estritamente positiva e preco nao-nulo e nao-negativo.
-        if (quantidade <= 0) {
+        // Guarda de dominio: rejeita quantidade negativa e preco nulo.
+        if (quantidade < 0) {
             throw new IllegalArgumentException(
-                    "quantidade do item deve ser maior que zero, mas foi " + quantidade);
+                    "quantidade do item nao pode ser negativa, mas foi " + quantidade);
         }
-        if (precoUnitario == null || precoUnitario.signum() < 0) {
-            throw new IllegalArgumentException(
-                    "precoUnitario do item nao pode ser nulo nem negativo, mas foi " + precoUnitario);
+        if (precoUnitario == null) {
+            throw new IllegalArgumentException("precoUnitario do item nao pode ser nulo");
         }
         this.produtoId = produtoId;
         this.nomeProduto = nomeProduto;
--- a/pedidos-service/src/main/java/com/devcart/pedidosservice/model/Pedido.java
+++ b/pedidos-service/src/main/java/com/devcart/pedidosservice/model/Pedido.java
@@ -61,9 +61,9 @@
     }
 
     public void recalcularTotal() {
-        this.valorTotal = itens.stream()
+        this.valorTotal = this.valorTotal.add(itens.stream()
                 .map(ItemPedido::getSubtotal)
-                .reduce(BigDecimal.ZERO, BigDecimal::add);
+                .reduce(BigDecimal.ZERO, BigDecimal::add));
     }
 
     public int getQuantidadeTotalItens() {
--- a/pedidos-service/src/main/java/com/devcart/pedidosservice/dto/ItemPedidoResponse.java
+++ b/pedidos-service/src/main/java/com/devcart/pedidosservice/dto/ItemPedidoResponse.java
@@ -18,8 +18,8 @@
                 item.getProdutoId(),
                 item.getNomeProduto(),
                 item.getQuantidade(),
-                item.getPrecoUnitario(),
-                item.getSubtotal()
+                item.getSubtotal(),
+                item.getPrecoUnitario()
         );
     }
 }
--- a/pedidos-service/src/main/java/com/devcart/pedidosservice/messaging/PedidoCriadoEvent.java
+++ b/pedidos-service/src/main/java/com/devcart/pedidosservice/messaging/PedidoCriadoEvent.java
@@ -20,7 +20,7 @@
                 pedido.getId(),
                 pedido.getUsuarioId(),
                 pedido.getValorTotal(),
-                pedido.getQuantidadeTotalItens(),
+                pedido.getItens().size(),
                 pedido.getCriadoEm()
         );
     }
--- a/pedidos-service/src/main/java/com/devcart/pedidosservice/service/PedidoService.java
+++ b/pedidos-service/src/main/java/com/devcart/pedidosservice/service/PedidoService.java
@@ -1,8 +1,6 @@
 package com.devcart.pedidosservice.service;
 
-import java.util.LinkedHashMap;
 import java.util.List;
-import java.util.Map;
 import java.util.concurrent.CompletionException;
 
 import org.springframework.context.ApplicationEventPublisher;
@@ -44,27 +42,19 @@
     public PedidoResponse criarPedido(String usuarioId, CriarPedidoRequest request) {
         String dono = validarUsuario(usuarioId);
 
-        // O mesmo produto pode aparecer em varias linhas: consolida a quantidade
-        // total pedida por produto ANTES de validar o estoque.
-        Map<String, Integer> quantidadePorProduto = new LinkedHashMap<>();
+        Pedido pedido = new Pedido(dono);
         for (ItemPedidoRequest itemReq : request.itens()) {
-            quantidadePorProduto.merge(itemReq.produtoId(), itemReq.quantidade(), Integer::sum);
-        }
+            ProdutoDto produto = consultarProduto(itemReq.produtoId());
 
-        Pedido pedido = new Pedido(dono);
-        for (Map.Entry<String, Integer> linha : quantidadePorProduto.entrySet()) {
-            int quantidadeTotal = linha.getValue();
-            ProdutoDto produto = consultarProduto(linha.getKey());
-
-            if (produto.estoque() == null || produto.estoque() < quantidadeTotal) {
+            if (produto.estoque() == null || produto.estoque() < itemReq.quantidade()) {
                 throw new RegraNegocioException(
-                        "Estoque insuficiente para o produto " + linha.getKey());
+                        "Estoque insuficiente para o produto " + itemReq.produtoId());
             }
 
             pedido.adicionarItem(new ItemPedido(
                     produto.id(),
                     produto.nome(),
-                    quantidadeTotal,
+                    itemReq.quantidade(),
                     produto.preco()));
         }
         pedido.recalcularTotal();
--- a/pedidos-service/src/test/java/com/devcart/pedidosservice/model/PedidoModelTest.java
+++ b/pedidos-service/src/test/java/com/devcart/pedidosservice/model/PedidoModelTest.java
@@ -52,21 +52,17 @@
     }
 
     @Test
-    void itemPedido_rejeitaQuantidadeZeroOuNegativa() {
-        assertThatThrownBy(() -> new ItemPedido("p1", "P", 0, BigDecimal.ONE))
-                .isInstanceOf(IllegalArgumentException.class)
-                .hasMessageContaining("quantidade do item deve ser maior que zero");
+    void itemPedido_rejeitaQuantidadeNegativa() {
         assertThatThrownBy(() -> new ItemPedido("p1", "P", -1, BigDecimal.ONE))
-                .isInstanceOf(IllegalArgumentException.class);
+                .isInstanceOf(IllegalArgumentException.class)
+                .hasMessageContaining("quantidade");
     }
 
     @Test
-    void itemPedido_rejeitaPrecoNuloOuNegativo() {
+    void itemPedido_rejeitaPrecoNulo() {
         assertThatThrownBy(() -> new ItemPedido("p1", "P", 1, null))
                 .isInstanceOf(IllegalArgumentException.class)
-                .hasMessageContaining("nao pode ser nulo nem negativo");
-        assertThatThrownBy(() -> new ItemPedido("p1", "P", 1, new BigDecimal("-0.01")))
-                .isInstanceOf(IllegalArgumentException.class);
+                .hasMessageContaining("nao pode ser nulo");
     }
 
     // ---------- Pedido ----------
@@ -144,7 +140,7 @@
     @Test
     void pedidoResponse_fromEntityMapeiaItensEStatus() {
         Pedido pedido = new Pedido("user-1");
-        pedido.adicionarItem(new ItemPedido("p1", "P1", 2, new BigDecimal("5.00")));
+        pedido.adicionarItem(new ItemPedido("p1", "P1", 1, new BigDecimal("10.00")));
         pedido.recalcularTotal();
 
         PedidoResponse response = PedidoResponse.fromEntity(pedido);
@@ -154,22 +150,21 @@
         assertThat(response.valorTotal()).isEqualByComparingTo("10.00");
         assertThat(response.itens()).hasSize(1);
         assertThat(response.itens().get(0).produtoId()).isEqualTo("p1");
-        assertThat(response.itens().get(0).precoUnitario()).isEqualByComparingTo("5.00");
         assertThat(response.itens().get(0).subtotal()).isEqualByComparingTo("10.00");
     }
 
     @Test
     void pedidoCriadoEvent_fromEntityResumeOPedido() {
         Pedido pedido = new Pedido("user-1");
-        pedido.adicionarItem(new ItemPedido("p1", "P1", 2, new BigDecimal("5.00")));
+        pedido.adicionarItem(new ItemPedido("p1", "P1", 1, new BigDecimal("5.00")));
         pedido.adicionarItem(new ItemPedido("p2", "P2", 1, new BigDecimal("3.00")));
         pedido.recalcularTotal();
 
         PedidoCriadoEvent evento = PedidoCriadoEvent.fromEntity(pedido);
 
         assertThat(evento.usuarioId()).isEqualTo("user-1");
-        assertThat(evento.quantidadeItens()).isEqualTo(3);
-        assertThat(evento.valorTotal()).isEqualByComparingTo("13.00");
+        assertThat(evento.quantidadeItens()).isEqualTo(2);
+        assertThat(evento.valorTotal()).isEqualByComparingTo("8.00");
     }
 
     @Test
--- a/pedidos-service/src/test/java/com/devcart/pedidosservice/service/PedidoServiceTest.java
+++ b/pedidos-service/src/test/java/com/devcart/pedidosservice/service/PedidoServiceTest.java
@@ -61,11 +61,11 @@
     @Test
     void criarPedido_calculaTotalPersisteEPublicaEvento() {
         when(catalogoClientService.buscarProduto("p1"))
-                .thenReturn(CompletableFuture.completedFuture(produto("p1", new BigDecimal("10.00"), 50)));
+                .thenReturn(CompletableFuture.completedFuture(produto("p1", new BigDecimal("30.00"), 50)));
         when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
 
         PedidoResponse response = pedidoService.criarPedido("  user-1 ",
-                pedidoCom(new ItemPedidoRequest("p1", 3)));
+                pedidoCom(new ItemPedidoRequest("p1", 1)));
 
         ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
         verify(pedidoRepository).save(captor.capture());
PATCH
```

</details>

Confira que 7 arquivos do `pedidos-service` foram alterados:

```bash
git status --short
#  M pedidos-service/src/main/java/.../dto/ItemPedidoResponse.java
#  M pedidos-service/src/main/java/.../messaging/PedidoCriadoEvent.java
#  M pedidos-service/src/main/java/.../model/ItemPedido.java
#  M pedidos-service/src/main/java/.../model/Pedido.java
#  M pedidos-service/src/main/java/.../service/PedidoService.java
#  M pedidos-service/src/test/java/.../model/PedidoModelTest.java
#  M pedidos-service/src/test/java/.../service/PedidoServiceTest.java
```

> Para voltar ao estado corrigido a qualquer momento: `git checkout -- pedidos-service`

---

## Passo 2 · Gate 1 — Cobertura (JaCoCo)

**Pergunta que o gate responde:** o código foi *executado* pelos testes?

```bash
mvn clean verify
```

**O que você deve ver:**

```
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0     ← catalogo-service
[INFO] All coverage checks have been met.
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0     ← carrinho-service
[INFO] All coverage checks have been met.
[INFO] Tests run: 42, Failures: 0, Errors: 0, Skipped: 0     ← pedidos-service
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

**Como interpretar:** 87 testes verdes e o gate do JaCoCo exigindo **100 % de linha e de ramo em cada
classe** — satisfeito. Os 6 defeitos estão lá e nada acusou. Numa Pull Request, isso seria lido como
"pronto para o merge".

Abra o relatório e confira `ItemPedido` — 100 % coberto, inclusive a linha do defeito:

```bash
open pedidos-service/target/site/jacoco/index.html        # Linux: xdg-open
```

<details>
<summary><strong>Opcional: ver o mesmo resultado no SonarQube</strong></summary>

```bash
docker run -d --name devcart-sonar -p 9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true sonarqube:community
# aguarde ~1 min, acesse http://localhost:9000 (admin/admin), troque a senha
# e gere um token em: My Account → Security → Generate Tokens

mvn -DskipTests verify sonar:sonar -Dsonar.token=SEU_TOKEN -Dsonar.projectKey=devcart
open "http://localhost:9000/dashboard?id=devcart"
```

Resultado esperado: **Coverage 100 %**, **0 Bugs**, **Quality Gate: Passed**. O Sonar analisa o código
de produção e *importa* a cobertura do JaCoCo — ele não avalia se os testes verificam alguma coisa.

</details>

---

## Passo 3 · Gate 2 — Teste de mutação (PITest)

**Pergunta que o gate responde:** se o código estivesse errado, algum teste *falharia*?

```bash
mvn -pl pedidos-service verify -Ppitest
```

**O que você deve ver:**

```
>> Line Coverage (for mutated classes only): 128/128 (100%)
>> Generated 45 mutations Killed 44 (98%)
[ERROR] ... Mutation score of 98 is below threshold of 100
[INFO] BUILD FAILURE
```

**Como interpretar:** o PITest criou 45 versões levemente alteradas ("mutantes") do código de domínio
e rodou os testes contra cada uma. 44 foram "mortas" (algum teste falhou). **Uma sobreviveu** — a
suíte inteira passou com o código errado.

Abra o relatório para ver qual:

```bash
open pedidos-service/target/pit-reports/index.html
```

Navegue até `model` → `ItemPedido.java` → a linha da guarda de quantidade, marcada como
**SURVIVED — changed conditional boundary**. O PITest trocou `quantidade < 0` por `quantidade <= 0` e
nenhum teste percebeu: os exemplos testam `-1` e um valor positivo, mas **nunca a fronteira `0`**.

Repare na contradição: essa linha está **100 % coberta** e mesmo assim nenhum teste verifica o
comportamento dela. É exatamente o que o Gate 1 não consegue enxergar.

---

## Passo 4 · Gate 3 — Property-based testing (jqwik)

**Pergunta que o gate responde:** a regra vale para *todo* o domínio de entrada?

Antes de rodar, vale abrir uma das propriedades para ver como ela é escrita — um invariante, não um
exemplo:

```bash
cat pedidos-service/src/test/java/com/devcart/pedidosservice/model/ItemPedidoPropertyTest.java
```

Agora rode:

```bash
mvn -pl pedidos-service verify -Ppbt
```

**O que você deve ver:**

```
[ERROR] Tests run: 48, Failures: 6, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

No log, cada propriedade que falhou traz um bloco `Shrunk Sample` — o **menor** input que ainda quebra
a regra, encontrado automaticamente pelo *shrinking*:

| Propriedade | Contraexemplo mínimo |
|---|---|
| `ItemPedido`: quantidade ≤ 0 lança exceção | `quantidade = 0` |
| `ItemPedido`: preço negativo lança exceção | `preco = -1E-30` |
| `Pedido.recalcularTotal` é idempotente | chamar 1× dá `1E-10`, chamar 3× dá `3E-10` |
| `ItemPedidoResponse`: subtotal = preço × quantidade | com `q = 2`, preço e subtotal aparecem trocados |
| `PedidoCriadoEvent`: `quantidadeItens` = soma das quantidades | 1 linha com `q = 2` → evento diz `1` |
| `PedidoService`: estoque validado pela soma por produto | estoque `2`, pedido `[1, 2]` do mesmo produto → aceito |

**Como interpretar:** o PITest apontou *uma linha*; o jqwik encontrou *os seis defeitos* e entregou o
input exato que reproduz cada um — inclusive `quantidade = 0`, que ninguém escreveu à mão. Mutação
encontra **teste fraco**; property-based encontra **especificação ausente**. Os dois se complementam.

> O jqwik guarda os contraexemplos em `.jqwik-database` (ignorado pelo git) e os testa **primeiro** na
> próxima execução — é por isso que uma falha de property é reproduzível, não "flaky".

---

## Passo 5 · Corrigir e ver o gate funcionando

Volte ao estado corrigido (é o código do repositório) e rode os mesmos dois gates:

```bash
git checkout -- pedidos-service

mvn -pl pedidos-service verify -Ppitest
#  >> Generated 47 mutations Killed 47 (100%)
#  BUILD SUCCESS

mvn -pl pedidos-service verify -Ppbt
#  Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
#  BUILD SUCCESS
```

**Como interpretar:** agora os gates são uma **barreira de regressão**. Se alguém — uma pessoa ou uma
IA — reintroduzir qualquer um dos 6 defeitos, a Pull Request fica vermelha antes do merge.

> **Experimente:** abra `pedidos-service/src/main/java/com/devcart/pedidosservice/model/ItemPedido.java`,
> troque só `quantidade <= 0` por `quantidade < 0` e rode de novo os dois comandos acima. Os dois gates
> ficam vermelhos, cada um do seu jeito. Desfaça com `git checkout -- pedidos-service`.

---

## Passo 6 · Gate 4 — Teste de contrato (Pact JVM)

**Pergunta que o gate responde:** os microsserviços continuam *compatíveis* entre si?

O `pedidos-service` (**consumer**) chama `GET /produtos/{id}` do `catalogo-service` (**provider**). Em
vez de subir os dois juntos num teste ponta a ponta, o contrato é verificado em duas metades
independentes:

```bash
# 1. O consumer declara o que precisa e gera o contrato (o "pact", um JSON)
mvn -pl pedidos-service test -Pcontract -Dtest=CatalogoContractTest
#    → pedidos-service/target/pacts/pedidos-service-catalogo-service.json

# 2. "Publica" o pact para o provider.
#    Aqui é uma cópia de arquivo; em produção isso seria um Pact Broker.
cp pedidos-service/target/pacts/pedidos-service-catalogo-service.json \
   catalogo-service/src/test/resources/pacts/

# 3. O provider sobe de verdade e verifica a resposta real contra o pact
mvn -pl catalogo-service test -Pcontract -Dtest=CatalogoProviderContractTest
```

**O que você deve ver no passo 3:**

```
Verifying a pact between pedidos-service and catalogo-service
  Given produto PROD-1 existe
  busca do produto PROD-1 pelo pedidos-service
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)
```

Abra o pact gerado e procure por `matchingRules`:

```bash
cat pedidos-service/target/pacts/pedidos-service-catalogo-service.json
```

Você vai ver regras como `$.preco → decimal` e `$.estoque → integer`: o contrato fixa **tipo e
forma**, não valores literais. O consumer não se importa se o preço é `199.90` ou `10.00` — importa
que exista um campo `preco` decimal.

---

## Passo 7 · Quebrando o contrato de propósito

Este é o cenário que o gate de contrato existe para barrar: alguém renomeia um campo no provider, os
testes do próprio provider continuam verdes, e o consumer só descobre em produção.

Simule a mudança — o `catalogo-service` passa a devolver `valor` em vez de `preco`:

```bash
sed -i.bak 's/        BigDecimal preco,/        @com.fasterxml.jackson.annotation.JsonProperty("valor") BigDecimal preco,/' \
  catalogo-service/src/main/java/com/devcart/catalogoservice/dto/ProdutoResponse.java
rm catalogo-service/src/main/java/com/devcart/catalogoservice/dto/ProdutoResponse.java.bak

mvn -pl catalogo-service test -Pcontract -Dtest=CatalogoProviderContractTest
```

**O que você deve ver:**

```
      has status code 200 (OK)
      has a matching body (FAILED)
    1.1) body: $ Actual map is missing the following keys: preco
[ERROR] Tests run: 1, Failures: 1
[INFO] BUILD FAILURE
```

**Como interpretar:** a quebra apareceu **no CI do catálogo**, antes do deploy, sem subir o
`pedidos-service`, banco ou fila. Com um Pact Broker, o comando `can-i-deploy` usaria esse resultado
para impedir que essa versão do catálogo fosse para produção enquanto houver um consumer dependendo
de `preco`.

Desfaça a alteração:

```bash
git checkout -- catalogo-service
```

---

## Resumo dos comandos

| Gate | Comando | Estado regressão | Estado corrigido |
|---|---|---|---|
| 1 — Cobertura | `mvn clean verify` | ✅ verde (o "falso positivo") | ✅ verde |
| 2 — Mutação | `mvn -pl pedidos-service verify -Ppitest` | ❌ 44/45 (98 %) | ✅ 47/47 (100 %) |
| 3 — Property-based | `mvn -pl pedidos-service verify -Ppbt` | ❌ 6 contraexemplos | ✅ 48/48 |
| 4 — Contrato | ver [Passo 6](#passo-6--gate-4--teste-de-contrato-pact-jvm) | ✅ verde | ✅ verde (❌ se o provider quebrar o contrato) |

| Ação | Comando |
|---|---|
| Aplicar o estado de regressão | bloco `git apply` do [Passo 1](#passo-1--colocar-o-código-no-estado-regressão) |
| Voltar ao estado corrigido | `git checkout -- pedidos-service` |
| Relatório de cobertura | `<módulo>/target/site/jacoco/index.html` |
| Relatório de mutação | `pedidos-service/target/pit-reports/index.html` |
| Relatório de testes (HTML) | `mvn surefire-report:report-only` → `<módulo>/target/reports/surefire.html` |
| Rodar todos os contratos do reator | `mvn verify -Pcontract` |

---

## Se algo der errado

- **`git apply` falhou com "patch does not apply"** — o working tree não está limpo. Rode
  `git checkout -- pedidos-service` e tente de novo.
- **Erro do Mockito/ByteBuddy em JDK mais novo que o 21** — o `pom` já passa
  `-Dnet.bytebuddy.experimental=true`; confira que você está rodando pela raiz do repositório.
- **O provider do Pact não sobe** — ele usa `@SpringBootTest` em porta aleatória, com MongoDB e Eureka
  desligados. Verifique se nenhum firewall bloqueia portas locais.
- **Quer começar do zero** — `git checkout -- . && mvn clean`.
