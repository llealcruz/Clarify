# 🚀 Backlog de Evoluções e Novas Features

Este documento consolida as ideias de melhorias contínuas e novas funcionalidades que devem ser desenvolvidas para elevar o Clarify a um nível de maturidade *Enterprise* para ambientes de produção.

## 1. Tradução Humanizada com "Ação" (Nova Feature)

**Problema:** Atualmente, a biblioteca registra o nome técnico do método (ex: `salvarCp()`), o que ainda pode soar muito técnico para QAs e POs que estão lendo o painel.
**Solução:**

- Adicionar um atributo opcional `action` (ação) na anotação `@ClarifyMonitor(action = "Salva Chave Pública")`.
- Quando o `MessageTranslator` for montar a frase, ele fará uma checagem: se o usuário preencheu a `action`, o log ficará *"A ação 'Salva Chave Pública' rodou em Xms"*, em vez de *"O método 'salvarCp' rodou em Xms"*.
- **Impacto:** Transforma definitivamente a leitura técnica em uma leitura de domínio de negócio (Domain-Driven).

## 2. Log Rotation (Rotatividade de Arquivos)

**Problema:** Em sistemas com grande volume de acessos, o arquivo `clarify-logs.txt` crescerá infinitamente até esgotar o disco rígido (Storage) do servidor.
**Solução:**

- Implementar no `FileStorage` uma verificação de tamanho de arquivo (ex: limite de 10MB) ou de tempo (fechamento diário).
- Quando o limite for atingido, o arquivo atual é renomeado (ex: `clarify-logs-29-04-2026.txt`) e um novo `clarify-logs.txt` em branco é criado.
- **Impacto:** Garante a estabilidade da infraestrutura e facilita a limpeza de logs antigos por ferramentas de DevOps.

## 3. Tratamento de Exceções no Aspecto

**Problema:** Atualmente o `PerformanceMonitorAspect` apenas mede o tempo se a execução for concluída com sucesso. Se o método `proceed()` lançar uma exceção, o cronômetro para e nada é gravado no log.
**Solução:**

- Envolver o `capturedJoinPoint.proceed()` em um bloco `try-catch-finally`.
- Capturar a duração mesmo se ocorrer erro e enviar para a fila com um status de `ERROR` ou `EXCEPTION`.
- **Impacto:** O Dashboard passará a ser uma ferramenta poderosa para detectar não apenas lentidão, mas também as falhas do sistema (mostrando a mensagem de erro da Exception nos argumentos).

## 4. Segurança do Dashboard Web (Decisão Arquitetural)

**Solução:** Adotar o padrão de "Segurança Delegada" (como o Swagger/Actuator). Não incluir autenticação própria para não gerar conflito. A instrução de proteção da rota será movida para a Etapa 6 (Documentação).

## 5. Adicionar novas propriedades do application

Devemos permitir que o sistema cliente modifique as mensagens que são exibidas no dashboard.
Atualmente o padrão é portugues, mas devemos alterar esse padrao para ingles, que é o padrão
mundialmente aceito. Mas o cliente podera editar as mensagens no application do sistema
dele utilizando a lingua que quiser.

## 6. Atualizar documentação README

Explicar no README como configurar as propriedades no application da aplicação cliente.
Inclusive com exemplos de configuração.
Tambem devemos informar quais sao os atributos que podem ser passados na nossa @ClarifyMonitor,
e falar quais sao os padrões caso não sejam passados.
**Segurança:** Incluir bloco de aviso ensinando o desenvolvedor a usar o seu próprio `Spring Security` para trancar a rota `/clarify/**`.

## 7. Monitoramento Profundo de Hardware (CPU e RAM)
**Problema:** Medir apenas o "Wall Clock Time" (tempo de relógio) não indica se a aplicação está realmente processando dados pesados (CPU) ou alocando muita memória (RAM), ou se está apenas parada esperando um banco de dados.
**Solução:**
- Utilizar `ThreadMXBean` no Aspecto para capturar os nanossegundos exatos gastos na CPU e os bytes alocados na Heap pela thread atual.
- Enriquecer o `JoinPointRecord` e o JSONL com métricas de `cpuTime` e `ramAllocated`.
- **Impacto:** Eleva o Clarify a um patamar de "Enterprise APM" de elite, fornecendo métricas de infraestrutura a nível de método sem depender de agentes pesados do Java.

## 8. Ranking de Gargalos (Dashboard Analytics)
**Problema:** Com a adi��o de CPU e RAM, o painel listar� muitas execu��es soltas. O PO ou QA n�o consegue visualizar rapidamente "qual � o pior m�todo do sistema".
**Solu��o:** 
- Criar uma se��o ou aba no Dashboard com o Top 10 piores m�todos (Ranking) categorizados por: Maior Consumo de RAM (Memory Leak), Maior Tempo de CPU (CPU Leak) e Maior Dura��o na Parede (Wall Time).
- **Impacto:** Transforma a biblioteca Clarify em uma ferramenta ativa de Analytics, dando direes claras de onde a engenharia deve atuar primeiro para otimizar a infraestrutura.

## 9. O Clarify precisa ficar me ingles
Todo o código, e as informações do dashboard precisam ficar em ingles,
para que a lib possa ser mundialmente utilizada. A unica coisa
que podemos manter em portugues sao os comentarios do código.