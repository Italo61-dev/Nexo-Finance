# Nexo Finance

Nexo Finance é uma aplicação de gestão financeira pessoal projetada para ajudar os usuários a controlar seus gastos, organizar as finanças por categorias e planejar o futuro financeiro com ferramentas de orçamento e acompanhamento.

Este repositório contém o backend da aplicação, construído com tecnologias robustas e modernas, e está preparado para uma futura integração com o frontend.

## ✨ Funcionalidades Implementadas

- **API RESTful**: Endpoints para as operações CRUD (Create, Read, Update, Delete) das principais entidades do sistema.
- **Autenticação e Autorização**: Mecanismos de segurança para proteger os dados dos usuários.
- **Versionamento de Banco de Dados**: Migrations automatizadas com Liquibase para garantir a consistência do schema.
- **Conteinerização**: Ambiente de desenvolvimento padronizado com Docker.

## 🚀 Roadmap

- [ ] **Dashboard Financeiro**: Visualização consolidada das finanças.
- [ ] **Metas de Orçamento**: Criação e acompanhamento de metas de gastos por categoria.
- [ ] **Relatórios Avançados**: Geração de relatórios mensais e anuais.

---

## 🛠️ Stack Tecnológica

O backend do Nexo Finance é construído com as seguintes tecnologias:

- **Java 17**: Versão mais recente da linguagem Java com suporte de longo prazo (LTS).
- **Spring Boot 3**: Framework principal para a construção de aplicações web robustas e seguras.
- **Spring Data JPA**: Para persistência de dados e interação com o banco de dados.
- **Spring Security**: Para implementação de autenticação e autorização.
- **PostgreSQL**: Banco de dados relacional, executado em um container Docker para desenvolvimento.
- **Liquibase**: Para gerenciamento e versionamento das alterações no schema do banco de dados.
- **Maven**: Ferramenta de automação de build e gerenciamento de dependências.
- **Docker & Docker Compose**: Para criar um ambiente de desenvolvimento consistente e isolado.

---

##  Prerequisites

Antes de começar, certifique-se de que você tem as seguintes ferramentas instaladas em sua máquina:

- [Java Development Kit (JDK) 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- [Apache Maven](https://maven.apache.org/download.cgi)
- [Docker](https://www.docker.com/get-started)
- [Docker Compose](https://docs.docker.com/compose/install/)
- [Git](https://git-scm.com/downloads)

---

##  clonagem do repositório

Primeiro, clone o repositório para a sua máquina local.

### HTTPS

```bash
git clone https://github.com/Italo61-dev/Nexo-Finance.git
```

### SSH

```bash
git clone git@github.com:Italo61-dev/Nexo-Finance.git
```

---

## ⚙️ Configuração do Ambiente

O projeto utiliza Docker para gerenciar o banco de dados em ambiente de desenvolvimento, simplificando a configuração.

### 1. Navegue até o diretório do backend

```bash
cd Nexo-Finance/backend
```

### 2. Configure as Variáveis de Ambiente

O `docker-compose.yml` já define as variáveis de ambiente padrão para o banco de dados. Nenhuma configuração adicional é necessária para o setup inicial.

### 3. Inicie o Banco de Dados com Docker

O PostgreSQL é executado em um container Docker. Para iniciá-lo, execute o comando a partir da pasta `backend`:

```bash
docker-compose -f docker/docker-compose.yml up -d
```

Este comando irá baixar a imagem do PostgreSQL (se ainda não estiver presente) e iniciar o container em background. O banco de dados estará acessível em `localhost:5433`.

Para verificar se o container está em execução:

```bash
docker ps
```

---

## ▶️ Executando a Aplicação

Com o banco de dados em execução, você pode iniciar a aplicação Spring Boot.

```bash
./mvnw spring-boot:run
```

A aplicação irá iniciar e se conectar automaticamente ao banco de dados no container Docker. Por padrão, o servidor estará disponível em `http://localhost:8080`.

---

## 🧪 Executando os Testes

Para garantir a qualidade e a integridade do código, execute a suíte de testes automatizados:

```bash
./mvnw test
```

Este comando irá executar todos os testes unitários e de integração do projeto.

---

## 🗂️ Estrutura de Pastas

A estrutura de pastas do backend segue as convenções do Maven e do Spring Boot:

```
backend/
├── .mvn/                  # Wrapper do Maven
├── docker/                # Arquivos do Docker Compose
│   └── docker-compose.yml # Define o serviço do banco de dados
├── src/
│   ├── main/
│   │   ├── java/          # Código-fonte da aplicação
│   │   └── resources/     # Arquivos de configuração e migrations
│   │       ├── db/changelog/ # Migrations do Liquibase
│   │       └── application.properties # Configurações do Spring
│   └── test/
│       └── java/          # Código dos testes
├── mvnw                   # Script de execução do Maven para Linux/macOS
├── mvnw.cmd               # Script de execução do Maven para Windows
└── pom.xml                # Arquivo de configuração do Maven
```

---

## 🤝 Contribuição

Contribuições são bem-vindas! Se você deseja contribuir para o Nexo Finance, siga os seguintes passos:

1.  **Faça um Fork** do repositório.
2.  **Crie uma Nova Branch**: `git checkout -b feature/sua-feature`.
3.  **Faça suas Alterações**: Implemente a nova funcionalidade ou correção.
4.  **Execute os Testes**: Garanta que todos os testes continuam passando.
5.  **Faça o Commit**: `git commit -m 'feat: Adiciona nova feature'`.
6.  **Envie para o GitHub**: `git push origin feature/sua-feature`.
7.  **Abra um Pull Request**.

---

## 📝 Licença

Este projeto está licenciado sob a [MIT License](LICENSE).
