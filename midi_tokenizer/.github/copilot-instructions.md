# MIDI Tokenizer - Development Instructions

## Running the Application

Due to terminal compatibility issues with Spring Shell when using `./gradlew bootRun` (Gradle intercepts stdin/stdout preventing interactive shell), always use the JAR method:

```bash
./gradlew bootJar && java -jar build/libs/midi-tokenizer.jar
```

This ensures Spring Shell can create a proper interactive terminal on all platforms.

# Writing Strudel patterns

Use these links when writing Strudel pattern converters:

- [Basic notes](https://strudel.cc/workshop/first-notes/)
- [Mini Notation](https://strudel.cc/learn/mini-notation/)

It´s important that you review Strudel documentation, fetch the pages, to get recent details that can help you write better Strudel code