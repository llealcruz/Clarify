# 🟢 Clarify APM - Enterprise Grade

Clarify é uma biblioteca (JAR) leve para Java 21+ projetada para monitoramento de performance e auditoria, com foco em transparência para o negócio.
Diferente de ferramentas puramente técnicas, o Clarify atua como uma ponte entre o código e o time de produto (PO, SM, QA).

[English Version Here](../../README.md)

## 🚀 Principais Features

- **Zero Config AOP:** Utiliza o Spring AOP nativo para monitorar sua aplicação sem necessidade de configurações de compilação (Arquitetura de *Baixo Overhead* / *Low Overhead*).
- **Storage Otimizado:** Logs estruturados em formato **JSON Lines (JSONL)**, ideais para ferramentas de log parsing e alta performance de leitura e escrita.
- **Log Rotation Automático:** Evita exaustão de disco particionando os logs de forma inteligente (via carimbo de data) quando atingem o limite configurado.
- **Tratamento de Exceções:** Permite capturar logs de lentidão E de falhas catastróficas, com granularidade por método para evitar falsos-positivos de validações de negócio.
- **Dashboard Embutido:** Visualização rica, dinâmica e legível dos relatórios diretamente no navegador da sua aplicação.
- **Virtual Threads:** I/O 100% assíncrono. A escrita no disco do monitoramento nunca atrasará a resposta do seu sistema principal.

---

## 📦 Instalação (Plug & Play)

A instalação foi desenhada para ter impacto zero no ciclo de vida do seu projeto. Basta adicionar a dependência:

### Maven (`pom.xml`)

```xml
<dependency>
    <groupId>llealcruz</groupId>
    <artifactId>clarify</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

*(Nenhum plugin complexo de build é necessário. O Clarify aproveita a mágica nativa de auto-configuração do Spring Boot!)*

---

## 🛠️ Como Usar (Guia Rápido)

Para monitorar qualquer método do seu Spring Bean, basta adicionar a anotação `@ClarifyMonitor`:

```java
import com.llealcruz.clarify.annotation.ClarifyMonitor;
import org.springframework.stereotype.Service;

@Service
public class MeuServico {

    // Monitoramento Padrão
    @ClarifyMonitor(action = "Busca de Cliente")
    public void buscarCliente() { ... }

    // Monitoramento Extremo (Avisa no painel se estourar Exception)
    @ClarifyMonitor(action = "Integração Bacen", recordExceptions = true)
    public void integrar() { ... }
}
```

### Parâmetros da Anotação

- `action`: (String) Um nome legível que aparecerá no relatório para facilitar o entendimento de QAs e POs. *Padrão: Vazio (Oculto).*
- `tag`: (String) Um marcador customizado (ex: "CRITICO", "ROTINA"). *Padrão: Vazio.*
- `warnMs`: (long) Tempo em milissegundos para considerar o método como "Abaixo do Esperado" (Alerta Amarelo). *Padrão: 500.*
- `dangerMs`: (long) Tempo em milissegundos para considerar o método como "Crítico" (Alerta Vermelho). *Padrão: 1000.*
- `recordExceptions`: (boolean) Se marcado como `true`, qualquer exceção lançada pelo método será gravada no log de erros (badge ERROR vermelho) e depois re-lançada para não quebrar a aplicação original. *Padrão: false.*

---

## ⚙️ Configurações (`application.yml`)

O Clarify permite extrema flexibilidade, desde regras de armazenamento até internacionalização das mensagens do Dashboard.
No seu `application.yml`, você pode sobrescrever as propriedades que desejar:

```yaml
clarify:
  # === CONFIGURAÇÕES DE ARMAZENAMENTO ===
  log:
    path: /var/logs/minha-aplicacao/ # Pasta onde o log será gerado (Padrão: raiz do projeto)
    filename: meu-sistema-logs.jsonl # Nome do arquivo (Padrão: clarify-logs.jsonl)
    max-size-mb: 50 # Tamanho máximo antes de acionar o Log Rotation (Padrão: 10)

  # === INTERNACIONALIZAÇÃO DAS MENSAGENS (Opcional) ===
  # Permite traduzir o Dashboard do inglês padrão para qualquer idioma!
  messages:
    ok: "Execução perfeita."
    warn: "Atenção: Método lento."
    danger: "Perigo: Possível gargalo no servidor!"
    error: "Crítico: O método falhou."
```

---

## 📊 Acesso ao Dashboard e Segurança

Ao subir sua aplicação, o Dashboard de performance interativo ficará disponível automaticamente na rota base do seu servidor:
`http://localhost:8080/clarify`

### 🔒 Segurança Delegada (IMPORTANTE)

A rota `/clarify` é **PÚBLICA** por padrão, para garantir um *Plug & Play* fácil em ambientes de desenvolvimento local.
Para ambientes de Produção, **NUNCA DEIXE ESTA ROTA ABERTA**. Você deve utilizar o mecanismo de segurança nativo da sua aplicação (Ex: `Spring Security`) para proteger o endpoint.

**Exemplo com Spring Security:**

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        // Tranca o dashboard do Clarify apenas para usuários com perfil ADMIN
        .requestMatchers("/clarify/**").hasRole("ADMIN")
        
        // Suas outras rotas de negócio...
        .anyRequest().authenticated()
    );
    return http.build();
}
```
