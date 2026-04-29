# Arquitetura de Monitoramento: CPU Time

Este documento descreve como a biblioteca **Clarify APM** captura o tempo real de processamento de CPU por método.

## Wall Clock Time vs. CPU Time

O monitoramento padrão utiliza `System.nanoTime()` para medir a diferença entre o início e o fim de um método. Isso é chamado de **Wall Clock Time** (Tempo de Relógio). 
Se o método leva 2 segundos, não significa que ele usou 2 segundos do processador. Na maioria das vezes, o método gasta 1.9 segundos inativo (bloqueado), esperando o Banco de Dados, uma API Externa, ou uma leitura de disco (I/O).

Para um APM de nível corporativo, precisamos saber o **CPU Time** (O tempo que a thread esteve efetivamente escalonada no processador realizando cálculos matemáticos/lógicos).

## A Solução: `ThreadMXBean`

A JVM fornece uma interface de gerenciamento chamada `java.lang.management.ThreadMXBean`.
Ela possui o método nativo:
```java
ThreadMXBean bean = ManagementFactory.getThreadMXBean();
long cpuTimeNs = bean.getCurrentThreadCpuTime();
```

### O Fluxo de Interceptação

1. Quando o método do cliente é acionado, o `PerformanceMonitorAspect` pergunta ao `ThreadMXBean` qual é o contador atual de CPU da thread. (Ex: `1000ns`).
2. O método do cliente (`joinPoint.proceed()`) executa e faz chamadas ao banco de dados.
3. Após o retorno do método, o aspecto pergunta novamente ao `ThreadMXBean` o contador atual de CPU. (Ex: `1500ns`).
4. A diferença (`500ns`) é o tempo **real** que o processador "suou" para executar o método.

### Overhead Adicionado
Invocar `getCurrentThreadCpuTime()` causa uma transição para código nativo (JNI/OS Kernel) para ler os contadores do Sistema Operacional. Isso custa aproximadamente de `100 a 200 nanossegundos`.
Por capturarmos esse valor na Thread Original, garantimos precisão milimétrica com um impacto ínfimo de performance.
