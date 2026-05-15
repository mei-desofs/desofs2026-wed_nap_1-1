# Code Review & Release Strategy

A nossa equipa adota um **Branch-based Workflow** suportado por integração contínua (CI) e automação de lançamentos. O objetivo desta estratégia é garantir que qualquer código que chegue à *branch* principal (`main`) seja funcional, seguro e devidamente revisto, minimizando a introdução de regressões ou vulnerabilidades.

Abaixo detalhamos o ciclo de vida de uma alteração de código, desde o desenvolvimento até ao lançamento da versão.

## 1. Desenvolvimento em Branches Isoladas
* Cada nova funcionalidade, correção de erro ou tarefa de manutenção é desenvolvida numa *branch* separada.
* Sempre que novos *commits* são enviados (push) para estas *branches* secundárias, o workflow `Feature Checks` é acionado automaticamente.
* Este passo preliminar compila o projeto em Java 21 e corre testes unitários e análises estáticas básicas (SpotBugs), garantindo que o código base está íntegro antes de sequer ser proposto para integração.

## 2. Criação do Pull Request (PR)
* Quando o trabalho na *branch* está concluído, o developer cria um Pull Request para obter *feedback* e fundir o trabalho na `main`.
* É obrigatório que o título do Pull Request e os *commits* finais sigam a especificação de **Conventional Commits** (ex: `feat:`, `fix:`, `chore:`), pois isto é essencial para a fase de lançamento.

## 3. Validação Automática (Integração Contínua e Segurança)
A criação ou atualização de um PR para a `main` serve como *trigger* para o nosso workflow principal de validação (`Development CI` e `Security Pipeline`). Antes de qualquer revisão humana, o GitHub Actions executa automaticamente:
* **Build & Test:** Compilação do código fonte (Maven) e execução de todos os testes automatizados.
* **Secret Scan:** Verificação de credenciais expostas no código através do GitLeaks.
* **SAST (Static Application Security Testing):** Análise de vulnerabilidades no código fonte utilizando o CodeQL e FindSecBugs.
* **SCA (Software Composition Analysis):** Verificação de vulnerabilidades em dependências externas através do OWASP Dependency-Check e geração do SBOM.
* **DAST (Dynamic Application Security Testing):** Análise dinâmica à API em execução usando o OWASP ZAP.

## 4. Revisão de Código (Peer Review)
* Com as pipelines concluídas, um elemento da equipa (diferente do autor) assume o papel de revisor.
* O revisor tem acesso imediato aos resultados das verificações de segurança e testes automatizados. Se a pipeline falhar (indicadores a vermelho), o revisor sabe de antemão que o código precisa de correções, poupando tempo na revisão manual.
* Se os testes passarem e a qualidade do código for validada, o revisor aprova o Pull Request e o código é integrado na `main`.

## 5. Promoção para Produção (`main` -> `prod`)
Após o código estar estabilizado na branch `main`, é necessário realizar a promoção para o ambiente de produção.
* Para enviar as alterações para a branch `prod`, deve ser criado um Pull Request específico da `main` para `prod`.
* Este Pull Request de promoção é sujeito a uma nova revisão formal pela equipa para garantir que o conjunto de funcionalidades está pronto para ser distribuído.

## 6. Lançamento Oficial e Automação (`release-please`)
O lançamento oficial acontece no momento em que o código é fundido (merge) na branch `prod`.
* A integração na branch `prod` serve como gatilho para a action **Release Please**.
* O *Release Please* analisa o histórico de Conventional Commits integrados e cria/atualiza um Pull Request de release automático.
* O papel fundamental do *Release Please* neste fluxo é a gestão do versionamento semântico, atualização do `CHANGELOG.md` e, principalmente, a disponibilização do artefacto `.jar` da nova versão através da página de Releases do GitHub, facilitando a distribuição da versão estável.