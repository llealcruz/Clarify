# 🏛️ Decisões Arquiteturais (ADR - Architecture Decision Records)

Este documento registra as decisões arquiteturais cruciais tomadas durante o desenvolvimento do **Clarify APM**, os problemas enfrentados e os *trade-offs* aceitos para justificar o design final da biblioteca.

---

## 1. Spring AOP vs AspectJ (Compile-Time Weaving)

### Contexto

O objetivo técnico primário no início do projeto era criar uma biblioteca de monitoramento com **Zero Overhead** absoluto. Para atingir isso, a primeira versão da arquitetura utilizava o *AspectJ* puro com a técnica de *Compile-Time Weaving (CTW)*. Nessa abordagem, o `aspectj-maven-plugin` injetava o cronômetro diretamente no *bytecode* das classes do cliente durante a fase de build.

### O Problema Encontrado

Apesar de ser a solução de maior performance teórica, o *Compile-Time Weaving* gerava um atrito gigantesco na experiência do desenvolvedor (DevEx):

1. **Intrusão:** O cliente final era obrigado a poluir o seu `pom.xml` ou `build.gradle` com plugins complexos do AspectJ apenas para usar a biblioteca.
2. **Conflitos Locais:** Ferramentas de desenvolvimento e compiladores internos de IDEs (como o VS Code) frequentemente ignoravam o plugin do Maven ao rodar aplicações ou testes locais. Isso gerava classes que não haviam sido "costuradas", fazendo o monitoramento falhar silenciosamente no ambiente do desenvolvedor.

### A Decisão: Migração para Spring AOP

Optamos por abandonar o compilador do AspectJ e abraçar totalmente o **Spring AOP**.
Registramos nosso Aspecto (`PerformanceMonitorAspect`) como um `@Bean` gerenciado na nossa `ClarifyAutoConfiguration`. Agora, o ecossistema do Spring Boot se encarrega de criar *Proxies* dinâmicos (CGLIB) na memória RAM durante a inicialização da aplicação para interceptar as chamadas dos métodos anotados.

### Trade-offs Aceitos

* ✅ **Pró (Plug & Play Extremo):** A instalação se tornou absurdamente simples. O cliente apenas importa o `.jar` da biblioteca e tudo funciona via auto-configuração. Nenhuma alteração de build é exigida.
* ✅ **Pró (Confiabilidade na IDE):** Funciona perfeitamente e consistentemente em qualquer IDE (VS Code, IntelliJ, Eclipse) com os botões nativos de `Run`/`Debug`.
* ❌ **Contra (Low Overhead vs Zero Overhead):** Perdemos o "Zero Overhead" verdadeiro. O uso de Proxies do Spring adiciona uma leve penalidade (cerca de 5 a 15 nanossegundos) por interceptação de método, uma vez que a chamada original passa por uma camada de reflexão/proxy.

### Conclusão

Aceitamos conscientemente o mínimo *overhead* de nanossegundos em favor de uma **DevEx (Developer Experience) infinitamente superior e amigável**. Como o Clarify é destinado a monitorar métodos de regra de negócio, consultas a banco de dados e integrações externas (onde a duração é frequentemente medida em dezenas ou centenas de milissegundos), o impacto do Spring AOP é completamente imperceptível para 99% das aplicações comerciais.

---

## 2. Arquitetura de Persistência

Este documento registra a decisão arquitetural e o funcionamento da persistência de logs na biblioteca Clarify, utilizando o **Padrão Produtor-Consumidor (Producer-Consumer Pattern)** com Buffer em Memória.

### O Problema: O Gargalo de I/O (Disco Físico)

O disco rígido (I/O) é o componente mais lento da computação. Se o sistema alvo tiver 1.000 requisições por segundo, teremos 1.000 Virtual Threads tentando abrir o arquivo `clarify-log.txt`, escrever uma linha e fechar simultaneamente.
O Sistema Operacional aplica mecanismos de "Lock" (bloqueio) para impedir que dois processos corrompam o arquivo escrevendo no mesmo byte ao mesmo tempo.
**Resultado:** O disco se torna um funil (gargalo), e a performance despenca, causando fila de espera no Sistema Operacional.

### A Solução: Batch Write com Fila em Memória

Para que a biblioteca tenha **Zero Overhead** e escale ao infinito, nós evitamos gravar no arquivo a cada requisição. Em vez disso, usamos a Memória RAM, que é feita para concorrência extrema, e delegamos a escrita para um "faxineiro".

#### Passo A: Os Produtores (Nossas Virtual Threads)

As Virtual Threads criadas pelo `ClarifyDispatcher` **não acessam o disco**.
O trabalho delas é apenas gerar a `String` (o relatório humano) e fazer um simples `fila.add(relatorio)`.
Essa `fila` é um objeto do tipo `ConcurrentLinkedQueue<String>`, uma estrutura thread-safe do Java desenhada para suportar milhares de threads inserindo dados simultaneamente sem sofrer bloqueios (non-blocking algorithm). Inserir um dado na RAM leva frações de nanossegundos. A Virtual Thread termina seu trabalho instantaneamente.

#### Passo B: O Consumidor (O Cronjob de Background)

Existe **apenas UMA thread solitária** dedicada à persistência (gerenciada por um `ScheduledExecutorService`).
Ela fica dormindo e acorda periodicamente (ex: a cada 5 segundos).
O ciclo de trabalho dela é:

1. Verifica a `ConcurrentLinkedQueue`.
2. Se houver itens (ex: 500 relatórios acumulados), ela retira todos da memória de uma vez.
3. Abre o arquivo físico `clarify-log.txt` **uma única vez**.
4. Despeja todas as 500 linhas no arquivo de uma tacada só (isso se chama *Batch Write*).
5. Fecha o arquivo e volta a dormir.

### Trade-off (A "Pegadinha" da Arquitetura)

Toda decisão arquitetural tem um preço.

* **Vantagem Colossal:** O disco físico é acionado apenas 1 vez a cada 5 segundos, independente de o tráfego ser de 10 ou 10.000 requisições por segundo. O sistema de I/O nunca fica sobrecarregado.
* **O Risco Aceitável:** Como os dados ficam na RAM aguardando o agendador passar a cada 5 segundos, **se o servidor sofrer um crash fatal (falta de energia, hardware fault)**, os logs daqueles últimos segundos que estavam na Fila (não consolidados no disco) serão perdidos.

Para bibliotecas de APM (Application Performance Monitoring), perder 5 segundos de métricas em um cenário de catástrofe do servidor é um preço **totalmente aceitável** em troca do ganho massivo de performance e escalabilidade.
