package llealcruz.clarify.aspect;

import java.time.LocalDateTime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import java.lang.management.ManagementFactory;
import com.sun.management.ThreadMXBean;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import llealcruz.clarify.async.ClarifyDispatcher;

@Aspect
public class PerformanceMonitorAspect {

    // Como o AspectJ cria uma única instância do Aspecto, podemos usar o 'new' aqui
    // para mais eficiencia na performance,
    // visto que não precisamos criar novas instancias toda vez que o advice for
    // chamado.
    private final ClarifyDispatcher clarifyDispatcher = new ClarifyDispatcher();
    private final ThreadMXBean threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();

    /*
     * Filtro para que o advice processe apenas os métodos anotados
     * com @ClarifyMonitor
     * O execution aqui serve para que seja processado qualquer execução de método e
     * somente oque estiver dentro do método (join point), e nao sua chamda(call).
     * os parametros de execution:
     * O primeiro * significa "qualquer tipo de retorno" (void, String, etc).
     * O segundo * significa "qualquer nome de método".
     * O (..) significa "com quaisquer parâmetros".
     */
    @Pointcut("execution(* *(..)) && @annotation(llealcruz.clarify.annotation.ClarifyMonitor)")
    public void pointCut() {
    }

    @Around("pointCut()")
    public Object advice(ProceedingJoinPoint capturedJoinPoint) throws Throwable { // Método do joinPoint pode lançar
        LocalDateTime startTimeForRecord = LocalDateTime.now();
        long startTime = System.nanoTime(); // nanoTime: relogio continuo imune a mudancas de horario

        long startCpuTime = threadBean.getCurrentThreadCpuTime();
        long startRamAllocated = threadBean.getThreadAllocatedBytes(Thread.currentThread().getId());

        try {
            Object result = capturedJoinPoint.proceed(); // Código do método(join point) original roda aqui
            long endTime = System.nanoTime();
            long endCpuTime = threadBean.getCurrentThreadCpuTime();
            long endRamAllocated = threadBean.getThreadAllocatedBytes(Thread.currentThread().getId());

            long durationMs = (endTime - startTime) / 1_000_000;
            long cpuTimeNs = (endCpuTime != -1 && startCpuTime != -1) ? (endCpuTime - startCpuTime) : 0;
            long ramAllocatedBytes = (endRamAllocated != -1 && startRamAllocated != -1)
                    ? (endRamAllocated - startRamAllocated)
                    : 0;

            clarifyDispatcher.dispatch(capturedJoinPoint, startTimeForRecord, durationMs, cpuTimeNs, ramAllocatedBytes,
                    null);

            return result; // Retorna oque o método original deveria retornar
        } catch (Throwable t) {
            long endTime = System.nanoTime();
            long endCpuTime = threadBean.getCurrentThreadCpuTime();
            long endRamAllocated = threadBean.getThreadAllocatedBytes(Thread.currentThread().threadId());

            long durationMs = (endTime - startTime) / 1_000_000;
            long cpuTimeNs = (endCpuTime != -1 && startCpuTime != -1) ? (endCpuTime - startCpuTime) : 0;
            long ramAllocatedBytes = (endRamAllocated != -1 && startRamAllocated != -1)
                    ? (endRamAllocated - startRamAllocated)
                    : 0;

            clarifyDispatcher.dispatch(capturedJoinPoint, startTimeForRecord, durationMs, cpuTimeNs, ramAllocatedBytes,
                    t);

            throw t; // OBRIGATÓRIO: Repassa a exceção para não quebrar o sistema do cliente
        }
    }
}
