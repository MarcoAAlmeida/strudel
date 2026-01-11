# MIDI Tokenizer - Development Instructions

## Running the Application

Due to terminal compatibility issues with Spring Shell when using `./gradlew bootRun` (Gradle intercepts stdin/stdout preventing interactive shell), always use the JAR method:

```bash
./gradlew bootJar && java -jar build/libs/midi-tokenizer.jar
```

This ensures Spring Shell can create a proper interactive terminal on all platforms.
