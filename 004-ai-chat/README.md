# Shipmate

A demo order-management app showing how to embed an **AI chat assistant** in a Vaadin + Spring Boot application. The assistant answers questions about the data ("How many orders are due today?") and modifies it ("Cancel the order for John Snow") through natural-language chat.

Built with:

- [Vaadin 25.2](https://vaadin.com) (Flow) with the [AI components](https://vaadin.com/docs/latest/flow/ai-support) preview (`AIOrchestrator`, Message List, Message Input)
- [Spring AI 2.0](https://spring.io/projects/spring-ai) with OpenAI
- Spring Boot, Spring Data JPA, and an H2 in-memory database seeded with 100 demo orders

**Want the step-by-step walkthrough?** See [TUTORIAL.md](TUTORIAL.md).

## Prerequisites

- Java 21+
- An OpenAI API key ([platform.openai.com/api-keys](https://platform.openai.com/api-keys))

## Configure your API key

Pick one:

**Option A** — create `src/main/resources/application-local.properties` (git-ignored):

```properties
spring.ai.openai.api-key=sk-...
```

**Option B** — set an environment variable:

```bash
export OPENAI_API_KEY=sk-...
```

## Run

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

No system Maven needed — the wrapper is included. The app opens at **http://localhost:8080**.

Click the ✨ button next to the "Orders" title and try:

- *How many orders are due today?*
- *How many orders are over $50?*
- *Cancel the order for John Snow*
- *Archive all orders that were completed over a week ago*

The grid updates live as the AI changes data.

## Notes

- The Vaadin AI components are a **preview feature**, enabled via `src/main/resources/vaadin-featureflags.properties`.
- The model is set to `gpt-5-mini` in `application.properties`. Reasoning models handle the "count and filter 100 rows" questions reliably; non-reasoning models often miscount.
- The database is in-memory and reseeded on every start, so feel free to let the AI loose on it.
- For instant hotswap while editing, run the app through the Vaadin IDE plugin (**Debug using Hotswap Agent**).

## Build for production

```bash
./mvnw package
java -jar target/*.jar
```
