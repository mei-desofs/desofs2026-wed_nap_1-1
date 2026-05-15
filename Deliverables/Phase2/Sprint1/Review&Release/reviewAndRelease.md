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


## 5. Automação de Release (`release-please`)
A integração de código na `main` não gera imediatamente uma nova versão em produção. Em vez disso, utilizamos a action **Release Please**, que ajuda a gerir as alterações e lançamentos no repositório.
* Quando o PR é fundido na `main`, o *Release Please* analisa o histórico do Git em busca de mensagens de Conventional Commits.
* Com base nesses *commits*, a ferramenta cria e/ou atualiza um Pull Request por ele criado para fazer o lançamento da nova versão.
* Esta ferramenta automatiza a geração do *changelog*, o incremento da versão do projeto (seguindo a lógica de *Semantic Versioning*: MAJOR.MINOR.PATCH) e atualiza o ficheiro `CHANGELOG.md`.

## 6. Lançamento Oficial (Release)
* Quando a equipa decide que o conjunto de funcionalidades atuais está pronto para ser lançado, basta aprovar e fazer o *merge* do Pull Request gerado pelo *Release Please*.
* Ao fazê-lo, o GitHub cria automaticamente a *Tag* da versão (ex: `v1.1.0`) e a página oficial de *Release*, documentando de forma clara o que foi introduzido na nova versão do software.