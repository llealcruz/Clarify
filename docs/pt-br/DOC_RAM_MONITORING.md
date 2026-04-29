# Arquitetura de Monitoramento: RAM (Alocação de Memória)

Este documento descreve como a biblioteca **Clarify APM** audita o consumo de memória de métodos individuais, isolando os efeitos do Garbage Collector (GC).

## O Desafio do Garbage Collector

No Java, não existe o conceito de "desalocar memória manualmente". A memória Heap é global. Medir a memória total da JVM antes e depois de um método é **inútil**, pois o GC pode rodar no meio do método e a memória final ser menor do que a inicial, gerando resultados negativos e irreais.

## A Solução: `Thread Allocated Bytes`

Para medir a pressão real de memória que um método causa na aplicação, não medimos "quanto de memória sobrou", medimos **quantos bytes a thread pediu para a JVM criar** enquanto o método executava.
Isso é feito usando uma extensão específica da JVM da Oracle/OpenJDK: `com.sun.management.ThreadMXBean`.

```java
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

// O Cast é necessário pois essa é uma interface interna avançada do JDK
ThreadMXBean sunThreadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
long bytes = sunThreadBean.getThreadAllocatedBytes(Thread.currentThread().getId());
```

### O Fluxo de Interceptação

1. O `PerformanceMonitorAspect` invoca `getThreadAllocatedBytes()` assim que o método inicia. (Ex: `10 MB` alocados desde que a thread nasceu na aplicação).
2. O método original é executado, criando listas, strings e objetos temporários.
3. O aspecto invoca a função novamente no final. (Ex: `15 MB`).
4. A matemática revela que o método alocou **5 MB** de RAM (`15 - 10`).

### Por que essa métrica é genial?
Se um método aloca 50 MB de strings temporárias a cada chamada, mesmo que o GC limpe essas strings instantaneamente (evitando erro de memória), ele vai gerar uma **altíssima pressão no Garbage Collector**, o que causa travamentos globais (*GC Pauses*) na aplicação.
O Clarify consegue denunciar esses métodos "esbanjadores" para que o time de Produto/QA possa abrir tickets de refatoração para a engenharia.
