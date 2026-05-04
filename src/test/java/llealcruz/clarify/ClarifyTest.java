package llealcruz.clarify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import llealcruz.clarify.annotation.ClarifyMonitor;
import llealcruz.clarify.model.TestObject;

@SpringBootApplication
public class ClarifyTest {

    public static void main(String[] args) throws Exception {
        // Inicializa o servidor Web do Spring Boot na porta 8080
        org.springframework.context.ApplicationContext context = SpringApplication.run(ClarifyTest.class, args);

        // Puxa a classe gerenciada pelo Spring para executar os metodos anotados
        ClarifyTest test = context.getBean(ClarifyTest.class);
        test.dangerMethod(new TestObject("Luã", 27));
        test.okMethod();
        test.warnMethod();
        test.heavyCpuMethod();

        try {
            test.monitoredErrorMethod();
        } catch (Exception e) {
            System.out.println("Monitored error captured in Main: " + e.getMessage());
        }

        try {
            test.silentErrorMethod();
        } catch (Exception e) {
            System.out.println("Silent error captured in Main: " + e.getMessage());
        }
    }

    @ClarifyMonitor(action = "Method that will warn", tag = "1")
    public void warnMethod() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @ClarifyMonitor(action = "Method that will danger", tag = "2")
    public void dangerMethod(TestObject testObject) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @ClarifyMonitor(action = "Method that will be ok", tag = "3")
    public void okMethod() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @ClarifyMonitor(action = "Method that will explode (Monitored)", tag = "4", recordExceptions = true)
    public void monitoredErrorMethod() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
        }
        throw new RuntimeException("Simulating a business failure!");
    }

    @ClarifyMonitor(action = "Method that will explode (Silent)", tag = "5") // recordExceptions padrão é false
    public void silentErrorMethod() {
        throw new IllegalArgumentException("This error will not go to APM");
    }

    @ClarifyMonitor(action = "Heavy processing method", tag = "6")
    public void heavyCpuMethod() {
        long sum = 0;
        // Loop gigante para forçar o processador a trabalhar e gastar CPU Time real
        for (long i = 0; i < 2_000_000_000L; i++) {
            sum += i;
        }
        System.out.println("Processing complete. Sum = " + sum);
    }
}
